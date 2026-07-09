package com.carlosramos.misgastos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LaravelErrorResponse(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)