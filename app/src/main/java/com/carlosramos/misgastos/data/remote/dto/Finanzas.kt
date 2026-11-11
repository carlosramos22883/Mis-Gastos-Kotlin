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
    val proximos: List<Proximo>,
    /** Tope de gasto del ciclo (null = sin tope) y presupuestos (súper, gasolina…) del ciclo */
    val tope: TopeGasto? = null,
    val presupuestos: List<PresupuestoResumen>? = null
)

data class TopeGasto(
    val tope: Double,
    val gastado: Double,
    val queda: Double,
    @SerializedName("por_dia") val porDia: Double,
    val porcentaje: Int,
    @SerializedName("dias_restantes") val diasRestantes: Int
)

/** queda < 0 = se pasó; comprometido = compromisos de su categoría aún por pagar (ya apartados) */
data class PresupuestoResumen(
    val id: Int,
    val descripcion: String,
    val color: String?,
    val monto: Double,
    val gastado: Double,
    val comprometido: Double,
    val queda: Double
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
    val activo: Boolean,
    /** false = de sistema o en uso de forma que no se edita (ej. "Saldo inicial") */
    val editable: Boolean = true
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
    /** Lo pagado de más a una tarjeta: quedó como crédito en ella. */
    val excedente: String? = null,
    val abonos: List<MovimientoEfectivo>? = null,
    /** Presupuesto (súper, gasolina…): no se paga, se descuenta con los gastos de su categoría */
    @SerializedName("es_presupuesto") val esPresupuesto: Boolean? = null,
    val presupuesto: PresupuestoDetalle? = null
)

data class PresupuestoDetalle(
    val monto: String,
    val gastado: String,
    val comprometido: String,
    val queda: String,
    val movimientos: List<MovimientoPresupuesto>? = null
)

/** Un gasto que descuenta el presupuesto; origen = de dónde salió (Efectivo, la cuenta o la tarjeta). */
data class MovimientoPresupuesto(val clase: String, val id: Int, val fecha: String, val descripcion: String, val origen: String, val monto: String)

data class Recurrente(
    val id: Int,
    val frecuencia: String,
    @SerializedName("descripcion_frecuencia") val descripcionFrecuencia: String?,
    val activo: Boolean,
    @SerializedName("es_presupuesto") val esPresupuesto: Boolean? = null,
    val descripcion: String? = null,
    val monto: String? = null,
    @SerializedName("dia_semana") val diaSemana: Int? = null,
    @SerializedName("dia_pago") val diaPago: Int? = null,
    @SerializedName("dia_pago_2") val diaPago2: Int? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String? = null,
    @SerializedName("fecha_fin") val fechaFin: String? = null,
    @SerializedName("total_cuotas") val totalCuotas: Int? = null,
    @SerializedName("categoria_personal_id") val categoriaId: Int? = null,
    @SerializedName("medio_pago") val medioPago: String? = null
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
    /** Ruta en storage ("avatars/x.webp") o URL completa (Google) */
    val avatar: String? = null,
    @SerializedName("es_administrador") val esAdministrador: Boolean
)

data class Contacto(
    val id: Int,
    val nombre: String? = null,
    val apellido: String? = null,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("tipo_contacto_id") val tipoContactoId: Int? = null,
    val tipo: String?,
    val telefono: String? = null,
    val email: String? = null,
    val notas: String? = null,
    @SerializedName("foto_url") val fotoUrl: String?,
    val activo: Boolean,
    val prestado: String? = null,
    val pagado: String? = null,
    val pendiente: String? = null,
    @SerializedName("deudas_abiertas") val deudasAbiertas: Int = 0,
    val deudas: List<Deuda>? = null
)

/** origen: efectivo | cuenta:{id} | tarjeta:{id} | ninguno (solo anotar) */
data class Deuda(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val descripcion: String,
    val comentario: String?,
    val fecha: String,
    val origen: String,
    val monto: String,
    val pagado: String,
    val pendiente: String,
    val estado: String,
    val pagos: List<DeudaPago>
)

data class DeudaPago(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val fecha: String,
    val monto: String,
    val destino: String,
    val comentario: String?
)

data class ContactoBody(
    val nombre: String,
    val apellido: String?,
    @SerializedName("tipo_contacto_id") val tipoContactoId: Int,
    val telefono: String?,
    val email: String?,
    val notas: String?,
    val activo: Boolean = true
)

