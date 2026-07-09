package com.carlosramos.misgastos.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.carlosramos.misgastos.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extensión de Context para crear una instancia única de DataStore.
 * "by preferencesDataStore" garantiza que solo haya UNA instancia en toda la app.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_NAME
)

/**
 * Implementación real y persistente del TokenProvider usando DataStore.
 * El token se guarda en el dispositivo y persiste entre sesiones.
 */
@Singleton
class DataStoreTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenProvider {

    // Clave con la que guardaremos el token en DataStore
    private val authTokenKey = stringPreferencesKey(Constants.KEY_AUTH_TOKEN)

    /**
     * Obtiene el token guardado.
     * Si no existe, devuelve null.
     */
    override suspend fun getToken(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[authTokenKey] }
            .first()
    }

    /**
     * Guarda el token en DataStore.
     */
    override suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[authTokenKey] = token
        }
    }

    /**
     * Elimina el token (útil para logout).
     */
    override suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(authTokenKey)
        }
    }
}