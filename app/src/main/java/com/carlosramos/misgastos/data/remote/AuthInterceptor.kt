package com.carlosramos.misgastos.data.remote

import com.carlosramos.misgastos.data.local.TokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor de OkHttp que inyecta automáticamente el token Bearer
 * de Sanctum en cada petición a la API (excepto en login/register).
 *
 * Esto es lo que hace posible que Laravel reconozca al usuario autenticado.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Rutas que NO necesitan token (autenticación pública)
        val publicPaths = listOf("/login", "/register", "/forgot-password", "/reset-password")
        val isPublicPath = publicPaths.any { originalRequest.url.encodedPath.contains(it) }

        // Si es una ruta pública, no inyectamos token
        if (isPublicPath) {
            return chain.proceed(originalRequest)
        }

        // Obtenemos el token (runBlocking porque intercept() no es suspend)
        val token = runBlocking { tokenProvider.getToken() }

        // Si no hay token, enviamos la petición sin autenticación
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Inyectamos el header "Authorization: Bearer {token}"
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}