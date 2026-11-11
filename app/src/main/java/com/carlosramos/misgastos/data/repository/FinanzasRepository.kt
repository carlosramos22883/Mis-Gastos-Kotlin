package com.carlosramos.misgastos.data.repository

import com.carlosramos.misgastos.data.local.TokenProvider
import com.carlosramos.misgastos.data.remote.ApiService
import com.carlosramos.misgastos.data.remote.FinanzasApi
import com.carlosramos.misgastos.data.remote.dto.LaravelErrorResponse
import com.carlosramos.misgastos.data.remote.dto.MovimientoEfectivoBody
import com.carlosramos.misgastos.data.remote.dto.PagoCompromisoBody
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de una llamada a la API: los datos o el mensaje de Laravel (con los errores por campo). */
sealed interface Resultado<out T> {
    data class Ok<T>(val datos: T) : Resultado<T>
    data class Falla(val mensaje: String, val campos: Map<String, String> = emptyMap()) : Resultado<Nothing>
}

/** Lo que usan todos los formularios: quién soy, el hogar, las categorías y los contactos. */
data class Comunes(
    val usuarioId: Int,
    val nombre: String,
    val email: String,
    val avatar: String? = null,
    val hogar: com.carlosramos.misgastos.data.remote.dto.Hogar?,
    val categorias: List<com.carlosramos.misgastos.data.remote.dto.Categoria>,
    val contactos: List<com.carlosramos.misgastos.data.remote.dto.Contacto>
) {
    val miembros get() = hogar?.miembros.orEmpty()
    fun categoriasDe(tipo: String) = categorias.filter { it.activo && it.tipo == tipo && it.nombre != "Saldo inicial" }
}

/** Avisa a la interfaz que el token ya no sirve (401) para volver al login. */
@Singleton
class Sesion @Inject constructor() {
    private val _expirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expirada: SharedFlow<Unit> = _expirada.asSharedFlow()
    fun expiro() { _expirada.tryEmit(Unit) }
}

