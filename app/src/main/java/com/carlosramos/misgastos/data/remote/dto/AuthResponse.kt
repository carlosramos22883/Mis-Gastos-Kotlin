package com.carlosramos.misgastos.data.remote.dto

/**
 * Respuesta que devuelve Laravel Sanctum al hacer login/register.
 * Ajusta los campos según lo que devuelva tu API.
 *
 * Ejemplo de respuesta típica de Sanctum:
 * {
 *   "user": { "id": 1, "name": "Carlos", "email": "..." },
 *   "token": "1|abc123..."
 * }
 */
data class AuthResponse(
    val user: UserResponse? = null,
    val token: String? = null
)