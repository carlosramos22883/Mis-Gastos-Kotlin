package com.carlosramos.misgastos.data.remote.dto

import com.google.gson.annotations.SerializedName

// ---------- Proyecciones (/api/proyecciones): ¿cuándo salgo de mis deudas? ----------
// Montos como texto ("12.50"); meses como "Y-m". Un hito o un "cero" en null = más de 15 años.

data class ProyeccionDeudas(
    val base: BaseProyeccion,
    val escenarios: List<EscenarioMini>,
    val ver: List<Int>,
    val comparacion: List<ComparacionEscenario>
)

data class BaseProyeccion(
    @SerializedName("mes_inicio") val mesInicio: String,
    val ingreso: String,
    val gasto: String,
    /** "tope" (el del ciclo) o "promedio" (de los últimos 3 ciclos) */
    @SerializedName("gasto_origen") val gastoOrigen: String,
    @SerializedName("cuotas_efectivo") val cuotasEfectivo: String,
    val sobra: String,
    val grupos: GruposDeuda,
    val deudas: List<DeudaProyeccion>
)

data class GruposDeuda(val tarjetas: String, val bancos: String, val personas: String, val hipoteca: String)

data class DeudaProyeccion(val clave: String, val tipo: String, val nombre: String, val saldo: String, val tasa: String, val hipoteca: Boolean)

/** Escenario guardado (se arma en la web); aquí solo se elige para comparar. */
data class EscenarioMini(val id: Int, val nombre: String)

data class ComparacionEscenario(
    /** null = "Si sigo igual" */
    val id: Int?,
    val nombre: String,
    /** mas_cara | mas_pequena | personalizado */
    val orden: String,
    /** intereses | rotar */
    @SerializedName("modo_tarjetas") val modoTarjetas: String,
    val hitos: HitosProyeccion,
    val totales: TotalesProyeccion,
    val meta: MetaProyeccion?,
    val deudas: List<DeudaCero>,
    val serie: List<PuntoSerie>
)

data class HitosProyeccion(val tarjetas: String?, val personas: String?, @SerializedName("sin_deudas") val sinDeudas: String?)

data class TotalesProyeccion(
    @SerializedName("intereses_tarjetas") val interesesTarjetas: String,
    @SerializedName("perdida_rotar") val perdidaRotar: String,
    @SerializedName("prestamos_nuevos") val prestamosNuevos: String,
    val faltante: String,
    @SerializedName("meses_con_faltante") val mesesConFaltante: Int
)

/** extra: lo que falta al mes para llegar a tiempo ("0.00" = ya se logra; null = ni con $20,000) */
data class MetaProyeccion(val que: String, val mes: String, val extra: String?, @SerializedName("logrado_en") val logradoEn: String?)

data class DeudaCero(val clave: String, val nombre: String, val cero: String?)

data class PuntoSerie(val mes: String, val total: String)