data class DeudaBody(
    val descripcion: String,
    val monto: String,
    val fecha: String,
    val origen: String,
    val comentario: String?,
    @SerializedName("user_id") val userId: Int?
)

/** destino: efectivo | cuenta:{id} | ninguno · deuda_id: a cuál se aplica (null = de la más antigua a la más reciente) */
data class DeudaPagoBody(
    val monto: String,
    val fecha: String,
    val destino: String,
    @SerializedName("deuda_id") val deudaId: Int?,
    val comentario: String?,
    @SerializedName("user_id") val userId: Int?
)

data class TipoSimple(val id: Int, val nombre: String)

data class OpcionLogo(val id: Int, val nombre: String, @SerializedName("logo_url") val logoUrl: String?)

data class Catalogos(
    @SerializedName("tipos_contacto") val tiposContacto: List<TipoSimple>,
    @SerializedName("tipos_cuenta") val tiposCuenta: List<TipoSimple> = emptyList(),
    @SerializedName("tipos_prestamo") val tiposPrestamo: List<TipoSimple> = emptyList(),
    @SerializedName("tipos_ingreso") val tiposIngreso: List<TipoSimple> = emptyList(),
    val bancos: List<OpcionLogo> = emptyList(),
    @SerializedName("marcas_red") val marcasRed: List<OpcionLogo> = emptyList()
)

// ---------- Préstamos (/api/prestamos) ----------

data class CargoPrestamo(val nombre: String, val monto: String?)

data class PrestamoPago(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val fecha: String,
    val origen: String,
    val planilla: String?,
    val capital: String,
    val interes: String,
    val cargos: String?,
    val mora: String?,
    val total: String,
    val comentario: String?
)

data class SugerenciaPago(val fecha: String, val capital: String, val interes: String, val cargos: String, val mora: String, val total: String)

data class DatosPrestamo(
    @SerializedName("contacto_id") val contactoId: Int?,
    val acreedor: String?,
    @SerializedName("tasa_mora") val tasaMora: String?,
    val cuota: String?,
    @SerializedName("saldo_inicial") val saldoInicial: String?,
    @SerializedName("fecha_saldo") val fechaSaldo: String?,
    val notas: String?
)

data class Prestamo(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val nombre: String,
    @SerializedName("tipo_prestamo_id") val tipoPrestamoId: Int? = null,
    @SerializedName("banco_id") val bancoId: Int? = null,
    val tipo: String?,
    val acreedor: String?,
    val banco: BancoLogo? = null,
    val contacto: ContactoMini? = null,
    @SerializedName("monto_original") val montoOriginal: String,
    @SerializedName("tasa_anual") val tasaAnual: String?,
    @SerializedName("plazo_meses") val plazoMeses: Int?,
    @SerializedName("fecha_otorgamiento") val fechaOtorgamiento: String?,
    @SerializedName("dia_pago") val diaPago: Int?,
    @SerializedName("medio_pago") val medioPago: String?,
    @SerializedName("monto_planilla") val montoPlanilla: String?,
    val color: String?,
    val activo: Boolean,
    val cargos: List<CargoPrestamo>,
    val saldo: String,
    val cuota: String?,
    @SerializedName("cuota_total") val cuotaTotal: String?,
    val avance: Double?,
    @SerializedName("proxima_fecha") val proximaFecha: String?,
    @SerializedName("fecha_fin") val fechaFin: String?,
    @SerializedName("meses_restantes") val mesesRestantes: Int?,
    @SerializedName("intereses_restantes") val interesesRestantes: String?,
    val pagos: List<PrestamoPago>? = null,
    @SerializedName("sugerencia_pago") val sugerenciaPago: SugerenciaPago? = null,
    val datos: DatosPrestamo? = null
)

/** origen: efectivo | cuenta:{id} | planilla | ninguno */
data class PrestamoPagoBody(
    val fecha: String,
    val capital: String,
    val interes: String,
    val cargos: String?,
    val mora: String?,
    val origen: String,
    val comentario: String?,
    @SerializedName("user_id") val userId: Int?
)

