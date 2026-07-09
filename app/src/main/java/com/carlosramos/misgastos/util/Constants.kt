package com.carlosramos.misgastos.util

object Constants {
    // URL del servidor Laravel
    // Si estás probando en local desde el emulador de Android:
    //   - Usa "10.0.2.2" para acceder a "localhost" de tu computadora
    //   - Ejemplo: http://10.0.2.2:8081/api/
    // Si tu API está en un servidor real:
    //   - Ejemplo: https://misgastos.com/api/

    const val BASE_URL = "http://10.0.2.2:8081/api/"

    // Nombre del archivo de DataStore (para guardar el token)
    const val DATASTORE_NAME = "mis_gastos_preferences"
    const val KEY_AUTH_TOKEN = "auth_token"
}