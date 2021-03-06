package net.hicks.recipe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hicks.recipe.beans.*;
import net.hicks.recipe.repos.MySystemRepository;
import net.hicks.recipe.repos.RoleRepository;
import net.hicks.recipe.repos.UserFavoriteRepository;
import net.hicks.recipe.repos.UserRepository;
import net.hicks.recipe.security.PasswordEncoder;
import net.hicks.recipe.services.RecipeService;
import org.hibernate.exception.SQLGrammarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Seeder
{
    private final Logger log = LoggerFactory.getLogger(Seeder.class);

    private final MySystemRepository mySystemRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManagerFactory entityManagerFactory;

    private final RecipeService recipeService;

    public Seeder(MySystemRepository mySystemRepository,
                  RoleRepository roleRepository,
                  UserRepository userRepository,
                  UserFavoriteRepository userFavoriteRepository,
                  PasswordEncoder passwordEncoder,
                  EntityManagerFactory entityManagerFactory,
                  RecipeService recipeService)
    {
        this.mySystemRepository = mySystemRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userFavoriteRepository = userFavoriteRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManagerFactory = entityManagerFactory;
        this.recipeService = recipeService;
    }

    @Transactional
    void installExtensions()
    {
        try
        {
            EntityManager entityManager = entityManagerFactory.createEntityManager();

            Arrays.asList("pg_trgm", "pg_stat_statements")
                    .forEach(extension -> {
                        Query query = entityManager.createNativeQuery("select count(*) from pg_extension where extname='" + extension + "';");
                        BigInteger result = (BigInteger) query.getSingleResult();
                        if (result == null || result.equals(BigInteger.ZERO))
                        {
                            log.info("installing extension " + extension + "...");
                            entityManager.getTransaction().begin();
                            query = entityManager.createNativeQuery("CREATE EXTENSION " + extension + ";");
                            query.executeUpdate();
                            entityManager.getTransaction().commit();
                        }
                        else
                            log.info("extension " + extension + " already installed.");
                    });
        }
        catch (SQLGrammarException e)
        {
            log.error(e.getLocalizedMessage());
        }
    }

    void seed(boolean seedDbIfEmpty)
    {
        if (!seedDbIfEmpty)
            return;

        log.info("Seeding data");

        installExtensions();
        log.info("installed db extensions");

        createDefaultRoles();  // use in production
        log.info("created default roles");

        // some dependencies
        createMySystem();  // use in production
        log.info("created mySystem");

        createUsers();
        log.info("created users");

        createRecipes();
        log.info("created recipes");
        log.info("created favorites");

        log.info("Done seeding data");
    }

    private void createMySystem()
    {
        MySystem mySystem;
        if (mySystemRepository.findAll().isEmpty())
            mySystem = new MySystem();
        else
            mySystem = mySystemRepository.findFirstBy();

        mySystemRepository.save(mySystem);
    }

    public void createRecipes() {
        Resource recipesFile = new ClassPathResource("recipes.json");
        User user = userRepository.findByUsername("admin@test.com");
        try {
            InputStream inputStream = recipesFile.getInputStream();

            ObjectMapper objectMapper = new ObjectMapper();
            List<Recipe> recipes = objectMapper.readValue(inputStream, new TypeReference<>() {});
            recipes.forEach(recipe -> recipe.setId(null));
            recipes.forEach(recipe -> {
                List<Direction> directions = recipe.getDirections();
                for (int i = 0; i < directions.size(); i++)
                    directions.get(i).setIndex(i);
            });
            recipes.forEach(recipe -> recipe.setAuthor(user));
            recipeService.createRecipes(recipes);

            recipes.stream().limit(new Random().nextInt(10)+1).forEach(x -> {
                UserFavorite favorite = new UserFavorite(user, x);
                userFavoriteRepository.save(favorite);
            });

        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    public void createDefaultRoles()
    {
        if (roleRepository.count() > 0)
            return;

        Arrays.asList("ROLE_USER", "ROLE_ADMIN").forEach((r) -> {
            Role role = new Role();
            role.setRole(r);
            roleRepository.save(role);
        });
    }

    private void createUsers()
    {
        List<UserData> users = Collections.singletonList(
                new UserData("admin@test.com", "adminGuy123", "admin", "Admin", "Test", true, true)
        );

        Role userRole = roleRepository.findByRole("ROLE_USER");
        Role adminRole = roleRepository.findByRole("ROLE_ADMIN");

        for (UserData userData : users)
        {
            String password = passwordEncoder.encoder().encode(userData.password);

            User user = new User(userData.username, userData.displayName, password, new HashSet<>());

            user.getRoles().add(userRole);
            if (userData.admin)
            {
                user.getRoles().add(adminRole);
                roleRepository.save(adminRole);
            }

            userRepository.save(user);
        }
    }

    static class UserData
    {
        final String username;
        final String displayName;
        final String password;
        final String first;
        final String last;
        final boolean enabled;
        final boolean admin;

        public UserData(String username, String displayName, String password, String first, String last, boolean enabled, boolean admin)
        {
            this.username = username;
            this.displayName = displayName;
            this.password = password;
            this.first = first;
            this.last = last;
            this.enabled = enabled;
            this.admin = admin;
        }
    }
}