data class TablaResumen(
    val meses: Int,
    val cuota: Double?,
    @SerializedName("total_intereses") val totalIntereses: Double,
    @SerializedName("total_pagado") val totalPagado: Double,
    @SerializedName("fecha_fin") val fechaFin: String?
)

data class PagarHoy(val monto: Double, @SerializedName("interes_a_hoy") val interesAHoy: Double, val ahorro: Double)

data class EscenarioExtra(val monto: Double, val tabla: TablaResumen, val ahorro: Double, @SerializedName("meses_menos") val mesesMenos: Int)

data class EscenarioAbono(
    val monto: Double,
    val modo: String,
    val tabla: TablaResumen,
    val ahorro: Double,
    @SerializedName("meses_menos") val mesesMenos: Int,
    @SerializedName("cuota_nueva") val cuotaNueva: Double?
)

data class Proyeccion(
    val base: TablaResumen?,
    @SerializedName("pagar_hoy") val pagarHoy: PagarHoy?,
    @SerializedName("extra_mensual") val extraMensual: EscenarioExtra?,
    val abono: EscenarioAbono?
)

// ---------- Ingresos (/api/ingresos) ----------

data class ConceptoIngreso(val nombre: String, val tipo: String, val monto: String?, val porcentaje: String?, val tope: String?, @SerializedName("prestamo_id") val prestamoId: Int?)

data class LineaRecibo(val nombre: String, val tipo: String, val monto: String, @SerializedName("prestamo_pago_id") val prestamoPagoId: Int? = null)

data class Recibo(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val fecha: String,
    val bruto: String,
    val neto: String,
    val destino: String,
    val comentario: String?,
    val lineas: List<LineaRecibo>
)

data class FuenteIngreso(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val nombre: String,
    @SerializedName("tipo_ingreso_id") val tipoIngresoId: Int? = null,
    val tipo: String?,
    val pagador: String?,
    val frecuencia: String,
    @SerializedName("dia_pago") val diaPago: Int?,
    @SerializedName("dia_pago_2") val diaPago2: Int?,
    @SerializedName("monto_bruto") val montoBruto: String?,
    val destino: String?,
    val color: String?,
    val activo: Boolean,
    val conceptos: List<ConceptoIngreso>,
    val recibos: List<Recibo>? = null
)

data class LineaPropuesta(val nombre: String, val tipo: String, val monto: Double, @SerializedName("prestamo_id") val prestamoId: Int?)

data class Propuesta(val bruto: Double, val lineas: List<LineaPropuesta>, val neto: Double)

data class LineaReciboBody(val nombre: String, val tipo: String, val monto: String, @SerializedName("prestamo_id") val prestamoId: Int?)

/** destino: efectivo | cuenta:{id} | ninguno */
data class ReciboBody(
    val fecha: String,
    val bruto: String,
    val destino: String,
    val comentario: String?,
    val lineas: List<LineaReciboBody>,
    @SerializedName("user_id") val userId: Int?
)

// ---------- Cuentas (/api/cuentas) ----------

data class TipoCuentaMini(val id: Int, val nombre: String)

data class BancoLogo(val id: Int, val nombre: String, @SerializedName("logo_url") val logoUrl: String?)

data class Cuenta(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val nombre: String,
    val color: String?,
    @SerializedName("tipo_cuenta") val tipoCuenta: TipoCuentaMini?,
    val banco: BancoLogo?,
    @SerializedName("saldo_inicial") val saldoInicial: String,
    @SerializedName("fecha_inicial") val fechaInicial: String?,
    val saldo: String,
    val activo: Boolean
)

data class TransferenciaMini(val id: Int, val origen: String, val destino: String, val eliminable: Boolean)

data class MovimientoCuenta(
    val id: Int,
    @SerializedName("cuenta_id") val cuentaId: Int,
    @SerializedName("user_id") val userId: Int?,
    val contacto: ContactoMini?,
    val descripcion: String,
    val monto: String,
    val fecha: String,
    val tipo: String,
    val categoria: CategoriaMini?,
    @SerializedName("compromiso_id") val compromisoId: Int?,
    val transferencia: TransferenciaMini?,
    @SerializedName("gestionado_en") val gestionadoEn: String?,
    val editable: Boolean
)

