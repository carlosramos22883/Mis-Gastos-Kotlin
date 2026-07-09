package com.carlosramos.misgastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.carlosramos.misgastos.ui.auth.AuthViewModel
import com.carlosramos.misgastos.ui.navigation.AppNavHost
import com.carlosramos.misgastos.ui.theme.MisGastosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MisGastosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Verificamos el estado de autenticación al abrir la app
                    val authViewModel: AuthViewModel = hiltViewModel()
                    authViewModel.checkAuthStatus()

                    AppNavHost(navController = navController)
                }
            }
        }
    }
}