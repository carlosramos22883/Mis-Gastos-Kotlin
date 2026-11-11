package com.carlosramos.misgastos.util

import com.carlosramos.misgastos.BuildConfig

object Constants {
    // URL del servidor Laravel, según el tipo de build (ver app/build.gradle.kts):
    //   - debug: emulador, http://10.0.2.2:8081/api/ (el localhost de la computadora)
    //   - release: celular en la red de la casa, http://IP-de-la-Mac:8081/api/
    val BASE_URL: String = BuildConfig.API_URL

    // Nombre del archivo de DataStore (para guardar el token)
    const val DATASTORE_NAME = "mis_gastos_preferences"
    const val KEY_AUTH_TOKEN = "auth_token"
}