data class ResumenCuenta(
    @SerializedName("saldo_inicial") val saldoInicial: String,
    val ingresos: String,
    val egresos: String,
    @SerializedName("saldo_final") val saldoFinal: String
)

data class PaginaCuenta(
    val data: List<MovimientoCuenta>,
    val meta: MetaPagina,
    val cuenta: Cuenta,
    val ciclo: CicloEfectivo,
    val resumen: ResumenCuenta
)

data class MovimientoCuentaBody(
    val tipo: String,
    val descripcion: String,
    val monto: String,
    val fecha: String,
    @SerializedName("categoria_personal_id") val categoriaId: Int?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("contacto_id") val contactoId: String?
)

// ---------- Transferencias (/api/transferencias) ----------

data class OpcionTransferencia(val valor: String, val nombre: String, val saldo: String)

data class OpcionesTransferencia(val origenes: List<OpcionTransferencia>, val destinos: List<OpcionTransferencia>)

data class TransferenciaBody(
    val origen: String,
    val destino: String,
    val monto: String,
    val fecha: String,
    val descripcion: String?,
    @SerializedName("user_id") val userId: Int?
)

// ---------- Tarjetas (/api/tarjetas) ----------

data class PorPagarTarjeta(@SerializedName("compromiso_id") val compromisoId: Int, val monto: String, val fecha: String?)

data class Tarjeta(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val nombre: String,
    @SerializedName("ultimos_digitos") val ultimosDigitos: String?,
    val color: String?,
    @SerializedName("color_texto") val colorTexto: String?,
    val banco: BancoLogo?,
    val marca: BancoLogo?,
    @SerializedName("limite_credito") val limite: String,
    @SerializedName("tasa_interes") val tasaInteres: String?,
    @SerializedName("permite_avances") val permiteAvances: Boolean,
    @SerializedName("dia_corte") val diaCorte: Int,
    @SerializedName("dia_pago") val diaPago: Int,
    val deuda: String,
    val disponible: String,
    /** Intereses bonificables del último corte, ya restados del disponible (el banco los resuelve en el corte siguiente). */
    @SerializedName("intereses_retenidos") val interesesRetenidos: String? = null,
    /** false = el banco solo resta el capital (Davivienda): intereses, comisiones e IVA no bajan el disponible hasta pagarse. */
    @SerializedName("cargos_restan_disponible") val cargosRestanDisponible: Boolean? = null,
    @SerializedName("cargos_sin_restar") val cargosSinRestar: String? = null,
    @SerializedName("proximo_corte") val proximoCorte: String?,
    @SerializedName("por_pagar") val porPagar: PorPagarTarjeta?,
    val activo: Boolean
)

data class MovimientoTarjeta(
    val id: Int,
    @SerializedName("tarjeta_id") val tarjetaId: Int,
    @SerializedName("user_id") val userId: Int?,
    val contacto: ContactoMini?,
    val tipo: String,
    @SerializedName("tipo_texto") val tipoTexto: String,
    val descripcion: String,
    val comentario: String?,
    val monto: String,
    val fecha: String,
    val cuotas: Int?,
    val categoria: CategoriaMini?,
    @SerializedName("transferencia_id") val transferenciaId: Int?,
    @SerializedName("gestionado_en") val gestionadoEn: String?,
    val editable: Boolean
)

data class LineaPorFacturar(
    @SerializedName("movimiento_id") val movimientoId: Int,
    val descripcion: String,
    val fecha: String,
    @SerializedName("cuota_numero") val cuotaNumero: Int?,
    val cuotas: Int?,
    val monto: String
)

/** saldoAFavor (≤ 0): el último corte quedó a favor; ya va restado en total. */
data class PorFacturar(val total: String, val lineas: List<LineaPorFacturar>, @SerializedName("saldo_a_favor") val saldoAFavor: String? = null)

data class PeriodoAbierto(val desde: String, val corte: String?)

data class EnCuotas(val movimiento: MovimientoTarjeta, @SerializedName("cuotas_cobradas") val cuotasCobradas: Int)

