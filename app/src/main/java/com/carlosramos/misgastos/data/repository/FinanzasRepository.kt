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
                usuarioId = u.id, nombre = u.name, email = u.email,
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
    suspend fun contactos() = llamar { api.contactos() }.datos()

    suspend fun efectivo(ciclo: String?, tipo: String?, pagina: Int) = llamar { api.efectivo(ciclo, tipo, pagina) }
    suspend fun guardarEfectivo(id: Int?, body: MovimientoEfectivoBody) =
        llamar { if (id == null) api.crearEfectivo(body) else api.editarEfectivo(id, body) }.datos()
    suspend fun eliminarEfectivo(id: Int) = llamarSinCuerpo { api.eliminarEfectivo(id) }

    suspend fun compromisos(periodo: String, estado: String?, pagina: Int) = llamar { api.compromisos(periodo, estado, pagina) }
    suspend fun compromiso(id: Int) = llamar { api.compromiso(id) }.datos()
    suspend fun origenesPago(id: Int) = llamar { api.origenesPago(id) }.datos()
    suspend fun pagarCompromiso(id: Int, body: PagoCompromisoBody) = llamar { api.pagarCompromiso(id, body) }.datos()
    suspend fun cancelarCompromiso(id: Int, serie: Boolean) =
        llamarSinCuerpo { api.cancelarCompromiso(id, if (serie) "serie" else "pago") }

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
