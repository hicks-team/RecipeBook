import React, { useEffect, useState, useCallback } from 'react';
import { Route } from 'react-router-dom';
import Recipe from './react/app/Recipe/Recipe';
import Navbar from './react/components/Navbar/Navbar';
import Footer from './react/components/Footer';
import { IRecipe, IUser } from './react/types/types';
import MyAccount from './react/app/MyAccount/MyAccount';
import Home from './react/app/Home/Home';
import RecipeForm from './react/app/RecipeForm/RecipeForm';
import authFetch from './react/authFetch';
import { UserContext } from './react/UserContext';
import UserAccess from './react/app/Login/UserAccess';
import setDefaultDescription from './utils';
import Loading from './react/components/Loading';

export default function App() {
  const [recipes, setRecipes] = useState<IRecipe[]>([]);
  const [favoriteIds, setFavoriteIds] = useState<number[]>([]);
  const [user, setUser] = useState<IUser | undefined>(undefined);

  const fetchUser = useCallback(() => {
    authFetch('/me').then(json => {
      if (json) setUser(json);
    });
  }, [setUser]);

  function fetchRecipes() {
    authFetch('/recipe').then(json => {
      setRecipes(json ? json.map(setDefaultDescription) : []);
    });
  }

  function fetchFavoriteIds() {
    authFetch('/recipe/favoriteIds').then(json => setFavoriteIds(json));
  }

  useEffect(() => {
    fetchRecipes();
    fetchUser();
    fetchFavoriteIds();
  }, [fetchUser]);

  if (!recipes || (user && !favoriteIds))
    return <Loading title="Loading..." subtitle="Please wait..." />;

  return (
    <UserContext.Provider
      value={{ user, setUser, favoriteIds, setFavoriteIds, fetchFavoriteIds }}
    >
      <Navbar />

      <Route exact path="/">
        <Home recipes={recipes} />
      </Route>
      <Route path="/recipe/:id">
        <Recipe recipes={recipes} />
      </Route>
      <Route path="/create-recipe">
        <RecipeForm fetchRecipes={fetchRecipes} />
      </Route>
      <Route path="/myAccount">
        <MyAccount />
      </Route>
      <Route path="/login">
        <UserAccess />
      </Route>

      <Footer />
    </UserContext.Provider>
  );
}
