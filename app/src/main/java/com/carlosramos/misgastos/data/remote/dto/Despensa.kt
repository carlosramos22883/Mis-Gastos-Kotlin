package com.carlosramos.misgastos.data.remote.dto

import com.google.gson.annotations.SerializedName

// ---------- Despensa (/api/productos) ----------

data class EstadoDespensa(
    @SerializedName("en_casa") val enCasa: Int?,
    @SerializedName("por_acabarse") val porAcabarse: Boolean = false,
    @SerializedName("vence_el") val venceEl: String? = null,
    val vencimiento: Vencimiento? = null,
    val falta: Boolean = false,
    @SerializedName("por_comprar") val porComprar: Int = 1
)

data class Vencimiento(val texto: String, val aviso: Boolean)

data class EstadoRespuesta(val estado: EstadoDespensa)

data class Producto(
    val id: Int,
    val nombre: String,
    val marca: String?,
    val cantidad: String?,
    val unidad: String?,
    val presentacion: String?,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("codigo_barras") val codigoBarras: String?,
    @SerializedName("categoria_despensa_id") val categoriaDespensaId: Int,
    val categoria: String?,
    val importancia: String,
    val duracion: String,
    @SerializedName("acepta_otra_marca") val aceptaOtraMarca: Boolean = false,
    @SerializedName("cantidad_ideal") val cantidadIdeal: Int?,
    @SerializedName("avisar_cuando") val avisarCuando: Int?,
    val notas: String?,
    val activo: Boolean = true,
    @SerializedName("foto_url") val fotoUrl: String?,
    val estado: EstadoDespensa?,
    val precios: List<PrecioProducto>? = null,
    val historial: List<PrecioProducto>? = null,
    @SerializedName("en_linea") val enLinea: List<EnlaceEnLinea>? = null
) {
    val basico get() = importancia == "basico"
}

data class PrecioProducto(
    val id: Int,
    @SerializedName("tienda_id") val tiendaId: Int,
    val tienda: String?,
    val precio: String,
    val unidades: Int,
    @SerializedName("por_unidad") val porUnidad: String,
    val origen: String,
    val fecha: String
)

data class EnlaceEnLinea(
    val fuente: String,
    val codigo: String,
    val nombre: String,
    val url: String?,
    val imagen: String?,
    val unidades: Int = 1,
    @SerializedName("consultado_at") val consultadoAt: String?,
    val error: String?
)

data class ResumenDespensa(val faltan: Int, @SerializedName("por_vencer") val porVencer: Int)
data class MetaDespensa(val resumen: ResumenDespensa)
data class ListaProductos(val data: List<Producto>, val meta: MetaDespensa)

/** Campos del formulario (los vacíos van como null). */
data class ProductoBody(
    val nombre: String,
    val marca: String?,
    val cantidad: String?,
    val unidad: String?,
    @SerializedName("codigo_barras") val codigoBarras: String?,
    @SerializedName("categoria_despensa_id") val categoriaDespensaId: Int?,
    val importancia: String,
    val duracion: String,
    @SerializedName("acepta_otra_marca") val aceptaOtraMarca: Boolean,
    @SerializedName("cantidad_ideal") val cantidadIdeal: Int?,
    @SerializedName("avisar_cuando") val avisarCuando: Int?,
    @SerializedName("en_casa") val enCasa: Int?,
    @SerializedName("vence_el") val venceEl: String?,
    val notas: String?,
    val activo: Boolean
)

data class DespensaBody(val accion: String, val valor: String? = null)

data class PrecioBody(@SerializedName("tienda_id") val tiendaId: Int, val precio: String, val unidades: Int?, val fecha: String?)

/** Un resultado de la búsqueda en una tienda en línea (se manda tal cual al ligarlo, con "unidades"). */
data class ResultadoEnLinea(
    val codigo: String,
    val nombre: String,
    val marca: String?,
    val precio: Double?,
    val ean: String?,
    val url: String?,
    val imagen: String?,
    @SerializedName("mismo_codigo") val mismoCodigo: Boolean = false,
    val unidades: Int? = null
)

data class ResultadosEnLinea(val resultados: List<ResultadoEnLinea>)

// ---------- Tiendas (/api/tiendas) ----------

data class Tienda(
    val id: Int,
    val nombre: String,
    val fuente: String?,
    @SerializedName("en_linea") val enLinea: Boolean = false,
    val notas: String?,
    val activo: Boolean = true,
    @SerializedName("precios_count") val preciosCount: Int? = null
)

data class TiendaBody(val nombre: String, val fuente: String?, val notas: String?, val activo: Boolean)

data class TiendasComunes(val data: List<Tienda>, val agregadas: Int)

// ---------- Compras (/api/compras) ----------

data class ResumenLista(
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("en_carrito") val enCarrito: Int,
    val total: String,
    @SerializedName("sin_precio") val sinPrecio: Int,
    @SerializedName("por_pagar") val porPagar: Int,
    @SerializedName("por_pagar_total") val porPagarTotal: String,
    val pagado: String
)

data class Comparacion(
    @SerializedName("tienda_id") val tiendaId: Int,
    val tienda: String,
    @SerializedName("por_unidad") val porUnidad: String,
    val cuando: String,
    val viejo: Boolean,
    val origen: String
)

