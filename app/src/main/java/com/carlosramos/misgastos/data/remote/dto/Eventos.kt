package com.carlosramos.misgastos.data.remote.dto

import com.google.gson.annotations.SerializedName

// ---------- Eventos: dividir la cuenta (/api/eventos) ----------

/**
 * Un evento como una tabla: participantes (filas) × consumos/productos (columnas). En cada producto, "cantidades" =
 * cuántos pidió cada participante, en su orden. estado de un participante: pagador, festejado, hogar (pagó el hogar y
 * es un miembro), pendiente, parcial o pagado. fotoUrl = foto de la cuenta (con el token).
 */
data class Evento(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val nombre: String,
    val fecha: String,
    val lugar: String?,
    val comentario: String?,
    @SerializedName("propina_tipo") val propinaTipo: String,
    @SerializedName("propina_valor") val propinaValor: String,
    val origen: String?,
    @SerializedName("origen_valor") val origenValor: String?,
    @SerializedName("categoria_personal_id") val categoriaId: Int?,
    @SerializedName("pago_el_hogar") val pagoElHogar: Boolean,
    val subtotal: String,
    val propina: String,
    val total: String,
    @SerializedName("por_cobrar") val porCobrar: String,
    val participantes: List<ParticipanteEvento>,
    val consumos: List<ConsumoEvento>,
    @SerializedName("foto_url") val fotoUrl: String? = null
) {
    val pagador get() = participantes.firstOrNull { it.esPagador }
}

data class ParticipanteEvento(
    val id: Int,
    val quien: String,
    val nombre: String,
    @SerializedName("user_id") val userId: Int?,
    val contacto: ContactoMini?,
    @SerializedName("es_festejado") val esFestejado: Boolean,
    @SerializedName("es_pagador") val esPagador: Boolean,
    val consumo: String,
    val propina: String,
    val festejo: String,
    val total: String,
    val pagado: String,
    val pendiente: String,
    val estado: String,
    @SerializedName("fecha_pago") val fechaPago: String?,
    @SerializedName("deuda_id") val deudaId: Int?,
    val cobros: List<CobroEvento> = emptyList(),
    val detalle: List<DetalleEvento> = emptyList()
)

data class CobroEvento(val id: Int, val fecha: String, val monto: String, val destino: String?)

data class DetalleEvento(val descripcion: String, val cantidad: Int, val precio: String, val monto: String)

data class ConsumoEvento(val id: Int, val descripcion: String, val precio: String, val cantidad: Int, val total: String, val cantidades: List<Int>)

data class OpcionEvento(val valor: String, val etiqueta: String)

/** participantes: "miembro:{id}" o id de contacto; origenes: de dónde pagar; destinos: a dónde entra un cobro. */
data class OpcionesEvento(val participantes: List<OpcionEvento>, val origenes: List<OpcionEvento>, val destinos: List<OpcionEvento>)

data class ParticipanteEventoBody(val quien: String, @SerializedName("es_festejado") val esFestejado: Boolean)

/** cantidades: cuántos pidió cada participante, en el orden de participantes. */
data class ConsumoEventoBody(val descripcion: String, val precio: String, val cantidades: List<Int>)

data class EventoBody(
    val nombre: String,
    val fecha: String,
    val lugar: String?,
    @SerializedName("propina_tipo") val propinaTipo: String,
    @SerializedName("propina_valor") val propinaValor: String?,
    val participantes: List<ParticipanteEventoBody>,
    val pagador: Int,
    val consumos: List<ConsumoEventoBody>,
    val origen: String?,
    @SerializedName("categoria_personal_id") val categoriaId: Int?,
    val confirmar: Boolean? = null
)

data class CobroEventoBody(val monto: String, val fecha: String, val destino: String, @SerializedName("user_id") val userId: Int?)

data class PagadoEventoBody(val fecha: String, val origen: String?, @SerializedName("categoria_personal_id") val categoriaId: Int?, val confirmar: Boolean? = null)
