package com.carlosramos.misgastos.data.remote

import com.carlosramos.misgastos.data.remote.dto.Catalogos
import com.carlosramos.misgastos.data.remote.dto.CuentaBody
import com.carlosramos.misgastos.data.remote.dto.FuenteBody
import com.carlosramos.misgastos.data.remote.dto.PrestamoBody
import com.carlosramos.misgastos.data.remote.dto.TarjetaBody
import com.carlosramos.misgastos.data.remote.dto.Categoria
import com.carlosramos.misgastos.data.remote.dto.ContactoBody
import com.carlosramos.misgastos.data.remote.dto.DeudaBody
import com.carlosramos.misgastos.data.remote.dto.DeudaPagoBody
import com.carlosramos.misgastos.data.remote.dto.FuenteIngreso
import com.carlosramos.misgastos.data.remote.dto.Prestamo
import com.carlosramos.misgastos.data.remote.dto.PrestamoPagoBody
import com.carlosramos.misgastos.data.remote.dto.Propuesta
import com.carlosramos.misgastos.data.remote.dto.Proyeccion
import com.carlosramos.misgastos.data.remote.dto.ReciboBody
import com.carlosramos.misgastos.data.remote.dto.Cuenta
import com.carlosramos.misgastos.data.remote.dto.MovimientoCuenta
import com.carlosramos.misgastos.data.remote.dto.MovimientoCuentaBody
import com.carlosramos.misgastos.data.remote.dto.MovimientoTarjeta
import com.carlosramos.misgastos.data.remote.dto.MovimientoTarjetaBody
import com.carlosramos.misgastos.data.remote.dto.OpcionesTransferencia
import com.carlosramos.misgastos.data.remote.dto.PaginaCuenta
import com.carlosramos.misgastos.data.remote.dto.Tarjeta
import com.carlosramos.misgastos.data.remote.dto.TarjetaDetalle
import com.carlosramos.misgastos.data.remote.dto.TransferenciaBody
import com.carlosramos.misgastos.data.remote.dto.Compromiso
import com.carlosramos.misgastos.data.remote.dto.CompromisoBody
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
    suspend fun contactos(@Query("filtro") filtro: String? = null): Response<Envoltura<List<Contacto>>>

    /** Sin envoltura "data": responde los catálogos directamente. */
    @GET("catalogos")
    suspend fun catalogos(): Response<Catalogos>

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

    /** Responde el primer pago generado; en recurrentes además recurrente_id y pagos_generados. */
    @POST("compromisos")
    suspend fun crearCompromiso(@Body body: CompromisoBody): Response<Unit>

    @PUT("compromisos/{id}")
    suspend fun editarCompromiso(@Path("id") id: Int, @Body body: CompromisoBody): Response<Envoltura<Compromiso>>

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

    // ----- Cuentas -----

    @GET("cuentas")
    suspend fun cuentas(): Response<Envoltura<List<Cuenta>>>

    @GET("cuentas/{id}")
    suspend fun cuenta(
        @Path("id") id: Int,
        @Query("ciclo") ciclo: String?,
        @Query("tipo") tipo: String?,
        @Query("page") pagina: Int,
        @Query("per_page") porPagina: Int = 30
    ): Response<PaginaCuenta>

    @POST("cuentas/{id}/movimientos")
    suspend fun crearMovimientoCuenta(@Path("id") cuentaId: Int, @Body body: MovimientoCuentaBody): Response<Envoltura<MovimientoCuenta>>

    @PUT("cuentas/{id}/movimientos/{mov}")
    suspend fun editarMovimientoCuenta(@Path("id") cuentaId: Int, @Path("mov") id: Int, @Body body: MovimientoCuentaBody): Response<Envoltura<MovimientoCuenta>>

    @DELETE("cuentas/{id}/movimientos/{mov}")
    suspend fun eliminarMovimientoCuenta(@Path("id") cuentaId: Int, @Path("mov") id: Int): Response<Unit>

    // ----- Transferencias -----

    @GET("transferencias/opciones")
    suspend fun opcionesTransferencia(): Response<Envoltura<OpcionesTransferencia>>

    @POST("transferencias")
    suspend fun crearTransferencia(@Body body: TransferenciaBody): Response<Unit>

    @DELETE("transferencias/{id}")
    suspend fun eliminarTransferencia(@Path("id") id: Int): Response<Unit>

    // ----- Tarjetas -----

    @GET("tarjetas")
    suspend fun tarjetas(): Response<Envoltura<List<Tarjeta>>>

    @GET("tarjetas/{id}")
    suspend fun tarjeta(@Path("id") id: Int): Response<Envoltura<TarjetaDetalle>>

    @POST("tarjetas/{id}/movimientos")
    suspend fun crearMovimientoTarjeta(@Path("id") tarjetaId: Int, @Body body: MovimientoTarjetaBody): Response<Envoltura<MovimientoTarjeta>>

    @PUT("tarjetas/{id}/movimientos/{mov}")
    suspend fun editarMovimientoTarjeta(@Path("id") tarjetaId: Int, @Path("mov") id: Int, @Body body: MovimientoTarjetaBody): Response<Envoltura<MovimientoTarjeta>>

    @DELETE("tarjetas/{id}/movimientos/{mov}")
    suspend fun eliminarMovimientoTarjeta(@Path("id") tarjetaId: Int, @Path("mov") id: Int): Response<Unit>

    // ----- Contactos y lo que nos deben -----

    @GET("contactos/{id}")
    suspend fun contacto(@Path("id") id: Int): Response<Envoltura<Contacto>>

    @POST("contactos")
    suspend fun crearContacto(@Body body: ContactoBody): Response<Envoltura<Contacto>>

    @PUT("contactos/{id}")
    suspend fun editarContacto(@Path("id") id: Int, @Body body: ContactoBody): Response<Envoltura<Contacto>>

    @POST("contactos/{id}/deudas")
    suspend fun crearDeuda(@Path("id") contactoId: Int, @Body body: DeudaBody): Response<Envoltura<Contacto>>

    @DELETE("deudas/{id}")
    suspend fun eliminarDeuda(@Path("id") id: Int): Response<Unit>

    @POST("contactos/{id}/pagos")
    suspend fun crearPagoDeuda(@Path("id") contactoId: Int, @Body body: DeudaPagoBody): Response<Envoltura<Contacto>>

    @DELETE("deuda-pagos/{id}")
    suspend fun eliminarPagoDeuda(@Path("id") id: Int): Response<Unit>

    // ----- Préstamos -----

    @GET("prestamos")
    suspend fun prestamos(): Response<Envoltura<List<Prestamo>>>

    @GET("prestamos/{id}")
    suspend fun prestamo(@Path("id") id: Int): Response<Envoltura<Prestamo>>

    /** modo del abono: plazo (se acorta) | cuota (baja la cuota) */
    @GET("prestamos/{id}/proyeccion")
    suspend fun proyeccion(
        @Path("id") id: Int,
        @Query("extra_mensual") extraMensual: String?,
        @Query("abono") abono: String?,
        @Query("modo") modo: String?
    ): Response<Envoltura<Proyeccion>>

    @POST("prestamos/{id}/pagos")
    suspend fun crearPagoPrestamo(@Path("id") id: Int, @Body body: PrestamoPagoBody): Response<Envoltura<Prestamo>>

    @DELETE("prestamo-pagos/{id}")
    suspend fun eliminarPagoPrestamo(@Path("id") id: Int): Response<Unit>

    // ----- Ingresos -----

    @GET("ingresos")
    suspend fun ingresos(): Response<Envoltura<List<FuenteIngreso>>>

    @GET("ingresos/{id}")
    suspend fun ingreso(@Path("id") id: Int): Response<Envoltura<FuenteIngreso>>

    @GET("ingresos/{id}/propuesta")
    suspend fun propuesta(@Path("id") id: Int, @Query("bruto") bruto: String?): Response<Envoltura<Propuesta>>

    @POST("ingresos/{id}/recibos")
    suspend fun crearRecibo(@Path("id") id: Int, @Body body: ReciboBody): Response<Envoltura<FuenteIngreso>>

    @DELETE("recibos/{id}")
    suspend fun eliminarRecibo(@Path("id") id: Int): Response<Unit>

    // ----- Alta, edición y baja -----

    @POST("cuentas") suspend fun crearCuenta(@Body body: CuentaBody): Response<Envoltura<Cuenta>>
    @PUT("cuentas/{id}") suspend fun editarCuenta(@Path("id") id: Int, @Body body: CuentaBody): Response<Envoltura<Cuenta>>
    @DELETE("cuentas/{id}") suspend fun eliminarCuenta(@Path("id") id: Int): Response<Unit>

    @POST("tarjetas") suspend fun crearTarjeta(@Body body: TarjetaBody): Response<Envoltura<Tarjeta>>
    @PUT("tarjetas/{id}") suspend fun editarTarjeta(@Path("id") id: Int, @Body body: TarjetaBody): Response<Envoltura<Tarjeta>>
    @DELETE("tarjetas/{id}") suspend fun eliminarTarjeta(@Path("id") id: Int): Response<Unit>

    @POST("prestamos") suspend fun crearPrestamo(@Body body: PrestamoBody): Response<Envoltura<Prestamo>>
    @PUT("prestamos/{id}") suspend fun editarPrestamo(@Path("id") id: Int, @Body body: PrestamoBody): Response<Envoltura<Prestamo>>
    @DELETE("prestamos/{id}") suspend fun eliminarPrestamo(@Path("id") id: Int): Response<Unit>

    @POST("ingresos") suspend fun crearFuente(@Body body: FuenteBody): Response<Envoltura<FuenteIngreso>>
    @PUT("ingresos/{id}") suspend fun editarFuente(@Path("id") id: Int, @Body body: FuenteBody): Response<Envoltura<FuenteIngreso>>
    @DELETE("ingresos/{id}") suspend fun eliminarFuente(@Path("id") id: Int): Response<Unit>
}