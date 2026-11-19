package com.carlosramos.misgastos.data.remote

import com.carlosramos.misgastos.data.remote.dto.Compras
import com.carlosramos.misgastos.data.remote.dto.DatosPago
import com.carlosramos.misgastos.data.remote.dto.DespensaBody
import com.carlosramos.misgastos.data.remote.dto.Envoltura
import com.carlosramos.misgastos.data.remote.dto.EstadoConsulta
import com.carlosramos.misgastos.data.remote.dto.EstadoRespuesta
import com.carlosramos.misgastos.data.remote.dto.InicioConsulta
import com.carlosramos.misgastos.data.remote.dto.ItemBody
import com.carlosramos.misgastos.data.remote.dto.ItemCambio
import com.carlosramos.misgastos.data.remote.dto.ListaCompra
import com.carlosramos.misgastos.data.remote.dto.ListaProductos
import com.carlosramos.misgastos.data.remote.dto.PagoBody
import com.carlosramos.misgastos.data.remote.dto.PrecioBody
import com.carlosramos.misgastos.data.remote.dto.Producto
import com.carlosramos.misgastos.data.remote.dto.ProductoBody
import com.carlosramos.misgastos.data.remote.dto.ResultadoEnLinea
import com.carlosramos.misgastos.data.remote.dto.ResultadosEnLinea
import com.carlosramos.misgastos.data.remote.dto.Ruta
import com.carlosramos.misgastos.data.remote.dto.Tienda
import com.carlosramos.misgastos.data.remote.dto.TiendaActualBody
import com.carlosramos.misgastos.data.remote.dto.TiendaBody
import com.carlosramos.misgastos.data.remote.dto.TiendasComunes
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/** Despensa, tiendas y compras (mismas reglas que la web). */
interface DespensaApi {

    // ----- Productos -----
    @GET("productos")
    suspend fun productos(@QueryMap filtros: Map<String, String>): Response<ListaProductos>

    @GET("productos/codigo/{codigo}")
    suspend fun productoPorCodigo(@Path("codigo") codigo: String): Response<Envoltura<Producto>>

    @GET("productos/{id}")
    suspend fun producto(@Path("id") id: Int): Response<Envoltura<Producto>>

    @POST("productos")
    suspend fun crearProducto(@Body body: ProductoBody): Response<Envoltura<Producto>>

    @PUT("productos/{id}")
    suspend fun editarProducto(@Path("id") id: Int, @Body body: ProductoBody): Response<Envoltura<Producto>>

    /** Foto: POST con _method=PUT y todos los datos (el formulario los exige). */
    @Multipart
    @POST("productos/{id}")
    suspend fun fotoProducto(@Path("id") id: Int, @PartMap campos: Map<String, @JvmSuppressWildcards RequestBody>, @Part foto: MultipartBody.Part): Response<Envoltura<Producto>>

    @DELETE("productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Int): Response<Unit>

    @PATCH("productos/{id}/despensa")
    suspend fun despensa(@Path("id") id: Int, @Body body: DespensaBody): Response<EstadoRespuesta>

    @POST("productos/{id}/precios")
    suspend fun anotarPrecio(@Path("id") id: Int, @Body body: PrecioBody): Response<Envoltura<Producto>>

    @DELETE("productos/{id}/precios/{precio}")
    suspend fun eliminarPrecio(@Path("id") id: Int, @Path("precio") precio: Int): Response<Unit>

    @GET("productos/{id}/en-linea/{fuente}")
    suspend fun buscarEnLinea(@Path("id") id: Int, @Path("fuente") fuente: String, @Query("q") q: String?): Response<ResultadosEnLinea>

    @POST("productos/{id}/en-linea/{fuente}")
    suspend fun enlazar(@Path("id") id: Int, @Path("fuente") fuente: String, @Body body: ResultadoEnLinea): Response<Envoltura<Producto>>

    @DELETE("productos/{id}/en-linea/{fuente}")
    suspend fun desenlazar(@Path("id") id: Int, @Path("fuente") fuente: String): Response<Unit>

    // ----- Tiendas -----
    @GET("tiendas")
    suspend fun tiendas(): Response<Envoltura<List<Tienda>>>

    @POST("tiendas")
    suspend fun crearTienda(@Body body: TiendaBody): Response<Envoltura<Tienda>>

    @PUT("tiendas/{id}")
    suspend fun editarTienda(@Path("id") id: Int, @Body body: TiendaBody): Response<Envoltura<Tienda>>

    @DELETE("tiendas/{id}")
    suspend fun eliminarTienda(@Path("id") id: Int): Response<Unit>

    @POST("tiendas/comunes")
    suspend fun tiendasComunes(): Response<TiendasComunes>

    // ----- Compras -----
    @GET("compras")
    suspend fun compras(): Response<Compras>

    @POST("compras")
    suspend fun nuevaLista(): Response<Envoltura<ListaCompra>>

    @GET("compras/{id}")
    suspend fun lista(@Path("id") id: Int): Response<Envoltura<ListaCompra>>

    @DELETE("compras/{id}")
    suspend fun eliminarLista(@Path("id") id: Int): Response<Unit>

    @POST("compras/{id}/faltantes")
    suspend fun agregarFaltantes(@Path("id") id: Int): Response<Envoltura<ListaCompra>>

    @POST("compras/{id}/terminar")
    suspend fun terminar(@Path("id") id: Int): Response<Envoltura<ListaCompra>>

    @PATCH("compras/{id}/tienda")
    suspend fun elegirTienda(@Path("id") id: Int, @Body body: TiendaActualBody): Response<Envoltura<ListaCompra>>

    @POST("compras/{id}/items")
    suspend fun agregarItem(@Path("id") id: Int, @Body body: ItemBody): Response<Envoltura<ListaCompra>>

    @PATCH("compras/{id}/items/{item}")
    suspend fun cambiarItem(@Path("id") id: Int, @Path("item") item: Int, @Body body: ItemCambio): Response<Envoltura<ListaCompra>>

    @DELETE("compras/{id}/items/{item}")
    suspend fun quitarItem(@Path("id") id: Int, @Path("item") item: Int): Response<Envoltura<ListaCompra>>

    @GET("compras/{id}/pagar")
    suspend fun datosPago(@Path("id") id: Int): Response<DatosPago>

    @POST("compras/{id}/pagar")
    suspend fun pagar(@Path("id") id: Int, @Body body: PagoBody): Response<Envoltura<ListaCompra>>

    @DELETE("compras/{id}/pagos/{pago}")
    suspend fun deshacerPago(@Path("id") id: Int, @Path("pago") pago: Int): Response<Envoltura<ListaCompra>>

    @POST("compras/{id}/consultas")
    suspend fun iniciarConsulta(@Path("id") id: Int): Response<InicioConsulta>

    @POST("compras/{id}/consultas/{consulta}/siguiente")
    suspend fun siguienteConsulta(@Path("id") id: Int, @Path("consulta") consulta: Int): Response<EstadoConsulta>

    @GET("compras/{id}/ruta")
    suspend fun ruta(@Path("id") id: Int, @Query("costo") costo: String?): Response<Ruta>
}
