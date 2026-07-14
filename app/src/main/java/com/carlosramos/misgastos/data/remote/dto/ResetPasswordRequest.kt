package com.carlosramos.misgastos.data.remote.dto

data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val password: String,
    val password_confirmation: String
)