data class ItemLista(
    val id: Int,
    @SerializedName("producto_id") val productoId: Int?,
    val descripcion: String,
    val cantidad: Int,
    val automatico: Boolean,
    val comprado: Boolean,
    val precio: String?,
    val subtotal: String?,
    @SerializedName("tienda_id") val tiendaId: Int?,
    val tienda: String?,
    @SerializedName("no_habia") val noHabia: String?,
    val extra: Boolean = false,
    val pagado: Boolean = false,
    val categoria: String?,
    val basico: Boolean = false,
    @SerializedName("foto_url") val fotoUrl: String?,
    val comparacion: List<Comparacion> = emptyList()
)

data class GastoPago(@SerializedName("categoria_personal_id") val categoriaId: Int?, val categoria: String?, val monto: String)

data class PagoCompra(
    val id: Int,
    val tienda: String?,
    val origen: String,
    @SerializedName("origen_nombre") val origenNombre: String,
    val total: String,
    val fecha: String,
    val gastos: List<GastoPago>
)

data class ListaCompra(
    val id: Int,
    val nombre: String,
    val abierta: Boolean,
    @SerializedName("terminada_at") val terminadaAt: String?,
    @SerializedName("tienda_actual_id") val tiendaActualId: Int?,
    @SerializedName("tienda_actual") val tiendaActual: String?,
    @SerializedName("items_count") val itemsCount: Int? = null,
    @SerializedName("comprados_count") val compradosCount: Int? = null,
    val resumen: ResumenLista? = null,
    val pendientes: List<ItemLista> = emptyList(),
    val comprados: List<ItemLista> = emptyList(),
    val pagos: List<PagoCompra> = emptyList(),
    @SerializedName("consultas_restantes") val consultasRestantes: Int? = null
)

data class Compras(val abierta: ListaCompra?, val historial: List<ListaCompra>, val faltan: Int)

data class ItemBody(@SerializedName("producto_id") val productoId: Int? = null, val descripcion: String? = null, val cantidad: Int? = null)

/** PATCH de una cosa: solo los campos que cambian (Gson omite los null). */
data class ItemCambio(val cantidad: Int? = null, val comprado: Boolean? = null, @SerializedName("no_habia") val noHabia: Boolean? = null, val precio: String? = null)

data class TiendaActualBody(@SerializedName("tienda_id") val tiendaId: Int?)

// «Pagar aquí»
data class Opcion(val clave: String, val nombre: String)
data class CategoriaOpcion(val id: Int, val nombre: String)
data class ItemPorPagar(val id: Int, val descripcion: String, val cantidad: Int, val precio: String?, val subtotal: String?)
data class Ciclo(val inicio: String, val fin: String)
data class TiendaCorta(val id: Int, val nombre: String)

data class DatosPago(
    val tienda: TiendaCorta,
    val items: List<ItemPorPagar>,
    val total: String,
    @SerializedName("sin_precio") val sinPrecio: Int,
    val origenes: List<Opcion>,
    val categorias: List<CategoriaOpcion>,
    @SerializedName("categoria_sugerida") val categoriaSugerida: Int?,
    val ciclo: Ciclo
)

data class ExtraBody(val descripcion: String, val precio: String, val categoria: String)
data class DecisionBody(val modo: String, val traspasos: List<TraspasoBody>? = null)

data class PagoBody(
    val origen: String,
    val fecha: String,
    @SerializedName("user_id") val userId: Int?,
    val categoria: String?,
    @SerializedName("monto_lista") val montoLista: String?,
    @SerializedName("extras_modo") val extrasModo: String,
    @SerializedName("extras_total") val extrasTotal: String? = null,
    @SerializedName("extras_categoria") val extrasCategoria: String? = null,
    val extras: List<ExtraBody>? = null,
    val presupuestos: Map<String, DecisionBody>? = null,
    val confirmar: Boolean? = null
)

// Consulta de precios en línea
data class EstadoConsulta(val hechos: Int, val total: Int, val nuevos: Int, val reusados: Int, val fallas: Int, val terminada: Boolean, val espera: Long? = null, val producto: String? = null)
data class InicioConsulta(@SerializedName("al_dia") val alDia: Boolean, @SerializedName("consulta_id") val consultaId: Int?, val estado: EstadoConsulta?, val message: String?)

// Ruta de compra
data class ItemRuta(val id: Int, val descripcion: String, val cantidad: Int, @SerializedName("por_unidad") val porUnidad: String? = null, val subtotal: String? = null, val cuando: String? = null, val viejo: Boolean = false)
data class Parada(@SerializedName("tienda_id") val tiendaId: Int, val tienda: String, val subtotal: String, val items: List<ItemRuta>)
data class UnaTienda(val tienda: String, val total: String, @SerializedName("sin_cubrir") val sinCubrir: Int)
data class Ruta(
    @SerializedName("costo_visita") val costoVisita: String,
    val paradas: List<Parada>,
    val total: String?,
    @SerializedName("costo_ir") val costoIr: String?,
    @SerializedName("una_tienda") val unaTienda: UnaTienda?,
    val ahorro: String?,
    @SerializedName("sin_precio") val sinPrecio: List<ItemRuta>,
    val viejos: Int
)

/** Valores fijos de la despensa (dentro de /api/catalogos): clave => texto. */
data class ValoresDespensa(val importancias: Map<String, String>, val duraciones: Map<String, String>, val unidades: Map<String, String>, val fuentes: Map<String, String>)
