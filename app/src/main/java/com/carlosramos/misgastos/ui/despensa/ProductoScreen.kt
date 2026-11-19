package com.carlosramos.misgastos.ui.despensa

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.PrecioBody
import com.carlosramos.misgastos.data.remote.dto.Producto
import com.carlosramos.misgastos.data.remote.dto.ProductoBody
import com.carlosramos.misgastos.data.remote.dto.ResultadoEnLinea
import com.carlosramos.misgastos.data.remote.dto.Tienda
import com.carlosramos.misgastos.data.remote.dto.ValoresDespensa
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Escaner
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.HojaFormulario
import com.carlosramos.misgastos.ui.common.Imagen
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lo que se escribe en el formulario (texto tal cual; se convierte al guardar). */
data class FormProducto(
    val nombre: String = "", val marca: String = "", val cantidad: String = "", val unidad: String? = null, val codigo: String = "",
    val categoria: Int? = null, val importancia: String = "basico", val duracion: String = "duradero", val otraMarca: Boolean = false,
    val ideal: String = "", val avisar: String = "", val enCasa: String = "", val venceEl: String = "", val notas: String = "", val activo: Boolean = true
) {
    fun body() = ProductoBody(
        nombre.trim(), marca.trim().ifBlank { null }, cantidad.trim().ifBlank { null }, unidad.takeIf { cantidad.isNotBlank() }, codigo.trim().ifBlank { null },
        categoria, importancia, duracion, otraMarca, ideal.toIntOrNull(), avisar.toIntOrNull(), enCasa.toIntOrNull(),
        venceEl.ifBlank { null }.takeIf { duracion == "perecedero" }, notas.trim().ifBlank { null }, activo
    )

    companion object {
        fun de(p: Producto) = FormProducto(p.nombre, p.marca.orEmpty(), p.cantidad.orEmpty(), p.unidad, p.codigoBarras.orEmpty(), p.categoriaDespensaId,
            p.importancia, p.duracion, p.aceptaOtraMarca, p.cantidadIdeal?.toString().orEmpty(), p.avisarCuando?.toString().orEmpty(),
            p.estado?.enCasa?.toString().orEmpty(), p.estado?.venceEl.orEmpty(), p.notas.orEmpty(), p.activo)
    }
}

data class ProductoEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val producto: Producto? = null,
    val form: FormProducto = FormProducto(),
    val categorias: List<Pair<Int, String>> = emptyList(),
    val valores: ValoresDespensa? = null,
    val tiendas: List<Tienda> = emptyList(),
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val fotoNueva: ByteArray? = null,
    val listo: Boolean = false,
    // búsqueda en línea
    val buscando: Boolean = false,
    val resultados: List<ResultadoEnLinea>? = null,
    val errorBusqueda: String? = null
)

@HiltViewModel
class ProductoViewModel @Inject constructor(private val repo: FinanzasRepository, guardado: SavedStateHandle) : ViewModel() {
    private val idInicial = guardado.get<Int>("id")?.takeIf { it > 0 }
    private val codigoInicial = guardado.get<String>("codigo")?.takeIf { it.isNotBlank() }
    private val _estado = MutableStateFlow(ProductoEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        val catalogos = repo.catalogos()
        val tiendas = (repo.tiendas() as? Resultado.Ok)?.datos.orEmpty()
        if (catalogos is Resultado.Falla) { _estado.value = _estado.value.copy(cargando = false, error = catalogos.mensaje); return@launch }
        val c = (catalogos as Resultado.Ok).datos
        var e = _estado.value.copy(categorias = c.categoriasDespensa.orEmpty().map { it.id to it.nombre }, valores = c.despensa, tiendas = tiendas)
        val id = e.producto?.id ?: idInicial
        e = if (id != null) when (val r = repo.producto(id)) {
            is Resultado.Ok -> e.copy(cargando = false, error = null, producto = r.datos, form = if (e.producto == null) FormProducto.de(r.datos) else e.form)
            is Resultado.Falla -> e.copy(cargando = false, error = r.mensaje)
        } else e.copy(cargando = false, form = if (e.form.nombre.isBlank()) FormProducto(codigo = codigoInicial.orEmpty(), categoria = e.categorias.firstOrNull()?.first) else e.form)
        _estado.value = e
    }

