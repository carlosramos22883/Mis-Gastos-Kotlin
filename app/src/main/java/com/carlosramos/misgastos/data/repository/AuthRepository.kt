package com.carlosramos.misgastos.data.repository

import com.carlosramos.misgastos.data.local.TokenProvider
import com.carlosramos.misgastos.data.remote.ApiService
import com.carlosramos.misgastos.data.remote.dto.LoginRequest
import com.carlosramos.misgastos.data.remote.dto.RegisterRequest
import com.carlosramos.misgastos.data.remote.dto.UserResponse
import com.carlosramos.misgastos.data.remote.dto.GoogleLoginRequest
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.carlosramos.misgastos.data.remote.dto.LaravelErrorResponse

/**
 * Resultado sellado (sealed class) para representar los posibles resultados
 * de las operaciones de autenticación.
 *
 * Usamos sealed classes porque:
 * - Son type-safe (el compilador verifica que manejes todos los casos)
 * - Son exhaustivas (cuando usas when, Kotlin te obliga a manejar todos los casos)
 */
sealed class AuthResult {
    data class Success(val user: UserResponse) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
    object Idle : AuthResult()
}

/**
 * Repositorio de autenticación.
 * Orquesta las llamadas a la API y el manejo del token.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenProvider: TokenProvider
) {

    /**
     * Iniciar sesión con email y password.
     * Si es exitoso, guarda el token en DataStore.
     */
    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val token = authResponse.token
                val user = authResponse.user

                if (token != null && user != null) {
                    // Guardar el token en DataStore
                    tokenProvider.saveToken(token)
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Respuesta inválida del servidor")
                }
            } else {
                // ️ MEJORA: Parsear el error de Laravel
                val errorBody = response.errorBody()?.string()
                val laravelError = com.google.gson.Gson().fromJson(
                    errorBody,
                    LaravelErrorResponse::class.java
                )

                val errorMessage = laravelError?.message
                    ?: "Error: ${response.code()}"

                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    /**
     * Registrar un nuevo usuario.
     * Si es exitoso, guarda el token en DataStore.
     */
    suspend fun register(name: String, email: String, password: String, passwordConfirmation: String): AuthResult {
        return try {
            val response = apiService.register(
                RegisterRequest(name, email, password, passwordConfirmation)
            )

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val token = authResponse.token
                val user = authResponse.user

                if (token != null && user != null) {
                    tokenProvider.saveToken(token)
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Respuesta inválida del servidor")
                }
            } else {
                // ️ MEJORA: Parsear el error de Laravel
                val errorBody = response.errorBody()?.string()
                val laravelError = com.google.gson.Gson().fromJson(
                    errorBody,
                    LaravelErrorResponse::class.java
                )

                val errorMessage = laravelError?.message
                    ?: "Error: ${response.code()}"

                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    /**
     * Cerrar sesión.
     * Invalida el token en el servidor y lo borra de DataStore.
     */
    suspend fun logout(): AuthResult {
        return try {
            val response = apiService.logout()

            // Borrar el token localmente (independientemente de si el servidor responde bien)
            tokenProvider.clearToken()

            if (response.isSuccessful) {
                AuthResult.Success(UserResponse(0, "", "")) // Usuario vacío, solo importa que fue exitoso
            } else {
                AuthResult.Error("Error al cerrar sesión: ${response.message()}")
            }
        } catch (e: Exception) {
            // Incluso si hay error de red, borramos el token localmente
            tokenProvider.clearToken()
            AuthResult.Error("Error de conexión, pero tu sesión se cerró localmente")
        }
    }

    /**
     * Verificar si el usuario está autenticado (tiene un token guardado).
     */
    suspend fun isAuthenticated(): Boolean {
        return tokenProvider.getToken() != null
    }

    /**
     * Obtener el usuario autenticado desde el servidor.
     */
    suspend fun getAuthenticatedUser(): AuthResult {
        return try {
            val response = apiService.getAuthenticatedUser()

            if (response.isSuccessful && response.body() != null) {
                AuthResult.Success(response.body()!!)
            } else {
                // Si el token es inválido o expiró, lo borramos
                if (response.code() == 401) {
                    tokenProvider.clearToken()
                }
                AuthResult.Error("Error al obtener usuario: ${response.message()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    /**
     * Login con Google.
     * Recibe el ID token de Google y lo envía a Laravel.
     */
    suspend fun loginWithGoogle(idToken: String): AuthResult {
        return try {
            val response = apiService.loginWithGoogle(GoogleLoginRequest(idToken))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val token = authResponse.token
                val user = authResponse.user

                if (token != null && user != null) {
                    tokenProvider.saveToken(token)
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Respuesta inválida del servidor")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val laravelError = com.google.gson.Gson().fromJson(
                    errorBody,
                    LaravelErrorResponse::class.java
                )

                val errorMessage = laravelError?.message
                    ?: "Error al autenticar con Google: ${response.code()}"

                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }
}