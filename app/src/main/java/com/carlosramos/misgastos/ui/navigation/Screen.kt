package com.carlosramos.misgastos.ui.navigation

/**
 * Define las rutas de la aplicación.
 * Usar un objeto con constantes evita errores de tipeo.
 */
object Screen {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password/{email}"

    fun resetPasswordRoute(email: String) = "reset_password/$email"
}