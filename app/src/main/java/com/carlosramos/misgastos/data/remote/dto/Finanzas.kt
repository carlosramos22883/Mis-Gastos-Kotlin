package com.carlosramos.misgastos.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Modelos de la API de finanzas (ver /api/documentation en el servidor).
 * Los montos de los recursos vienen como texto ("12.50"); los del dashboard, como número.
 */

/** Respuesta envuelta en "data" (recursos de Laravel). */
data class Envoltura<T>(val data: T)

data class MetaPagina(
    @SerializedName("current_page") val paginaActual: Int,
    @SerializedName("last_page") val ultimaPagina: Int,
    val total: Int
)

// ---------- Mini objetos que se repiten ----------

data class BancoMini(val id: Int, val nombre: String, val logo: String? = null)

data class MarcaMini(val id: Int, val nombre: String, val logo: String? = null)

data class CategoriaMini(val id: Int, val nombre: String, val color: String?)

data class ContactoMini(
    val id: Int,
    val nombre: String,
    @SerializedName("foto_url") val fotoUrl: String?
)

data class TarjetaMini(val id: Int, val nombre: String?, val color: String?)

// ---------- Dashboard (GET /api/dashboard) ----------

data class Dashboard(
    val hoy: String,
    val ciclo: CicloResumen,
    val efectivo: Double,
    val cuentas: List<CuentaResumen>,
    @SerializedName("total_cuentas") val totalCuentas: Double,
    val tarjetas: List<TarjetaResumen>,
    @SerializedName("tarjetas_totales") val tarjetasTotales: TarjetasTotales,
    @SerializedName("por_pagar_ciclo") val porPagarCiclo: Double,
    val liquidez: Double,
    @SerializedName("deuda_prestamos") val deudaPrestamos: Double,
    @SerializedName("cuotas_prestamos") val cuotasPrestamos: Double,
    @SerializedName("nos_deben") val nosDeben: Double,
    @SerializedName("gastos_por_categoria") val gastosPorCategoria: List<GastoCategoria>,
    val historial: List<CicloHistorial>,
    val alertas: List<Alerta>,
    val proximos: List<Proximo>
)

data class CicloResumen(
    val inicio: String,
    val fin: String,
    @SerializedName("dias_restantes") val diasRestantes: Int
)

data class CuentaResumen(
    val id: Int,
    val nombre: String,
    val color: String?,
    val banco: BancoMini?,
    @SerializedName("user_id") val userId: Int,
    val saldo: Double
)

data class TarjetaResumen(
    val id: Int,
    val nombre: String,
    val color: String?,
    val banco: BancoMini?,
    val marca: MarcaMini?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("dia_corte") val diaCorte: Int,
    @SerializedName("dia_pago") val diaPago: Int,
    val limite: Double,
    val disponible: Double,
    val deuda: Double,
    @SerializedName("permite_avances") val permiteAvances: Boolean,
    @SerializedName("por_pagar") val porPagar: Double,
    val vence: String?
)

data class TarjetasTotales(
    val limite: Double,
    val disponible: Double,
    @SerializedName("disponible_avances") val disponibleAvances: Double,
    val deuda: Double,
    @SerializedName("por_pagar") val porPagar: Double
)

data class GastoCategoria(val nombre: String, val color: String?, val monto: Double)

data class CicloHistorial(val inicio: String, val fin: String, val ingresos: Double, val gastos: Double)

/** nivel: critico | serio | aviso | info */
data class Alerta(val nivel: String, val etiqueta: String, val texto: String, val url: String?)

data class Proximo(
    val id: Int,
    val descripcion: String,
    val fecha: String,
    val monto: Double,
    val vencido: Boolean,
    val color: String?
)

// ---------- Efectivo (/api/efectivo) ----------

data class MovimientoEfectivo(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val contacto: ContactoMini?,
    val descripcion: String,
    val monto: String,
    val fecha: String,
    val tipo: String,
    val categoria: CategoriaMini?,
    @SerializedName("compromiso_id") val compromisoId: Int?,
    @SerializedName("es_saldo_arrastrado") val esSaldoArrastrado: Boolean,
    val editable: Boolean
)

data class PaginaEfectivo(
    val data: List<MovimientoEfectivo>,
    val meta: MetaPagina,
    val ciclo: CicloEfectivo,
    val resumen: ResumenEfectivo
)

data class CicloEfectivo(
    val inicio: String,
    val fin: String,
    @SerializedName("es_actual") val esActual: Boolean,
    val anterior: String?,
    val siguiente: String?
)

data class ResumenEfectivo(val ingresos: String, val egresos: String, val saldo: String)

/** Cuerpo para crear o editar un movimiento de efectivo. Gson omite los nulos. */
data class MovimientoEfectivoBody(
    val tipo: String,
    val descripcion: String,
    val monto: String,
    val fecha: String,
    @SerializedName("categoria_personal_id") val categoriaId: Int?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("contacto_id") val contactoId: String?
)

// ---------- Categorías (/api/categorias) ----------

data class Categoria(
    val id: Int,
    val nombre: String,
    val tipo: String,
    val color: String?,
    val activo: Boolean
)

// ---------- Compromisos (/api/compromisos) ----------

data class Compromiso(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val contacto: ContactoMini?,
    val descripcion: String,
    val comentario: String?,
    val monto: String,
    @SerializedName("saldo_pendiente") val saldoPendiente: String,
    @SerializedName("monto_pagado") val montoPagado: String,
    @SerializedName("fecha_esperada") val fechaEsperada: String?,
    @SerializedName("numero_cuota") val numeroCuota: Int?,
    @SerializedName("total_cuotas") val totalCuotas: Int?,
    @SerializedName("medio_pago") val medioPago: String?,
    val estado: String,
    @SerializedName("estado_visual") val estadoVisual: String,
    @SerializedName("estado_texto") val estadoTexto: String,
    val categoria: CategoriaMini?,
    val recurrente: Recurrente?,
    val tarjeta: TarjetaMini?,
    val abonos: List<MovimientoEfectivo>? = null
)

data class Recurrente(
    val id: Int,
    val frecuencia: String,
    @SerializedName("descripcion_frecuencia") val descripcionFrecuencia: String?,
    val activo: Boolean
)

data class PaginaCompromisos(
    val data: List<Compromiso>,
    val meta: MetaPagina,
    val ciclo: CicloSimple
)

data class CicloSimple(val inicio: String, val fin: String)

/** origen: "efectivo" | "cuenta:{id}" | "tarjeta:{id}" */
data class PagoCompromisoBody(
    val origen: String,
    val descripcion: String,
    val monto: String,
    val fecha: String,
    @SerializedName("categoria_personal_id") val categoriaId: Int?,
    @SerializedName("user_id") val userId: Int?
)

// ---------- Hogar y contactos ----------

data class Hogar(
    val id: Int,
    val nombre: String,
    @SerializedName("es_administrador") val esAdministrador: Boolean,
    @SerializedName("fecha_corte_dia") val fechaCorteDia: Int,
    val miembros: List<Miembro>
)

data class Miembro(
    val id: Int,
    val nombre: String,
    val email: String,
    @SerializedName("es_administrador") val esAdministrador: Boolean
)

data class Contacto(
    val id: Int,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    val tipo: String?,
    @SerializedName("foto_url") val fotoUrl: String?,
    val activo: Boolean
)
