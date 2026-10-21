package com.carlosramos.misgastos.data.remote

import com.carlosramos.misgastos.data.remote.dto.Categoria
import com.carlosramos.misgastos.data.remote.dto.Compromiso
import com.carlosramos.misgastos.data.remote.dto.Contacto
import com.carlosramos.misgastos.data.remote.dto.Dashboard
import com.carlosramos.misgastos.data.remote.dto.Envoltura
import com.carlosramos.misgastos.data.remote.dto.Hogar
import com.carlosramos.misgastos.data.remote.dto.MovimientoEfectivo
import com.carlosramos.misgastos.data.remote.dto.MovimientoEfectivoBody
import com.carlosramos.misgastos.data.remote.dto.PagoCompromisoBody
import com.carlosramos.misgastos.data.remote.dto.PaginaCompromisos
import com.carlosramos.misgastos.data.remote.dto.PaginaEfectivo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de finanzas (requieren el token de Sanctum, que agrega AuthInterceptor). */
interface FinanzasApi {

    @GET("dashboard")
    suspend fun dashboard(): Response<Envoltura<Dashboard>>

    @GET("hogar")
    suspend fun hogar(): Response<Envoltura<Hogar>>

    @GET("categorias")
    suspend fun categorias(): Response<Envoltura<List<Categoria>>>

    @GET("contactos")
    suspend fun contactos(): Response<Envoltura<List<Contacto>>>

    // ----- Efectivo -----

    /** ciclo: cualquier fecha dentro del ciclo a consultar (null = el actual). */
    @GET("efectivo")
    suspend fun efectivo(
        @Query("ciclo") ciclo: String?,
        @Query("tipo") tipo: String?,
        @Query("page") pagina: Int,
        @Query("per_page") porPagina: Int = 30
    ): Response<PaginaEfectivo>

    @POST("efectivo")
    suspend fun crearEfectivo(@Body body: MovimientoEfectivoBody): Response<Envoltura<MovimientoEfectivo>>

    @PUT("efectivo/{id}")
    suspend fun editarEfectivo(@Path("id") id: Int, @Body body: MovimientoEfectivoBody): Response<Envoltura<MovimientoEfectivo>>

    @DELETE("efectivo/{id}")
    suspend fun eliminarEfectivo(@Path("id") id: Int): Response<Unit>

    // ----- Compromisos -----

    /** periodo: actual | proximos | todos · estado: pendiente | parcial | vencido | antiguo | pagado | cancelado */
    @GET("compromisos")
    suspend fun compromisos(
        @Query("periodo") periodo: String,
        @Query("estado") estado: String?,
        @Query("page") pagina: Int,
        @Query("per_page") porPagina: Int = 30
    ): Response<PaginaCompromisos>

    @GET("compromisos/{id}")
    suspend fun compromiso(@Path("id") id: Int): Response<Envoltura<Compromiso>>

    /** Mapa ordenado origen → etiqueta ("cuenta:42" → "Agrícola Carlos ($ 19.36)"). */
    @GET("compromisos/{id}/origenes-pago")
    suspend fun origenesPago(@Path("id") id: Int): Response<Envoltura<Map<String, String>>>

    @POST("compromisos/{id}/pagar")
    suspend fun pagarCompromiso(@Path("id") id: Int, @Body body: PagoCompromisoBody): Response<Envoltura<Compromiso>>

    /** alcance: pago (solo este) | serie (este y los siguientes de la regla) */
    @DELETE("compromisos/{id}")
    suspend fun cancelarCompromiso(@Path("id") id: Int, @Query("alcance") alcance: String): Response<Unit>
}
