package net.hicks.recipe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class MvcConfig implements WebMvcConfigurer
{
    // Allows the handling of input type='datetime-local'
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
    }

   @Override
   public void addCorsMappings(CorsRegistry registry) {
       registry
               .addMapping("/**")
               .allowedOrigins("http://localhost:3000", "https://localhost:3000",
                       "https://hicks-recipes.netlify.app", "https://recipes.ehicks.net")
               .allowedMethods("*");
   }
}