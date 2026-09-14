package com.example.movieapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.movieapp.feature.auth.AuthViewModel
import com.example.movieapp.feature.auth.LoginScreen
import com.example.movieapp.feature.auth.RegisterScreen
import com.example.movieapp.feature.auth.SplashScreen
import com.example.movieapp.feature.auth.SplashViewModel
import com.example.movieapp.feature.detail.MovieDetailScreen
import com.example.movieapp.feature.detail.MovieDetailViewModel
import com.example.movieapp.feature.home.HomeScreen
import com.example.movieapp.feature.home.HomeViewModel
import com.example.movieapp.feature.player.PlayerScreen
import com.example.movieapp.feature.player.PlayerViewModel
import com.example.movieapp.feature.profile.FavoritesScreen
import com.example.movieapp.feature.profile.ProfileFormScreen
import com.example.movieapp.feature.profile.ProfileListScreen
import com.example.movieapp.feature.profile.SettingsScreen
import com.example.movieapp.feature.profile.WatchHistoryScreen
import com.example.movieapp.feature.search.SearchScreen
import com.example.movieapp.feature.subscription.PlansScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        modifier = modifier
    ) {
        composable(NavRoutes.SPLASH) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            SplashScreen(
                viewModel = splashViewModel,
                onAuthenticated = {
                    navController.navigate(NavRoutes.PROFILE_LIST) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                },
                onUnauthenticated = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoutes.PROFILE_LIST) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.REGISTER)
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            val authViewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = { _ ->
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.PUBLIC_HOME) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onMovieClick = { movieId ->
                    navController.navigate(NavRoutes.movieDetail(movieId))
                },
                onContinueWatchingClick = { movieId, playableId, sourceItemId ->
                    navController.navigate(NavRoutes.player(movieId, playableId, sourceItemId, "public"))
                }
            )
        }

        composable(NavRoutes.HOME) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onMovieClick = { movieId ->
                    navController.navigate(NavRoutes.movieDetail(movieId))
                },
                onContinueWatchingClick = { movieId, playableId, sourceItemId ->
                    navController.navigate(NavRoutes.player(movieId, playableId, sourceItemId, "active"))
                }
            )
        }

        composable(NavRoutes.SEARCH) {
            SearchScreen(
                onMovieClick = { movieId ->
                    navController.navigate(NavRoutes.movieDetail(movieId))
                }
            )
        }

        composable(NavRoutes.FAVORITES) {
            FavoritesScreen(
                onMovieClick = { movieId ->
                    navController.navigate(NavRoutes.movieDetail(movieId))
                }
            )
        }

        composable(NavRoutes.WATCH_HISTORY) {
            WatchHistoryScreen(
                onPlayClick = { movieId, playableId, sourceItemId ->
                    navController.navigate(NavRoutes.player(movieId, playableId, sourceItemId, "active"))
                }
            )
        }

        composable(NavRoutes.PROFILE_LIST) {
            ProfileListScreen(
                onProfileSelected = { _ ->
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.PROFILE_LIST) { inclusive = true }
                    }
                },
                onCreateProfileClick = {
                    navController.navigate(NavRoutes.PROFILE_CREATE)
                },
                onEditProfileClick = { profileId ->
                    navController.navigate(NavRoutes.profileEdit(profileId))
                }
            )
        }

        composable(NavRoutes.PROFILE_CREATE) {
            ProfileFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoutes.PROFILE_EDIT,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) {
            ProfileFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoutes.MOVIE_DETAIL,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            val detailViewModel: MovieDetailViewModel = hiltViewModel()
            MovieDetailScreen(
                movieId = movieId,
                viewModel = detailViewModel,
                profileId = null,
                onPlayClick = { mId, pId, sId, profId ->
                    navController.navigate(NavRoutes.player(mId, pId, sId, profId ?: "active"))
                }
            )
        }

        composable(
            route = NavRoutes.PLAYER,
            arguments = listOf(
                navArgument("movieId") { type = NavType.StringType },
                navArgument("playableId") { type = NavType.StringType },
                navArgument("sourceItemId") { type = NavType.StringType },
                navArgument("profileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            val playableId = backStackEntry.arguments?.getString("playableId") ?: ""
            val sourceItemId = backStackEntry.arguments?.getString("sourceItemId") ?: ""
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            val playerViewModel: PlayerViewModel = hiltViewModel()

            PlayerScreen(
                movieId = movieId,
                playableId = playableId,
                sourceItemId = sourceItemId,
                profileId = profileId,
                viewModel = playerViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.PLANS) {
            PlansScreen(
                onSubscriptionSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.PLANS) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onLoggedOut = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}


