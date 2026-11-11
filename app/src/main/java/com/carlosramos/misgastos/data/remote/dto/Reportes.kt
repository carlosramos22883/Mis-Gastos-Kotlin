package com.carlosramos.misgastos.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Reportes (GET /api/reportes/{reporte}). A diferencia de los recursos, los montos vienen como número (igual que el dashboard).
 * Período: ciclo (cualquier fecha dentro) o desde/hasta; tarjetas y "nos deben" muestran cómo están hoy.
 */

data class PeriodoReporte(
    val desde: String,
    val hasta: String,
    @SerializedName("es_ciclo") val esCiclo: Boolean,
    val anterior: String?,
    val siguiente: String?,
    val texto: String
)

/** cortes: solo en el estado de cuenta cuando se elige una tarjeta. */
data class RespuestaReporte<T>(
    val titulo: String,
    val periodo: PeriodoReporte,
    val data: T?,
    val cortes: List<CorteReporte>? = null
)

data class CorteReporte(val id: Int, @SerializedName("fecha_corte") val fechaCorte: String, val total: Double)

/** Un movimiento en un reporte. clase: efectivo | cuenta | tarjeta */
data class FilaReporte(
    val clase: String,
    val id: Int,
    val fecha: String,
    val descripcion: String,
    val categoria: String?,
    val color: String?,
    val donde: String,
    @SerializedName("user_id") val userId: Int?,
    val monto: Double
)

data class CategoriaReporte(
    val nombre: String,
    val color: String?,
    val monto: Double,
    val porcentaje: Double,
    val cantidad: Int,
    val movimientos: List<FilaReporte>? = null,
    @SerializedName("por_ciclo") val porCiclo: List<Double>? = null
)

// ---------- Ejecución financiera ----------

data class CajaReporte(
    @SerializedName("saldo_inicial") val saldoInicial: Double,
    val ingresos: Double,
    val egresos: Double,
    @SerializedName("saldo_final") val saldoFinal: Double
)

data class CuentaReporte(
    val id: Int,
    val nombre: String,
    val color: String?,
    @SerializedName("logo_url") val logoUrl: String?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("saldo_inicial") val saldoInicial: Double,
    val ingresos: Double,
    val egresos: Double,
    @SerializedName("saldo_final") val saldoFinal: Double
)

data class CompromisoReporte(
    val id: Int,
    val descripcion: String,
    val fecha: String,
    val color: String?,
    val monto: Double,
    val pagado: Double,
    val pendiente: Double,
    val estado: String,
    @SerializedName("estado_texto") val estadoTexto: String
)

data class TarjetaReporte(
    val id: Int,
    val nombre: String,
    val color: String?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("proximo_corte") val proximoCorte: String?,
    @SerializedName("fecha_limite") val fechaLimite: String?,
    @SerializedName("por_pagar") val porPagar: Double,
    val limite: Double,
    val disponible: Double,
    val deuda: Double
)

data class DeudorReporte(val id: Int, val nombre: String, val pendiente: Double)

data class TotalesEjecucion(
    val ingresos: Double,
    val gastos: Double,
    val ahorro: Double,
    val efectivo: Double,
    val cuentas: Double,
    val compromisos: Double,
    @SerializedName("compromisos_pagado") val compromisosPagado: Double,
    @SerializedName("por_pagar") val porPagar: Double,
    val liquidez: Double,
    @SerializedName("tarjetas_por_pagar") val tarjetasPorPagar: Double,
    @SerializedName("tarjetas_disponible") val tarjetasDisponible: Double,
    @SerializedName("tarjetas_deuda") val tarjetasDeuda: Double,
    @SerializedName("nos_deben") val nosDeben: Double
)

data class Ejecucion(
    val ingresos: List<FilaReporte>,
    val gastos: List<CategoriaReporte>,
    val efectivo: CajaReporte,
    val cuentas: List<CuentaReporte>,
    val compromisos: List<CompromisoReporte>,
    val tarjetas: List<TarjetaReporte>,
    @SerializedName("nos_deben") val nosDeben: List<DeudorReporte>,
    val totales: TotalesEjecucion
)

// ---------- Gastos por categoría ----------

data class CicloRango(val inicio: String, val fin: String)

data class GastosReporte(
    val ciclos: List<CicloRango>,
    val categorias: List<CategoriaReporte>,
    @SerializedName("por_ciclo") val porCiclo: List<Double>,
    val total: Double,
    @SerializedName("promedio_por_ciclo") val promedioPorCiclo: Double
)

// ---------- Préstamos ----------

data class PagoReporte(val fecha: String, val capital: Double, val interes: Double, val cargos: Double, val total: Double)

data class PrestamoReporte(
    val id: Int,
    val nombre: String,
    val acreedor: String?,
    val color: String?,
    @SerializedName("user_id") val userId: Int?,
    val activo: Boolean,
    @SerializedName("monto_original") val montoOriginal: Double,
    @SerializedName("tasa_anual") val tasaAnual: Double?,
    @SerializedName("plazo_meses") val plazoMeses: Int?,
    @SerializedName("fecha_otorgamiento") val fechaOtorgamiento: String?,
    val saldo: Double,
    @SerializedName("cuota_total") val cuotaTotal: Double?,
    val avance: Double,
    @SerializedName("pagos_realizados") val pagosRealizados: Int,
    @SerializedName("pagado_capital") val pagadoCapital: Double,
    @SerializedName("pagado_intereses") val pagadoIntereses: Double,
    @SerializedName("pagado_cargos") val pagadoCargos: Double,
    @SerializedName("pagado_total") val pagadoTotal: Double,
    @SerializedName("meses_restantes") val mesesRestantes: Int?,
    @SerializedName("fecha_fin") val fechaFin: String?,
    @SerializedName("intereses_restantes") val interesesRestantes: Double?,
    val pagos: List<PagoReporte>
)

data class TotalesPrestamos(
    val saldo: Double,
    @SerializedName("cuotas_al_mes") val cuotasAlMes: Double,
    @SerializedName("pagado_capital") val pagadoCapital: Double,
    @SerializedName("pagado_intereses") val pagadoIntereses: Double,
    @SerializedName("pagado_cargos") val pagadoCargos: Double,
    @SerializedName("intereses_restantes") val interesesRestantes: Double
)

data class PrestamosReporte(val prestamos: List<PrestamoReporte>, val totales: TotalesPrestamos)

// ---------- Estado de cuenta ----------

data class TarjetaEstadoReporte(val id: Int, val nombre: String, val color: String?, @SerializedName("ultimos_digitos") val ultimosDigitos: String?)

data class LineaEstado(val fecha: String?, val descripcion: String, val categoria: String?, val tipo: String?, val cuota: String?, val monto: Double)

data class EstadoReporte(
    val tarjeta: TarjetaEstadoReporte,
    val id: Int,
    @SerializedName("fecha_inicio") val fechaInicio: String?,
    @SerializedName("fecha_corte") val fechaCorte: String,
    @SerializedName("fecha_limite_pago") val fechaLimitePago: String?,
    val total: Double,
    val pagado: Double,
    val pendiente: Double,
    val estado: String?,
    @SerializedName("compromiso_id") val compromisoId: Int?,
    val lineas: List<LineaEstado>
)
