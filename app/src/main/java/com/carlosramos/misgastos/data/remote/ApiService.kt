package com.carlosramos.misgastos.data.remote

import com.carlosramos.misgastos.data.remote.dto.AuthResponse
import com.carlosramos.misgastos.data.remote.dto.LoginRequest
import com.carlosramos.misgastos.data.remote.dto.RegisterRequest
import com.carlosramos.misgastos.data.remote.dto.UserResponse
import com.carlosramos.misgastos.data.remote.dto.GoogleLoginRequest
import com.carlosramos.misgastos.data.remote.dto.ForgotPasswordRequest
import com.carlosramos.misgastos.data.remote.dto.ResetPasswordRequest
import com.carlosramos.misgastos.data.remote.dto.PasswordResetResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz que define todos los endpoints de la API de Laravel.
 * Cada función representa una petición HTTP.
 */
interface ApiService {

    /**
     * Iniciar sesión con email y password.
     * Laravel Sanctum devuelve un token Bearer.
     * POST /api/login
     */
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Registrar un nuevo usuario.
     * POST /api/register
     */
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /**
     * Cerrar sesión (invalida el token actual).
     * POST /api/logout
     */
    @POST("logout")
    suspend fun logout(): Response<Unit>

    /**
     * Obtener el usuario autenticado.
     * GET /api/user
     */
    @GET("user")
    suspend fun getAuthenticatedUser(): Response<UserResponse>

    /**
     * Login con Google.
     * Envía el ID token de Google a Laravel.
     * POST /api/auth/google
     */
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>


    /**
     * Enviar link de recuperación de contraseña.
     * POST /api/forgot-password
     */
    @POST("forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<PasswordResetResponse>

    /**
     * Resetear contraseña con el token recibido por email.
     * POST /api/reset-password
     */
    @POST("reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<PasswordResetResponse>
}