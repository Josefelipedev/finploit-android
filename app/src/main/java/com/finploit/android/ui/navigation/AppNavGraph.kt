package com.finploit.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.finploit.android.ui.auth.LoginScreen
import com.finploit.android.ui.auth.LoginViewModel
import com.finploit.android.ui.main.MainScreen

sealed class Route(val path: String) {
    data object Login : Route("login")
    data object Main : Route("main")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Route.Main.path else Route.Login.path,
    ) {
        composable(Route.Login.path) {
            LoginScreen(
                viewModel = hiltViewModel(),
                onLoginSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Main.path) {
            MainScreen(
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                }
            )
        }
    }
}
