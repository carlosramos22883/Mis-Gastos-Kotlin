package com.carlosramos.misgastos.ui.eventos

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.CobroEventoBody
import com.carlosramos.misgastos.data.remote.dto.Evento
import com.carlosramos.misgastos.data.remote.dto.OpcionesEvento
import com.carlosramos.misgastos.data.remote.dto.PagadoEventoBody
import com.carlosramos.misgastos.data.remote.dto.ParticipanteEvento
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.HojaFormulario
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private fun String?.num() = this?.toDoubleOrNull() ?: 0.0

// ---------- Lista ----------

data class EventosEstado(val cargando: Boolean = true, val error: String? = null, val eventos: List<Evento> = emptyList())

@HiltViewModel
class EventosViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(EventosEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _estado.value = when (val r = repo.eventos()) {
            is Resultado.Ok -> EventosEstado(cargando = false, eventos = r.datos)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventosScreen(onAbrir: (Int) -> Unit, onNuevo: () -> Unit, onVolver: () -> Unit, viewModel: EventosViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (estado.eventos.isNotEmpty()) viewModel.cargar() }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Eventos") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { IconButton(onClick = onNuevo) { Icon(Icons.Default.Add, contentDescription = "Nuevo evento") } }
            )
        }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            estado.error != null && estado.eventos.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() }, Modifier.padding(relleno))
            estado.eventos.isEmpty() -> Vacio("¿Salieron a comer? Agrega un evento: quiénes fueron, qué pidió cada uno, la propina y los festejados.", Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Seccion("Resumen") { FilaValor("Te falta cobrar", Formato.dinero(estado.eventos.sumOf { it.porCobrar.num() }), negrita = true) }
                }
                items(estado.eventos, key = { it.id }) { e ->
                    Card(Modifier.fillMaxWidth().clickable { onAbrir(e.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(e.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${Formato.fechaCorta(e.fecha)}${e.lugar?.let { " · $it" } ?: ""} · ${e.participantes.size} personas",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formato.dinero(e.total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (!e.pagoElHogar) "Pagó ${e.pagador?.nombre.orEmpty()}" else if (e.porCobrar.num() > 0) "Falta cobrar ${Formato.dinero(e.porCobrar)}" else "Todo cobrado",
                                style = MaterialTheme.typography.bodySmall, color = if (e.pagoElHogar && e.porCobrar.num() > 0) Semantica.egreso else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Detalle ----------

/** Hoja abierta: cobrar a un contacto (pagó el hogar) o marcar que alguien ya pagó (pagó otra persona). */
data class HojaEvento(
    val participante: ParticipanteEvento,
    val cobro: Boolean,
    val monto: String = "",
    val fecha: String = Formato.hoy(),
    val destino: String? = "efectivo",
    val categoriaId: Int? = null,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null,
    val confirmar: String? = null
)

data class EventoDetalleEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val evento: Evento? = null,
    val opciones: OpcionesEvento? = null,
    val comunes: Comunes? = null,
    val hoja: HojaEvento? = null,
    val compartiendo: Boolean = false,
    val imagen: File? = null,
    val mensaje: String? = null,
    val eliminado: Boolean = false
)

@HiltViewModel
class EventoDetalleViewModel @Inject constructor(private val repo: FinanzasRepository, estadoGuardado: SavedStateHandle) : ViewModel() {
    private val id: Int = checkNotNull(estadoGuardado["id"])
    private val _estado = MutableStateFlow(EventoDetalleEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        val comunes = (repo.comunes() as? Resultado.Ok)?.datos
        val opciones = (repo.opcionesEvento() as? Resultado.Ok)?.datos
        _estado.value = when (val r = repo.evento(id)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, error = null, evento = r.datos, opciones = opciones, comunes = comunes)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun abrirCobro(p: ParticipanteEvento) { _estado.value = _estado.value.copy(hoja = HojaEvento(p, cobro = true, monto = p.pendiente)) }
    fun abrirPagado(p: ParticipanteEvento) { _estado.value = _estado.value.copy(hoja = HojaEvento(p, cobro = false, destino = if (p.userId != null) "efectivo" else null)) }
    fun cambiar(h: HojaEvento) { _estado.value = _estado.value.copy(hoja = h) }
    fun cerrar() { _estado.value = _estado.value.copy(hoja = null) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }
    fun imagenCompartida() { _estado.value = _estado.value.copy(imagen = null) }

    fun guardarHoja(confirmado: Boolean = false) {
        val h = _estado.value.hoja ?: return
        val errores = buildMap {
            if (h.cobro && !Formato.montoValido(h.monto)) put("monto", "Monto positivo con máximo dos decimales.")
            if (!h.cobro && h.participante.userId != null && h.destino != "ninguno" && h.categoriaId == null) put("categoria_personal_id", "Elige una categoría.")
        }
        if (errores.isNotEmpty()) { cambiar(h.copy(errores = errores)); return }
        cambiar(h.copy(guardando = true, errores = emptyMap(), error = null, confirmar = null))
        viewModelScope.launch {
            val r = if (h.cobro) repo.cobrarEvento(id, h.participante.id, CobroEventoBody(h.monto, h.fecha, h.destino ?: "efectivo", null))
            else repo.marcarPagadoEvento(id, h.participante.id, PagadoEventoBody(h.fecha, h.destino, if (h.destino == "ninguno") null else h.categoriaId, if (confirmado) true else null))
            when (r) {
                is Resultado.Ok -> _estado.value = _estado.value.copy(evento = r.datos, hoja = null, mensaje = if (h.cobro) "Pago de ${h.participante.nombre} registrado." else "${h.participante.nombre} ya pagó su parte.")
                is Resultado.Falla -> {
                    val pregunta = r.campos["confirmar"]
                    cambiar(h.copy(guardando = false, errores = r.campos - "confirmar", error = if (pregunta != null) null else r.mensaje, confirmar = pregunta))
                }
            }
        }
    }

    fun desmarcar(p: ParticipanteEvento) = viewModelScope.launch {
        when (val r = repo.desmarcarPagadoEvento(id, p.id)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(evento = r.datos, mensaje = "Se quitó el pago de ${p.nombre}.")
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }

    fun eliminar() = viewModelScope.launch {
        when (val r = repo.eliminarEvento(id)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(eliminado = true)
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }

    fun compartir(cache: File) = viewModelScope.launch {
        _estado.value = _estado.value.copy(compartiendo = true)
        _estado.value = when (val r = repo.descargarImagenEvento(id, File(cache, "eventos"))) {
            is Resultado.Ok -> _estado.value.copy(compartiendo = false, imagen = r.datos)
            is Resultado.Falla -> _estado.value.copy(compartiendo = false, mensaje = r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoDetalleScreen(onVolver: () -> Unit, onEditar: (Int) -> Unit, onEliminado: () -> Unit, onAbrirContacto: (Int) -> Unit = {}, viewModel: EventoDetalleViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var borrar by remember { mutableStateOf(false) }
    var quitar by remember { mutableStateOf<ParticipanteEvento?>(null) }
    LaunchedEffect(Unit) { if (estado.evento != null) viewModel.cargar() }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    LaunchedEffect(estado.eliminado) { if (estado.eliminado) onEliminado() }
    // La imagen se comparte con la app que se elija (WhatsApp, etc.)
    LaunchedEffect(estado.imagen) {
        estado.imagen?.let { f ->
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", f)
            // ClipData: así la hoja de compartir muestra la miniatura de la imagen
            val enviar = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TITLE, estado.evento?.nombre).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .apply { clipData = android.content.ClipData.newRawUri(estado.evento?.nombre ?: "Evento", uri) }
            contexto.startActivity(Intent.createChooser(enviar, "Compartir el detalle"))
            viewModel.imagenCompartida()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(estado.evento?.nombre ?: "Evento", maxLines = 1) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    estado.evento?.let { e ->
                        if (estado.compartiendo) CircularProgressIndicator(Modifier.size(24.dp).padding(2.dp), strokeWidth = 2.dp)
                        else IconButton(onClick = { viewModel.compartir(contexto.cacheDir) }) { Icon(Icons.Default.Share, contentDescription = "Compartir imagen") }
                        IconButton(onClick = { onEditar(e.id) }) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                        IconButton(onClick = { borrar = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                    }
                }
            )
        }
    ) { relleno ->
        val e = estado.evento
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            e == null -> ErrorConReintento(estado.error ?: "No se encontró el evento.", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Seccion("Resumen") {
                        Text("${Formato.fechaLarga(e.fecha)}${e.lugar?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilaValor("Pagó la cuenta", e.pagador?.nombre.orEmpty())
                        FilaValor("Consumo", Formato.dinero(e.subtotal))
                        FilaValor("Propina", Formato.dinero(e.propina))
                        FilaValor("Total", Formato.dinero(e.total), negrita = true)
                        if (e.pagoElHogar) FilaValor("Te falta cobrar", Formato.dinero(e.porCobrar), colorValor = if (e.porCobrar.num() > 0) Semantica.egreso else Semantica.ingreso, negrita = true)
                    }
                }
                items(e.participantes, key = { it.id }) { p -> TarjetaParticipante(e, p, viewModel::abrirCobro, viewModel::abrirPagado, onAbrirContacto) { quitar = p } }
                item {
                    Seccion("Consumos") {
                        e.consumos.forEach { c -> FilaValor("${c.cantidad} × ${c.descripcion} (${Formato.dinero(c.precio)})", Formato.dinero(c.total)) }
                    }
                }
                e.fotoUrl?.let { url ->
                    item {
                        Seccion("Foto de la cuenta") {
                            coil.compose.AsyncImage(Formato.urlImagen(url), "Foto de la cuenta", Modifier.fillMaxWidth(), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth)
                        }
                    }
                }
            }
        }
    }

    estado.hoja?.let { h -> HojaPago(h, estado, viewModel) }
    if (borrar) Confirmar("¿Eliminar evento?", "Se eliminan el evento, su pago y lo que te deben de él.", "Eliminar", { borrar = false; viewModel.eliminar() }, { borrar = false })
    quitar?.let { p -> Confirmar("¿Quitar el pago?", "Se quitará el pago de ${p.nombre} (y su movimiento, si lo tiene).", "Quitar", { quitar = null; viewModel.desmarcar(p) }, { quitar = null }) }
}

@Composable
private fun TarjetaParticipante(e: Evento, p: ParticipanteEvento, onCobrar: (ParticipanteEvento) -> Unit, onPagado: (ParticipanteEvento) -> Unit, onAbrirContacto: (Int) -> Unit, onQuitar: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val (texto, color) = when (p.estado) {
        "pagador" -> "Pagó la cuenta" to Semantica.info
        "festejado" -> "Festejado" to Semantica.aviso
        "hogar" -> "Del hogar" to MaterialTheme.colorScheme.outline
        "pagado" -> "Pagado" to Semantica.ingreso
        "parcial" -> "Abonó" to Semantica.aviso
        else -> "Pendiente" to Semantica.egreso
    }
    Card(Modifier.fillMaxWidth().clickable { abierto = !abierto }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(Formato.dinero(p.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Insignia(texto, color)
                if (p.estado == "parcial") { Spacer(Modifier.width(8.dp)); Text("falta ${Formato.dinero(p.pendiente)}", style = MaterialTheme.typography.bodySmall) }
            }
            if (abierto) {
                p.detalle.forEach { d -> FilaValor("${if (d.cantidad > 1) "${d.cantidad}× " else ""}${d.descripcion}", Formato.dinero(d.monto)) }
                if (p.propina.num() > 0) FilaValor("Propina", Formato.dinero(p.propina))
                if (p.festejo.num() > 0) FilaValor("Lo del cumpleañero", Formato.dinero(p.festejo))
                p.cobros.forEach { c -> FilaValor("Cobro del ${Formato.fechaCorta(c.fecha)}", Formato.dinero(c.monto), colorValor = Semantica.ingreso) }
                p.fechaPago?.let { Text("Pagó el ${Formato.fechaCorta(it)}", style = MaterialTheme.typography.bodySmall, color = Semantica.ingreso) }
                p.contacto?.let { c -> TextButton(onClick = { onAbrirContacto(c.id) }) { Text("Ver contacto (todo lo que debe)") } }
            }
            when {
                e.pagoElHogar && p.deudaId != null && p.pendiente.num() > 0 -> OutlinedButton(onClick = { onCobrar(p) }) { Text("Cobrar") }
                !e.pagoElHogar && p.estado in listOf("pendiente", "parcial") -> OutlinedButton(onClick = { onPagado(p) }) { Text("Ya pagó") }
                !e.pagoElHogar && p.fechaPago != null -> TextButton(onClick = onQuitar) { Text("Quitar pago") }
            }
        }
    }
}

@Composable
private fun HojaPago(h: HojaEvento, estado: EventoDetalleEstado, vm: EventoDetalleViewModel) {
    val p = h.participante
    HojaFormulario(
        titulo = if (h.cobro) "Cobrar a ${p.nombre}" else "${p.nombre} ya pagó",
        accion = if (h.cobro) "Registrar pago" else "Marcar como pagado",
        guardando = h.guardando, onCerrar = vm::cerrar, onAccion = { vm.guardarHoja() }, error = h.error
    ) {
        if (h.cobro) {
            Text("Te debe ${Formato.dinero(p.pendiente)}. El dinero entra solo al efectivo o a la cuenta que elijas.", style = MaterialTheme.typography.bodyMedium)
            CampoMonto("Monto", h.monto, { vm.cambiar(h.copy(monto = it)) }, error = h.errores["monto"])
            Desplegable("¿A dónde entró el dinero?", estado.opciones?.destinos.orEmpty().map { it.valor to it.etiqueta }, h.destino, { vm.cambiar(h.copy(destino = it)) }, error = h.errores["destino"])
        } else {
            Text("Su parte es ${Formato.dinero(p.total)} y se la pagó a ${estado.evento?.pagador?.nombre.orEmpty()}.", style = MaterialTheme.typography.bodyMedium)
        }
        CampoFecha("Fecha", h.fecha, { vm.cambiar(h.copy(fecha = it)) }, desde = null, hasta = Formato.hoy(), error = h.errores["fecha"])
        if (!h.cobro && p.userId != null) {
            // Un miembro del hogar: su parte es un gasto nuestro
            Desplegable("¿De dónde salió el dinero?", estado.opciones?.origenes.orEmpty().map { it.valor to it.etiqueta }, h.destino, { vm.cambiar(h.copy(destino = it)) }, error = h.errores["origen"])
            if (h.destino != "ninguno") Desplegable("Categoría del gasto", estado.comunes?.categoriasDe("egreso").orEmpty().map { it.id to it.nombre }, h.categoriaId,
                { vm.cambiar(h.copy(categoriaId = it)) }, error = h.errores["categoria_personal_id"])
        }
    }
    h.confirmar?.let { texto -> Confirmar("¿Registrar de todas formas?", texto, "Sí, registrar", { vm.guardarHoja(confirmado = true) }, { vm.cambiar(h.copy(confirmar = null)) }) }
}