@Singleton
class FinanzasRepository @Inject constructor(
    private val api: FinanzasApi,
    private val authApi: ApiService,
    private val tokenProvider: TokenProvider,
    private val sesion: Sesion
) {
    private var comunes: Comunes? = null

    /** Datos comunes (en caché hasta [refrescar] o cerrar sesión). */
    suspend fun comunes(refrescar: Boolean = false): Resultado<Comunes> {
        comunes?.takeUnless { refrescar }?.let { return Resultado.Ok(it) }
        val usuario = llamar { authApi.getAuthenticatedUser() }
        if (usuario is Resultado.Falla) return usuario
        val categorias = categorias()
        if (categorias is Resultado.Falla) return categorias
        val hogar = hogar() // sin permiso de hogar la app sigue funcionando
        val contactos = contactos()
        val u = (usuario as Resultado.Ok).datos
        return Resultado.Ok(
            Comunes(
                usuarioId = u.id, nombre = u.name, email = u.email, avatar = u.avatar,
                hogar = (hogar as? Resultado.Ok)?.datos,
                categorias = (categorias as Resultado.Ok).datos,
                contactos = (contactos as? Resultado.Ok)?.datos.orEmpty()
            ).also { comunes = it }
        )
    }

    fun olvidar() { comunes = null }

    suspend fun dashboard() = llamar { api.dashboard() }.datos()
    suspend fun hogar() = llamar { api.hogar() }.datos()
    suspend fun categorias() = llamar { api.categorias() }.datos()
    suspend fun contactos(filtro: String? = null) = llamar { api.contactos(filtro) }.datos()
    suspend fun catalogos() = llamar { api.catalogos() }

    suspend fun efectivo(ciclo: String?, tipo: String?, pagina: Int) = llamar { api.efectivo(ciclo, tipo, pagina) }
    suspend fun guardarEfectivo(id: Int?, body: MovimientoEfectivoBody) =
        llamar { if (id == null) api.crearEfectivo(body) else api.editarEfectivo(id, body) }.datos()
    suspend fun eliminarEfectivo(id: Int) = llamarSinCuerpo { api.eliminarEfectivo(id) }

    suspend fun compromisos(periodo: String, estado: String?, pagina: Int) = llamar { api.compromisos(periodo, estado, pagina) }
    suspend fun compromiso(id: Int) = llamar { api.compromiso(id) }.datos()
    suspend fun crearCompromiso(body: com.carlosramos.misgastos.data.remote.dto.CompromisoBody) = llamarSinCuerpo { api.crearCompromiso(body) }
    suspend fun editarSerie(id: Int, body: com.carlosramos.misgastos.data.remote.dto.CompromisoBody) = llamar { api.editarSerie(id, body) }.datos()
    suspend fun editarCompromiso(id: Int, body: com.carlosramos.misgastos.data.remote.dto.CompromisoBody) = llamar { api.editarCompromiso(id, body) }.datos()
    suspend fun origenesPago(id: Int) = llamar { api.origenesPago(id) }.datos()
    suspend fun pagarCompromiso(id: Int, body: PagoCompromisoBody) = llamar { api.pagarCompromiso(id, body) }.datos()
    suspend fun cancelarCompromiso(id: Int, serie: Boolean) =
        llamarSinCuerpo { api.cancelarCompromiso(id, if (serie) "serie" else "pago") }

    suspend fun cuentas() = llamar { api.cuentas() }.datos()
    suspend fun cuenta(id: Int, ciclo: String?, tipo: String?, pagina: Int) = llamar { api.cuenta(id, ciclo, tipo, pagina) }
    suspend fun guardarMovimientoCuenta(cuentaId: Int, id: Int?, body: com.carlosramos.misgastos.data.remote.dto.MovimientoCuentaBody) =
        llamar { if (id == null) api.crearMovimientoCuenta(cuentaId, body) else api.editarMovimientoCuenta(cuentaId, id, body) }.datos()
    suspend fun eliminarMovimientoCuenta(cuentaId: Int, id: Int) = llamarSinCuerpo { api.eliminarMovimientoCuenta(cuentaId, id) }

    suspend fun opcionesTransferencia() = llamar { api.opcionesTransferencia() }.datos()
    suspend fun transferir(body: com.carlosramos.misgastos.data.remote.dto.TransferenciaBody) = llamarSinCuerpo { api.crearTransferencia(body) }
    suspend fun eliminarTransferencia(id: Int) = llamarSinCuerpo { api.eliminarTransferencia(id) }

    suspend fun tarjetas() = llamar { api.tarjetas() }.datos()
    suspend fun tarjeta(id: Int) = llamar { api.tarjeta(id) }.datos()
    suspend fun guardarMovimientoTarjeta(tarjetaId: Int, id: Int?, body: com.carlosramos.misgastos.data.remote.dto.MovimientoTarjetaBody) =
        llamar { if (id == null) api.crearMovimientoTarjeta(tarjetaId, body) else api.editarMovimientoTarjeta(tarjetaId, id, body) }.datos()
    suspend fun eliminarMovimientoTarjeta(tarjetaId: Int, id: Int) = llamarSinCuerpo { api.eliminarMovimientoTarjeta(tarjetaId, id) }
    suspend fun guardarCargoFijo(tarjetaId: Int, id: Int?, body: com.carlosramos.misgastos.data.remote.dto.CargoFijoBody) =
        llamar { if (id == null) api.crearCargoFijo(tarjetaId, body) else api.editarCargoFijo(tarjetaId, id, body) }.datos()
    suspend fun eliminarCargoFijo(tarjetaId: Int, id: Int) = llamarSinCuerpo { api.eliminarCargoFijo(tarjetaId, id) }
    suspend fun guardarIntereses(tarjetaId: Int, estadoId: Int, monto: String) =
        llamar { api.interesesEstado(tarjetaId, estadoId, com.carlosramos.misgastos.data.remote.dto.InteresesBody(monto)) }.datos()

    suspend fun contacto(id: Int) = llamar { api.contacto(id) }.datos()
    suspend fun guardarContacto(id: Int?, body: com.carlosramos.misgastos.data.remote.dto.ContactoBody) =
        llamar { if (id == null) api.crearContacto(body) else api.editarContacto(id, body) }.datos().also { if (it is Resultado.Ok) olvidar() }
    suspend fun crearDeuda(contactoId: Int, body: com.carlosramos.misgastos.data.remote.dto.DeudaBody) = llamar { api.crearDeuda(contactoId, body) }.datos()
    suspend fun eliminarDeuda(id: Int) = llamarSinCuerpo { api.eliminarDeuda(id) }
    suspend fun crearPagoDeuda(contactoId: Int, body: com.carlosramos.misgastos.data.remote.dto.DeudaPagoBody) = llamar { api.crearPagoDeuda(contactoId, body) }.datos()
    suspend fun eliminarPagoDeuda(id: Int) = llamarSinCuerpo { api.eliminarPagoDeuda(id) }

    suspend fun prestamos() = llamar { api.prestamos() }.datos()
    suspend fun prestamo(id: Int) = llamar { api.prestamo(id) }.datos()
    suspend fun proyeccion(id: Int, extra: String?, abono: String?, modo: String?) = llamar { api.proyeccion(id, extra, abono, modo) }.datos()
    suspend fun pagarPrestamo(id: Int, body: com.carlosramos.misgastos.data.remote.dto.PrestamoPagoBody) = llamar { api.crearPagoPrestamo(id, body) }.datos()
    suspend fun eliminarPagoPrestamo(id: Int) = llamarSinCuerpo { api.eliminarPagoPrestamo(id) }

    suspend fun ingresos() = llamar { api.ingresos() }.datos()
    suspend fun ingreso(id: Int) = llamar { api.ingreso(id) }.datos()
    suspend fun propuesta(id: Int, bruto: String?) = llamar { api.propuesta(id, bruto) }.datos()
    suspend fun registrarRecibo(id: Int, body: com.carlosramos.misgastos.data.remote.dto.ReciboBody) = llamar { api.crearRecibo(id, body) }.datos()
    suspend fun eliminarRecibo(id: Int) = llamarSinCuerpo { api.eliminarRecibo(id) }

    /** Efectivo y cuentas activas con su saldo, para elegir de dónde sale o a dónde entra el dinero. */
    suspend fun cajas() = when (val r = opcionesTransferencia()) {
        is Resultado.Ok -> Resultado.Ok(r.datos.destinos)
        is Resultado.Falla -> r
    }

    suspend fun guardarCuenta(id: Int?, b: com.carlosramos.misgastos.data.remote.dto.CuentaBody) =
        llamar { if (id == null) api.crearCuenta(b) else api.editarCuenta(id, b) }.datos()
    suspend fun eliminarCuenta(id: Int) = llamarSinCuerpo { api.eliminarCuenta(id) }
    suspend fun guardarTarjeta(id: Int?, b: com.carlosramos.misgastos.data.remote.dto.TarjetaBody) =
        llamar { if (id == null) api.crearTarjeta(b) else api.editarTarjeta(id, b) }.datos()
    suspend fun eliminarTarjeta(id: Int) = llamarSinCuerpo { api.eliminarTarjeta(id) }
    suspend fun guardarPrestamo(id: Int?, b: com.carlosramos.misgastos.data.remote.dto.PrestamoBody) =
        llamar { if (id == null) api.crearPrestamo(b) else api.editarPrestamo(id, b) }.datos()
    suspend fun eliminarPrestamo(id: Int) = llamarSinCuerpo { api.eliminarPrestamo(id) }
    suspend fun guardarFuente(id: Int?, b: com.carlosramos.misgastos.data.remote.dto.FuenteBody) =
        llamar { if (id == null) api.crearFuente(b) else api.editarFuente(id, b) }.datos()
    suspend fun eliminarFuente(id: Int) = llamarSinCuerpo { api.eliminarFuente(id) }

    suspend fun actualizarPerfil(nombre: String, email: String) =
        llamar { api.actualizarPerfil(com.carlosramos.misgastos.data.remote.dto.PerfilBody(nombre, email)) }.also { if (it is Resultado.Ok) olvidar() }

    suspend fun subirAvatar(jpeg: ByteArray) = llamar {
        api.subirAvatar(texto("PATCH"), okhttp3.MultipartBody.Part.createFormData("avatar", "avatar.jpg", jpeg.toRequestBody("image/jpeg".toMediaType())))
    }.also { if (it is Resultado.Ok) olvidar() }

    /** Cambia la foto de un contacto conservando sus datos. */
    suspend fun fotoContacto(c: com.carlosramos.misgastos.data.remote.dto.Contacto, jpeg: ByteArray): Resultado<com.carlosramos.misgastos.data.remote.dto.Contacto> {
        val campos = buildMap {
            put("_method", texto("PUT"))
            put("nombre", texto(c.nombre ?: c.nombreCompleto))
            c.apellido?.let { put("apellido", texto(it)) }
            c.tipoContactoId?.let { put("tipo_contacto_id", texto(it.toString())) }
            c.telefono?.let { put("telefono", texto(it)) }
            c.email?.let { put("email", texto(it)) }
            c.notas?.let { put("notas", texto(it)) }
            put("activo", texto(if (c.activo) "1" else "0"))
        }
        return llamar { api.fotoContacto(c.id, campos, okhttp3.MultipartBody.Part.createFormData("foto", "foto.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))) }.datos()
            .also { if (it is Resultado.Ok) olvidar() }
    }

    suspend fun guardarCategoria(id: Int?, b: com.carlosramos.misgastos.data.remote.dto.CategoriaBody) =
        llamar { if (id == null) api.crearCategoria(b) else api.editarCategoria(id, b) }.datos().also { if (it is Resultado.Ok) olvidar() }
    suspend fun eliminarCategoria(id: Int) = llamarSinCuerpo { api.eliminarCategoria(id) }.also { if (it is Resultado.Ok) olvidar() }

    // ----- Reportes -----

    /** ver = null: los escenarios más recientes; lista vacía: solo "Si sigo igual". */
    suspend fun proyecciones(ver: List<Int>?) = llamar { api.proyecciones(ver?.joinToString(",")) }.datos()
    suspend fun reporteEjecucion(periodo: Map<String, String>) = llamar { api.reporteEjecucion(periodo) }
    suspend fun reporteGastos(periodo: Map<String, String>) = llamar { api.reporteGastos(periodo) }
    suspend fun reportePrestamos() = llamar { api.reportePrestamos() }
    suspend fun reporteEstadoCuenta(tarjeta: Int?, estado: Int?) = llamar { api.reporteEstadoCuenta(tarjeta, estado) }

    suspend fun eventos() = llamar { api.eventos() }.datos()
    suspend fun evento(id: Int) = llamar { api.evento(id) }.datos()
    suspend fun opcionesEvento() = llamar { api.opcionesEvento() }.datos()
    suspend fun guardarEvento(id: Int?, body: com.carlosramos.misgastos.data.remote.dto.EventoBody) =
        llamar { if (id == null) api.crearEvento(body) else api.editarEvento(id, body) }.datos()
    suspend fun eliminarEvento(id: Int) = llamarSinCuerpo { api.eliminarEvento(id) }
    /** Foto de la cuenta (JPEG ya reducido con Imagen.reducida). */
    suspend fun subirFotoEvento(id: Int, jpeg: ByteArray) =
        llamar { api.subirFotoEvento(id, okhttp3.MultipartBody.Part.createFormData("foto", "cuenta.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))) }.datos()
    suspend fun quitarFotoEvento(id: Int) = llamar { api.quitarFotoEvento(id) }.datos()
    suspend fun cobrarEvento(id: Int, participante: Int, body: com.carlosramos.misgastos.data.remote.dto.CobroEventoBody) = llamar { api.cobrarEvento(id, participante, body) }.datos()
    suspend fun marcarPagadoEvento(id: Int, participante: Int, body: com.carlosramos.misgastos.data.remote.dto.PagadoEventoBody) = llamar { api.marcarPagadoEvento(id, participante, body) }.datos()
    suspend fun desmarcarPagadoEvento(id: Int, participante: Int) = llamar { api.desmarcarPagadoEvento(id, participante) }.datos()

    /** Descarga la imagen del cuadro de un evento a [destino] (carpeta de caché) para compartirla. */
    suspend fun descargarImagenEvento(id: Int, destino: java.io.File): Resultado<java.io.File> = try {
        val respuesta = api.imagenEvento(id)
        val cuerpo = respuesta.body()
        if (!respuesta.isSuccessful || cuerpo == null) falla(respuesta) else {
            destino.mkdirs()
            val archivo = java.io.File(destino, "evento-$id.png")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { cuerpo.byteStream().use { e -> archivo.outputStream().use { e.copyTo(it) } } }
            Resultado.Ok(archivo)
        }
    } catch (e: Exception) {
        Resultado.Falla("No se pudo descargar la imagen. Revisa tu conexión.")
    }

    /** Descarga el reporte al [destino] (carpeta de caché) y devuelve el archivo. */
    suspend fun descargarReporte(reporte: String, parametros: Map<String, String>, destino: java.io.File): Resultado<java.io.File> = try {
        val respuesta = api.exportarReporte(reporte, parametros)
        val cuerpo = respuesta.body()
        if (!respuesta.isSuccessful || cuerpo == null) falla(respuesta) else {
            val nombre = respuesta.headers()["content-disposition"]?.substringAfter("filename=", "")?.trim('"', ' ')?.takeIf { it.isNotBlank() }
                ?: "reporte_$reporte.${parametros["formato"] ?: "pdf"}"
            destino.mkdirs()
            val archivo = java.io.File(destino, nombre)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { cuerpo.byteStream().use { e -> archivo.outputStream().use { e.copyTo(it) } } }
            Resultado.Ok(archivo)
        }
    } catch (e: Exception) {
        Resultado.Falla("No se pudo descargar el reporte. Revisa tu conexión.")
    }

    private fun texto(valor: String) = valor.toRequestBody("text/plain".toMediaType())

    private suspend fun <T> llamar(peticion: suspend () -> Response<T>): Resultado<T> = try {
        val respuesta = peticion()
        val cuerpo = respuesta.body()
        if (respuesta.isSuccessful && cuerpo != null) Resultado.Ok(cuerpo) else falla(respuesta)
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }

    private suspend fun llamarSinCuerpo(peticion: suspend () -> Response<Unit>): Resultado<Unit> = try {
        val respuesta = peticion()
        if (respuesta.isSuccessful) Resultado.Ok(Unit) else falla(respuesta)
    } catch (e: Exception) {
        Resultado.Falla("No se pudo conectar con el servidor. Revisa tu conexión.")
    }

    private suspend fun falla(respuesta: Response<*>): Resultado.Falla {
        val error = runCatching {
            Gson().fromJson(respuesta.errorBody()?.string(), LaravelErrorResponse::class.java)
        }.getOrNull()
        return when (respuesta.code()) {
            401 -> {
                olvidar()
                tokenProvider.clearToken()
                sesion.expiro()
                Resultado.Falla("Tu sesión expiró. Vuelve a iniciar sesión.")
            }
            403 -> Resultado.Falla("No tienes permiso para hacer esto.")
            404 -> Resultado.Falla(error?.message?.takeIf { it.isNotBlank() && it != "Not Found" } ?: "No se encontró el registro.")
            422 -> Resultado.Falla(
                error?.errors?.values?.firstOrNull()?.firstOrNull() ?: error?.message ?: "Revisa los datos.",
                error?.errors?.mapValues { it.value.firstOrNull().orEmpty() }.orEmpty()
            )
            else -> Resultado.Falla(error?.message?.takeIf { it.isNotBlank() } ?: "Error del servidor (${respuesta.code()}).")
        }
    }
}

/** Saca el contenido de "data". */
private fun <T> Resultado<com.carlosramos.misgastos.data.remote.dto.Envoltura<T>>.datos(): Resultado<T> = when (this) {
    is Resultado.Ok -> Resultado.Ok(datos.data)
    is Resultado.Falla -> this
}