data class EstadoCuenta(
    val id: Int,
    @SerializedName("fecha_corte") val fechaCorte: String,
    @SerializedName("fecha_limite_pago") val fechaLimitePago: String?,
    val total: String,
    /** ≤ 0: saldo a favor del corte anterior (ya va en total). */
    @SerializedName("saldo_anterior") val saldoAnterior: String? = null,
    /** Los trae el estado de cuenta del banco; los del último corte se apartan del disponible hasta el corte siguiente. */
    @SerializedName("intereses_bonificables") val interesesBonificables: String? = null,
    @SerializedName("compromiso_id") val compromisoId: Int?,
    val estado: String?,
    @SerializedName("saldo_pendiente") val saldoPendiente: String?
)

data class InteresesBody(@SerializedName("intereses_bonificables") val interesesBonificables: String)

data class CostoNoPagar(
    @SerializedName("interes_mes") val interesMes: Double,
    val tasa: Double
)

data class PagoPendienteTarjeta(val compromiso: Compromiso, @SerializedName("costo_de_no_pagar") val costo: CostoNoPagar?)

data class TarjetaDetalle(
    val tarjeta: Tarjeta,
    @SerializedName("periodo_abierto") val periodoAbierto: PeriodoAbierto,
    @SerializedName("por_facturar") val porFacturar: PorFacturar,
    val movimientos: List<MovimientoTarjeta>,
    @SerializedName("en_cuotas") val enCuotas: List<EnCuotas>,
    @SerializedName("estados_cuenta") val estadosCuenta: List<EstadoCuenta>,
    @SerializedName("por_pagar") val porPagar: List<PagoPendienteTarjeta>,
    val pagos: List<PagoTarjeta> = emptyList(),
    @SerializedName("cargos_fijos") val cargosFijos: List<CargoFijo> = emptyList(),
    /** Cargos fijos que se sumarán al cortar (aún no aplicados) */
    @SerializedName("cargos_proximo_corte") val cargosProximoCorte: String = "0.00"
)

/** Lo que la tarjeta cobra en cada corte (seguro, membresía…); se agrega solo al estado de cuenta. */
data class CargoFijo(
    val id: Int,
    val descripcion: String,
    val monto: String,
    val desde: String,
    val activo: Boolean,
    @SerializedName("categoria_personal_id") val categoriaId: Int? = null
)

/** desde: primer corte en el que se cobra (Y-m-d). */
data class CargoFijoBody(
    val descripcion: String,
    val monto: String,
    val desde: String,
    val activo: Boolean,
    @SerializedName("categoria_personal_id") val categoriaId: Int?
)

/** Un pago hecho a la tarjeta (clase: efectivo | cuenta | tarjeta). */
data class PagoTarjeta(
    val clase: String,
    val id: Int,
    val fecha: String,
    val descripcion: String,
    val desde: String,
    val monto: String,
    @SerializedName("user_id") val userId: Int?
)

/** tipo: compra | cargo | credito (los avances van por transferencia). */
data class MovimientoTarjetaBody(
    val tipo: String,
    val descripcion: String,
    val monto: String,
    val fecha: String,
    val cuotas: Int?,
    @SerializedName("categoria_personal_id") val categoriaId: Int?,
    val comentario: String?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("contacto_id") val contactoId: String?
)

/**
 * Crear (único o recurrente) o editar un pago. Para recurrente: frecuencia semanal | quincenal | mensual,
 * dia_semana (0 = domingo) o dia_pago (31 = último día) y dia_pago_2 (quincenal); finalizacion indefinido | fecha | cuotas.
 */
data class CompromisoBody(
    val descripcion: String,
    val monto: String,
    @SerializedName("medio_pago") val medioPago: String,
    @SerializedName("categoria_personal_id") val categoriaId: Int?,
    val comentario: String?,
    @SerializedName("contacto_id") val contactoId: String?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("recurrencia_tipo") val recurrenciaTipo: String?,
    @SerializedName("fecha_esperada") val fechaEsperada: String?,
    val frecuencia: String? = null,
    @SerializedName("dia_semana") val diaSemana: Int? = null,
    @SerializedName("dia_pago") val diaPago: Int? = null,
    @SerializedName("dia_pago_2") val diaPago2: Int? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String? = null,
    val finalizacion: String? = null,
    @SerializedName("fecha_fin") val fechaFin: String? = null,
    @SerializedName("total_cuotas") val totalCuotas: Int? = null,
    @SerializedName("es_presupuesto") val esPresupuesto: Boolean? = null
)