    fun cambiar(f: FormProducto) { _estado.value = _estado.value.copy(form = f) }
    fun foto(jpeg: ByteArray?) { if (jpeg == null) avisar("No se pudo leer la foto.") else _estado.value = _estado.value.copy(fotoNueva = jpeg) }
    fun avisar(t: String) { _estado.value = _estado.value.copy(mensaje = t) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }

    fun guardar() = viewModelScope.launch {
        val e = _estado.value
        _estado.value = e.copy(guardando = true, errores = emptyMap())
        val body = e.form.body()
        var r = repo.guardarProducto(e.producto?.id, body)
        if (r is Resultado.Ok && e.fotoNueva != null) r = repo.fotoProducto(r.datos.id, body, e.fotoNueva)
        _estado.value = when (r) {
            is Resultado.Ok -> _estado.value.copy(guardando = false, producto = r.datos, form = FormProducto.de(r.datos), fotoNueva = null,
                mensaje = if (e.producto == null) "Producto agregado." else "Producto actualizado.")
            is Resultado.Falla -> _estado.value.copy(guardando = false, errores = r.campos, mensaje = r.mensaje)
        }
    }

    fun eliminar() = viewModelScope.launch {
        val id = _estado.value.producto?.id ?: return@launch
        when (val r = repo.eliminarProducto(id)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(listo = true)
            is Resultado.Falla -> avisar(r.mensaje)
        }
    }

    fun anotarPrecio(tienda: Int, precio: String, unidades: Int?, fecha: String, alTerminar: () -> Unit) = viewModelScope.launch {
        val id = _estado.value.producto?.id ?: return@launch
        when (val r = repo.anotarPrecio(id, PrecioBody(tienda, precio, unidades, fecha))) {
            is Resultado.Ok -> { _estado.value = _estado.value.copy(producto = r.datos, mensaje = "Precio anotado."); alTerminar() }
            is Resultado.Falla -> avisar(r.mensaje)
        }
    }

    fun buscar(fuente: String, texto: String?) = viewModelScope.launch {
        val id = _estado.value.producto?.id ?: return@launch
        _estado.value = _estado.value.copy(buscando = true, errorBusqueda = null)
        _estado.value = when (val r = repo.buscarEnLinea(id, fuente, texto)) {
            is Resultado.Ok -> _estado.value.copy(buscando = false, resultados = r.datos.resultados)
            is Resultado.Falla -> _estado.value.copy(buscando = false, resultados = null, errorBusqueda = r.mensaje)
        }
    }

    fun enlazar(fuente: String, r: ResultadoEnLinea, unidades: Int, alTerminar: () -> Unit) = viewModelScope.launch {
        val id = _estado.value.producto?.id ?: return@launch
        when (val x = repo.enlazar(id, fuente, r.copy(unidades = unidades))) {
            is Resultado.Ok -> { _estado.value = _estado.value.copy(producto = x.datos, resultados = null, mensaje = "Quedó ligado."); alTerminar() }
            is Resultado.Falla -> avisar(x.mensaje)
        }
    }

    fun desenlazar(fuente: String) = viewModelScope.launch {
        val id = _estado.value.producto?.id ?: return@launch
        when (val r = repo.desenlazar(id, fuente)) {
            is Resultado.Ok -> cargar()
            is Resultado.Falla -> avisar(r.mensaje)
        }
    }

