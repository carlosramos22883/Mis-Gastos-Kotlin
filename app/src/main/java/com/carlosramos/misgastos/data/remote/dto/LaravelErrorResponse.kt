package com.carlosramos.misgastos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LaravelErrorResponse(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
    /** El gasto pasa su presupuesto (error "presupuesto"): de qué categorías se puede cubrir. */
    val presupuesto: ExcesoPresupuesto? = null
)