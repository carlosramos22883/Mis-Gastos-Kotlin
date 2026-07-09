package com.carlosramos.misgastos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.carlosramos.misgastos.ui.auth.AuthUiState
import com.carlosramos.misgastos.ui.auth.AuthViewModel
import com.carlosramos.misgastos.ui.auth.LoginScreen
import com.carlosramos.misgastos.ui.auth.RegisterScreen
import com.carlosramos.misgastos.ui.dashboard.DashboardScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.LOGIN
) {
    // Inyectamos el ViewModel usando Hilt
    val authViewModel: AuthViewModel = hiltViewModel()

    // Observamos el estado de autenticación y el estado de la UI
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val uiState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.LOGIN) {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.login(email, password)
                },
                onGoogleLoginClick = { idToken ->
                    authViewModel.loginWithGoogle(idToken)
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.REGISTER)
                },
                uiState = uiState
            )
        }

        composable(Screen.REGISTER) {
            RegisterScreen(
                onRegisterClick = { name, email, password, confirmation ->
                    authViewModel.register(name, email, password, confirmation)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                uiState = uiState
            )
        }

        composable(Screen.DASHBOARD) {
            // Si el login/register fue exitoso, obtenemos el nombre del usuario
            val userName = if (uiState is AuthUiState.Success) {
                (uiState as AuthUiState.Success).user.name
            } else {
                "Usuario"
            }

            DashboardScreen(
                userName = userName,
                onLogoutClick = {
                    authViewModel.logout()
                    // Limpiamos el backstack para que no pueda volver al login con el botón atrás
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }
    }

    // Efecto colateral: Si cambia el estado de autenticación, navegamos al Dashboard
    if (isAuthenticated) {
        // Navegamos solo si no estamos ya en el dashboard
        if (navController.currentDestination?.route != Screen.DASHBOARD) {
            navController.navigate(Screen.DASHBOARD) {
                popUpTo(Screen.LOGIN) { inclusive = true }
            }
        }
    }
}