    fun limpiarBusqueda() { _estado.value = _estado.value.copy(resultados = null, errorBusqueda = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoScreen(onVolver: () -> Unit, viewModel: ProductoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var anotando by remember { mutableStateOf(false) }
    var buscandoEn by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    LaunchedEffect(estado.listo) { if (estado.listo) onVolver() }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { viewModel.foto(Imagen.paraSubir(contexto, it, 600)) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(estado.producto?.nombre ?: "Nuevo producto") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (estado.producto != null) IconButton(onClick = { confirmarEliminar = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            estado.error != null -> ErrorConReintento(estado.error!!, { viewModel.cargar() }, Modifier.padding(relleno))
            else -> Column(Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Formulario(estado, viewModel, onFoto = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                estado.producto?.let { p ->
                    Precios(p, onAnotar = { anotando = true })
                    EnLinea(p, estado.tiendas, estado.valores, onBuscar = { f -> viewModel.limpiarBusqueda(); buscandoEn = f; viewModel.buscar(f, null) }, onQuitar = { viewModel.desenlazar(it) },
                        onAbrir = { url -> contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) })
                }
            }
        }
    }

    if (confirmarEliminar) Confirmar("¿Eliminar el producto?", "Se borra con su foto y sus precios.", "Eliminar", { confirmarEliminar = false; viewModel.eliminar() }, { confirmarEliminar = false })
    if (anotando) HojaPrecio(estado.tiendas.filter { it.activo }, { anotando = false }) { t, precio, u, fecha -> viewModel.anotarPrecio(t, precio, u, fecha) { anotando = false } }
    buscandoEn?.let { fuente ->
        HojaBuscarEnLinea(fuente, estado, estado.valores, onBuscar = { viewModel.buscar(fuente, it) }, onCerrar = { buscandoEn = null; viewModel.limpiarBusqueda() }) { r, u ->
            viewModel.enlazar(fuente, r, u) { buscandoEn = null }
        }
    }
}

@Composable
private fun Formulario(estado: ProductoEstado, vm: ProductoViewModel, onFoto: () -> Unit) {
    val f = estado.form
    val err = estado.errores
    val contexto = LocalContext.current
    val v = estado.valores
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable(onClick = onFoto)) {
            if (estado.fotoNueva != null) {
                val bitmap = remember(estado.fotoNueva) { android.graphics.BitmapFactory.decodeByteArray(estado.fotoNueva, 0, estado.fotoNueva.size).asImageBitmapSeguro() }
                if (bitmap != null) androidx.compose.foundation.Image(bitmap, contentDescription = "Foto nueva", modifier = Modifier.width(88.dp).padding(0.dp))
            } else Avatar(estado.producto?.fotoUrl, f.nombre.ifBlank { "?" }, tamano = 88.dp, cuadrado = true)
        }
        Spacer(Modifier.width(12.dp))
        Text(if (estado.fotoNueva != null) "Foto lista: se sube al guardar." else "Toca para elegir una foto (se recorta cuadrada).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    OutlinedTextField(f.nombre, { vm.cambiar(f.copy(nombre = it)) }, label = { Text("Producto (ej. Atún)") }, isError = err["nombre"] != null,
        supportingText = err["nombre"]?.let { { Text(it) } }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(f.marca, { vm.cambiar(f.copy(marca = it)) }, label = { Text("Marca") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(f.cantidad, { vm.cambiar(f.copy(cantidad = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' })) }, label = { Text("Presentación") },
            isError = err["cantidad"] != null, supportingText = err["cantidad"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        Desplegable("Unidad", v?.unidades?.toList().orEmpty(), f.unidad, { vm.cambiar(f.copy(unidad = it)) }, Modifier.weight(1f), err["unidad"])
    }
    OutlinedTextField(f.codigo, { vm.cambiar(f.copy(codigo = it.filter { c -> c.isDigit() })) }, label = { Text("Código de barras") },
        isError = err["codigo_barras"] != null, supportingText = err["codigo_barras"]?.let { { Text(it) } }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = { Escaner.escanear(contexto, { vm.cambiar(f.copy(codigo = it)) }, { vm.avisar(it) }) }) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear") } })
    Desplegable("Categoría", estado.categorias, f.categoria, { vm.cambiar(f.copy(categoria = it)) }, error = err["categoria_despensa_id"])
    Desplegable("Importancia", v?.importancias?.toList().orEmpty(), f.importancia, { vm.cambiar(f.copy(importancia = it)) })
    Desplegable("Duración", v?.duraciones?.toList().orEmpty(), f.duracion, { vm.cambiar(f.copy(duracion = it)) })
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Otra marca también sirve", Modifier.weight(1f))
        Switch(f.otraMarca, { vm.cambiar(f.copy(otraMarca = it)) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Numero("Cantidad ideal", f.ideal, err["cantidad_ideal"], Modifier.weight(1f)) { vm.cambiar(f.copy(ideal = it)) }
        Numero("Avisar cuando queden", f.avisar, err["avisar_cuando"], Modifier.weight(1f)) { vm.cambiar(f.copy(avisar = it)) }
    }
    Numero("Cuántos hay en casa (vacío = no llevar la cuenta)", f.enCasa, err["en_casa"], Modifier.fillMaxWidth()) { vm.cambiar(f.copy(enCasa = it)) }
    if (f.duracion == "perecedero") {
        CampoFecha("Vence el", f.venceEl, { vm.cambiar(f.copy(venceEl = it)) }, null, null, err["vence_el"])
        if (f.venceEl.isNotBlank()) TextButton(onClick = { vm.cambiar(f.copy(venceEl = "")) }) { Text("Quitar la fecha") }
    }
    OutlinedTextField(f.notas, { vm.cambiar(f.copy(notas = it)) }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
    if (estado.producto != null) Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Lo sigo comprando", Modifier.weight(1f))
        Switch(f.activo, { vm.cambiar(f.copy(activo = it)) })
    }
    Button(onClick = { vm.guardar() }, enabled = !estado.guardando, modifier = Modifier.fillMaxWidth()) { Text(if (estado.guardando) "Guardando…" else "Guardar") }
}

private fun android.graphics.Bitmap?.asImageBitmapSeguro() = this?.asImageBitmap()

@Composable
private fun Numero(etiqueta: String, valor: String, error: String?, modifier: Modifier, onCambio: (String) -> Unit) {
    OutlinedTextField(valor, { onCambio(it.filter { c -> c.isDigit() }.take(3)) }, label = { Text(etiqueta) }, isError = error != null,
        supportingText = error?.let { { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = modifier)
}

@Composable
private fun Precios(p: Producto, onAnotar: () -> Unit) {
    Seccion("Precios", accion = { TextButton(onClick = onAnotar) { Text("Anotar precio") } }) {
        val precios = p.precios.orEmpty()
        if (precios.isEmpty()) Text("Aún no hay precios. Anótalo cuando lo veas, o se guarda solo al comprarlo con la lista.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        precios.forEachIndexed { i, pr ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(pr.tienda.orEmpty(), fontWeight = FontWeight.SemiBold)
                        if (i == 0 && precios.size > 1) Insignia("Más barato", Semantica.ingreso)
                    }
                    Text("${origen(pr.origen)} · ${Formato.fechaCorta(pr.fecha)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Formato.dinero(pr.precio) + if (pr.unidades > 1) " × ${pr.unidades}" else "", fontWeight = FontWeight.SemiBold)
                    if (pr.unidades > 1) Text("${Formato.dinero(pr.porUnidad)} c/u", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
        }
    }
}

private fun origen(o: String) = when (o) { "en_linea" -> "En línea"; "compra" -> "Compra"; else -> "Anotado" }

@Composable
private fun EnLinea(p: Producto, tiendas: List<Tienda>, valores: ValoresDespensa?, onBuscar: (String) -> Unit, onQuitar: (String) -> Unit, onAbrir: (String) -> Unit) {
    val fuentes = tiendas.filter { it.activo && it.fuente != null }.mapNotNull { it.fuente }.distinct().sorted()
    if (fuentes.isEmpty()) return
    var quitar by remember { mutableStateOf<String?>(null) }
    Seccion("En línea") {
        fuentes.forEach { fuente ->
            val nombre = nombreFuente(fuente, valores)
            val enlace = p.enLinea.orEmpty().firstOrNull { it.fuente == fuente }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.SemiBold)
                    if (enlace == null) Text("Sin ligar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        Text(enlace.nombre + if (enlace.unidades > 1) " · paquete de ${enlace.unidades}" else "", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(enabled = enlace.url != null) { enlace.url?.let(onAbrir) })
                        enlace.error?.let { Text(if (it == "no_encontrado") "La tienda ya no lo tenía: búscalo otra vez." else "No mostraba precio.", style = MaterialTheme.typography.bodySmall, color = Semantica.aviso) }
                    }
                }
                TextButton(onClick = { onBuscar(fuente) }) { Text(if (enlace == null) "Buscar" else "Cambiar") }
                if (enlace != null) TextButton(onClick = { quitar = fuente }) { Text("Quitar", color = Semantica.egreso) }
            }
        }
        if ("walmart" in fuentes && "donjuan" in fuentes) Text("Walmart y Don Juan comparten catálogo: al ligar uno se liga también el otro.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    quitar?.let { f -> Confirmar("¿Quitar la liga?", "Ya no se consultará su precio en ${nombreFuente(f, valores)}. Los precios leídos se quedan.", "Quitar", { quitar = null; onQuitar(f) }, { quitar = null }) }
}

fun nombreFuente(fuente: String, valores: ValoresDespensa?) = valores?.fuentes?.get(fuente)?.substringBefore(" (") ?: fuente

@Composable
private fun HojaPrecio(tiendas: List<Tienda>, onCerrar: () -> Unit, onGuardar: (Int, String, Int?, String) -> Unit) {
    var tienda by remember { mutableStateOf(tiendas.firstOrNull()?.id) }
    var precio by remember { mutableStateOf("") }
    var unidades by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(Formato.hoy()) }
    var guardando by remember { mutableStateOf(false) }
    HojaFormulario("Anotar un precio que viste", "Anotar", guardando, onCerrar, {
        val t = tienda ?: return@HojaFormulario
        if (!Formato.montoValido(precio)) return@HojaFormulario
        guardando = true; onGuardar(t, precio, unidades.toIntOrNull(), fecha)
    }) {
        if (tiendas.isEmpty()) Text("Primero agrega tus tiendas (botón de tienda en la Despensa).")
        Desplegable("Tienda", tiendas.map { it.id to it.nombre }, tienda, { tienda = it })
        CampoMonto("Precio", precio, { precio = it; guardando = false })
        OutlinedTextField(unidades, { unidades = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Paquete de (opcional)") },
            supportingText = { Text("Ej. 3 latas por $5.25: escribe 5.25 y 3; se compara por unidad.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        CampoFecha("Fecha", fecha, { fecha = it }, null, Formato.hoy())
    }
}

@Composable
private fun HojaBuscarEnLinea(fuente: String, estado: ProductoEstado, valores: ValoresDespensa?, onBuscar: (String) -> Unit, onCerrar: () -> Unit, onElegir: (ResultadoEnLinea, Int) -> Unit) {
    var texto by remember { mutableStateOf(estado.producto?.nombreCompleto.orEmpty()) }
    var unidades by remember { mutableStateOf("1") }
    HojaFormulario("Buscar en ${nombreFuente(fuente, valores)}", "Buscar", estado.buscando, onCerrar, { onBuscar(texto) }) {
        OutlinedTextField(texto, { texto = it.take(100) }, label = { Text("Buscar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        estado.errorBusqueda?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (estado.buscando) Text("Buscando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        estado.resultados?.let { lista ->
            if (lista.isEmpty()) Text("No salió nada. Prueba con menos palabras (ej. solo la marca y el producto).", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else OutlinedTextField(unidades, { unidades = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Allá se vende en paquete de") },
                supportingText = { Text("1 = suelto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            lista.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(r.imagen, r.nombre, tamano = 44.dp, cuadrado = true)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.nombre, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.precio?.let { Formato.dinero(it) } ?: "sin precio", style = MaterialTheme.typography.bodySmall)
                            if (r.mismoCodigo) Insignia("Mismo código de barras", Semantica.ingreso)
                        }
                    }
                    TextButton(onClick = { onElegir(r, unidades.toIntOrNull()?.coerceAtLeast(1) ?: 1) }) { Text("Es este") }
                }
                HorizontalDivider()
            }
        }
    }
}