// ---------- Alta y edición (los nulos no se envían: en la edición, lo que no viaja no cambia) ----------

data class CuentaBody(
    val nombre: String,
    @SerializedName("tipo_cuenta_id") val tipoCuentaId: Int,
    @SerializedName("banco_id") val bancoId: Int?,
    val color: String,
    /** Solo al crear */
    @SerializedName("saldo_inicial") val saldoInicial: String?,
    @SerializedName("fecha_inicial") val fechaInicial: String?,
    val activo: Boolean,
    @SerializedName("user_id") val userId: Int?
)

data class TarjetaBody(
    val nombre: String,
    @SerializedName("banco_id") val bancoId: Int?,
    @SerializedName("marca_red_id") val marcaRedId: Int?,
    @SerializedName("ultimos_digitos") val ultimosDigitos: String?,
    val color: String,
    @SerializedName("limite_credito") val limite: String,
    @SerializedName("tasa_interes") val tasaInteres: String?,
    @SerializedName("dia_corte") val diaCorte: Int,
    @SerializedName("dia_pago") val diaPago: Int,
    val activo: Boolean,
    @SerializedName("permite_avances") val permiteAvances: Boolean,
    @SerializedName("cargos_restan_disponible") val cargosRestanDisponible: Boolean,
    /** Solo al crear: disponible hoy y, si hay, el estado de cuenta pendiente */
    val disponible: String?,
    @SerializedName("tiene_pendiente") val tienePendiente: Boolean?,
    @SerializedName("pendiente_monto") val pendienteMonto: String?,
    @SerializedName("pendiente_fecha") val pendienteFecha: String?,
    @SerializedName("user_id") val userId: Int?
)

data class CargoBody(val nombre: String, val monto: String)

data class PrestamoBody(
    val nombre: String,
    @SerializedName("tipo_prestamo_id") val tipoPrestamoId: Int,
    @SerializedName("banco_id") val bancoId: Int?,
    val acreedor: String?,
    @SerializedName("monto_original") val montoOriginal: String,
    @SerializedName("tasa_anual") val tasaAnual: String,
    @SerializedName("plazo_meses") val plazoMeses: Int?,
    @SerializedName("fecha_otorgamiento") val fechaOtorgamiento: String?,
    @SerializedName("dia_pago") val diaPago: Int?,
    val cuota: String?,
    @SerializedName("saldo_inicial") val saldoInicial: String,
    @SerializedName("fecha_saldo") val fechaSaldo: String,
    @SerializedName("medio_pago") val medioPago: String,
    @SerializedName("monto_planilla") val montoPlanilla: String?,
    val color: String?,
    val notas: String?,
    val activo: Boolean,
    val cargos: List<CargoBody>,
    @SerializedName("crear_compromiso") val crearCompromiso: Boolean?,
    @SerializedName("contacto_id") val contactoId: String?,
    @SerializedName("user_id") val userId: Int?
)

/** Un concepto de la plantilla: monto fijo o porcentaje del bruto (con tope opcional); prestamo_id = se registra como pago de ese préstamo. */
data class ConceptoBody(
    val nombre: String,
    val tipo: String,
    val monto: String?,
    val porcentaje: String?,
    val tope: String?,
    @SerializedName("prestamo_id") val prestamoId: Int?
)

/** conceptos == null: la plantilla se mantiene; una lista (aun vacía) la reemplaza completa. */
data class FuenteBody(
    val nombre: String,
    @SerializedName("tipo_ingreso_id") val tipoIngresoId: Int,
    val pagador: String?,
    val frecuencia: String,
    @SerializedName("dia_pago") val diaPago: Int?,
    @SerializedName("dia_pago_2") val diaPago2: Int?,
    @SerializedName("monto_bruto") val montoBruto: String?,
    val destino: String,
    val color: String?,
    val activo: Boolean,
    @SerializedName("user_id") val userId: Int?,
    val conceptos: List<ConceptoBody>? = null
)

// ---------- Perfil y categorías ----------

data class PerfilBody(val name: String, val email: String)

data class PerfilRespuesta(val message: String?, val user: UserResponse?)

data class AvatarRespuesta(val message: String?, @SerializedName("avatar_url") val avatarUrl: String?)

data class CategoriaBody(val nombre: String, val tipo: String, val color: String, val activo: Boolean)
