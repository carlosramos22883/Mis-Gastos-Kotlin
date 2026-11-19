package com.carlosramos.misgastos.ui.compras

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.DatosPago
import com.carlosramos.misgastos.data.remote.dto.DecisionBody
import com.carlosramos.misgastos.data.remote.dto.ExcesoPresupuesto
import com.carlosramos.misgastos.data.remote.dto.ExtraBody
import com.carlosramos.misgastos.data.remote.dto.ItemBody
import com.carlosramos.misgastos.data.remote.dto.ItemCambio
import com.carlosramos.misgastos.data.remote.dto.ItemLista
import com.carlosramos.misgastos.data.remote.dto.ListaCompra
import com.carlosramos.misgastos.data.remote.dto.PagoBody
import com.carlosramos.misgastos.data.remote.dto.Producto
import com.carlosramos.misgastos.data.remote.dto.Ruta
import com.carlosramos.misgastos.data.remote.dto.Tienda
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.DialogoCubrirPresupuesto
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.HojaFormulario
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListaEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val lista: ListaCompra? = null,
    val tiendas: List<Tienda> = emptyList(),
    val enviando: Int? = null,
    val mensaje: String? = null,
    val avance: String? = null,
    val salir: Boolean = false,
    // «Pagar aquí»
    val pago: DatosPago? = null,
    val pagando: Boolean = false,
    val errorPago: String? = null,
    val camposPago: Map<String, String> = emptyMap(),
    val exceso: ExcesoPresupuesto? = null,
    val confirmarSobregiro: String? = null,
    // Ruta
    val ruta: Ruta? = null,
    val productos: List<Producto> = emptyList()
)

@HiltViewModel
class ListaCompraViewModel @Inject constructor(private val repo: FinanzasRepository, guardado: SavedStateHandle) : ViewModel() {
    private val id: Int = guardado.get<Int>("id") ?: 0
    private val _estado = MutableStateFlow(ListaEstado())
    val estado = _estado.asStateFlow()
    private var ultimoPago: PagoBody? = null
    private val decisiones = mutableMapOf<String, DecisionBody>()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        val tiendas = (repo.tiendas() as? Resultado.Ok)?.datos.orEmpty().filter { it.activo }
        _estado.value = when (val r = repo.lista(id)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, error = null, lista = r.datos, tiendas = tiendas)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    private fun aplicar(r: Resultado<ListaCompra>, mensaje: String? = null) {
        _estado.value = when (r) {
            is Resultado.Ok -> _estado.value.copy(lista = r.datos, enviando = null, mensaje = mensaje)
            is Resultado.Falla -> _estado.value.copy(enviando = null, mensaje = r.mensaje)
        }
    }

    fun elegirTienda(tienda: Int?) = viewModelScope.launch { aplicar(repo.elegirTienda(id, tienda)) }
    fun cambiar(item: ItemLista, cambio: ItemCambio) = viewModelScope.launch {
        _estado.value = _estado.value.copy(enviando = item.id)
        aplicar(repo.cambiarItem(id, item.id, cambio))
    }
    fun quitar(item: ItemLista) = viewModelScope.launch { aplicar(repo.quitarItem(id, item.id), "Quitado de la lista.") }
    fun agregar(body: ItemBody, alTerminar: () -> Unit) = viewModelScope.launch {
        val r = repo.agregarALista(id, body)
        aplicar(r, (r as? Resultado.Ok)?.let { "Agregado." })
        if (r is Resultado.Ok) alTerminar()
    }
    fun cargarProductos() = viewModelScope.launch {
        (repo.productos() as? Resultado.Ok)?.let { _estado.value = _estado.value.copy(productos = it.datos.data.filter { p -> p.activo }) }
    }
    fun faltantes() = viewModelScope.launch { aplicar(repo.agregarFaltantes(id), "Se agregó lo que falta.") }
    fun terminar() = viewModelScope.launch {
        when (val r = repo.terminarLista(id)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(lista = r.datos, mensaje = "Compra terminada: lo comprado subió en tu despensa.")
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }
    fun eliminar() = viewModelScope.launch {
        when (val r = repo.eliminarLista(id)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(salir = true)
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }

    /** Pide los precios de uno en uno; el servidor dice cuánto esperar entre cada uno. */
    fun consultarPrecios() = viewModelScope.launch {
        if (_estado.value.avance != null) return@launch
        _estado.value = _estado.value.copy(avance = "Empezando…")
        val inicio = repo.iniciarConsulta(id)
        if (inicio is Resultado.Falla) { _estado.value = _estado.value.copy(avance = null, mensaje = inicio.mensaje); return@launch }
        val datos = (inicio as Resultado.Ok).datos
        val consultaId = datos.consultaId
        val primero = datos.estado
        if (datos.alDia || consultaId == null || primero == null) { _estado.value = _estado.value.copy(avance = null, mensaje = datos.message ?: "Los precios ya son de hoy."); return@launch }
        var e: com.carlosramos.misgastos.data.remote.dto.EstadoConsulta = primero
        while (!e.terminada) {
            _estado.value = _estado.value.copy(avance = "Consultando ${minOf(e.hechos + 1, e.total)} de ${e.total}…")
            when (val paso = repo.siguienteConsulta(id, consultaId)) {
                is Resultado.Ok -> { e = paso.datos; delay(paso.datos.espera ?: 2100) }
                is Resultado.Falla -> { _estado.value = _estado.value.copy(avance = null, mensaje = paso.mensaje); return@launch }
            }
        }
        val partes = listOfNotNull(
            e.nuevos.takeIf { it > 0 }?.let { if (it == 1) "Se leyó 1 precio." else "Se leyeron $it precios." },
            e.reusados.takeIf { it > 0 }?.let { if (it == 1) "1 ya era de hoy." else "$it ya eran de hoy." },
            e.fallas.takeIf { it > 0 }?.let { if (it == 1) "1 no se pudo leer." else "$it no se pudieron leer." }
        )
        _estado.value = _estado.value.copy(avance = null, mensaje = partes.joinToString(" ").ifBlank { "Listo." })
        cargar()
    }

    fun abrirPago() = viewModelScope.launch {
        decisiones.clear()
        when (val r = repo.datosPago(id)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(pago = r.datos, errorPago = null, camposPago = emptyMap())
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }
    fun cerrarPago() { _estado.value = _estado.value.copy(pago = null, exceso = null, confirmarSobregiro = null) }

    fun pagar(body: PagoBody) = viewModelScope.launch {
        ultimoPago = body
        _estado.value = _estado.value.copy(pagando = true, errorPago = null, camposPago = emptyMap())
        when (val r = repo.pagarCompra(id, body.copy(presupuestos = decisiones.toMap().ifEmpty { null }))) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(pagando = false, pago = null, lista = r.datos, mensaje = "Pago registrado.")
            is Resultado.Falla -> _estado.value = when {
                r.presupuesto != null -> _estado.value.copy(pagando = false, exceso = r.presupuesto)
                r.campos.keys == setOf("confirmar") -> _estado.value.copy(pagando = false, confirmarSobregiro = r.mensaje)
                else -> _estado.value.copy(pagando = false, errorPago = r.mensaje, camposPago = r.campos)
            }
        }
    }
    /** Lo decidido en el aviso de presupuesto de una categoría: se guarda por su clave y se reenvía. */
    fun decidir(modo: String, traspasos: List<com.carlosramos.misgastos.data.remote.dto.TraspasoBody>?) {
        val clave = _estado.value.exceso?.clave ?: return
        decisiones[clave] = DecisionBody(modo, traspasos)
        _estado.value = _estado.value.copy(exceso = null)
        ultimoPago?.let { pagar(it) }
    }
    fun sinDecidir() { _estado.value = _estado.value.copy(exceso = null) }
    fun confirmarSobregiro(si: Boolean) {
        _estado.value = _estado.value.copy(confirmarSobregiro = null)
        if (si) ultimoPago?.let { pagar(it.copy(confirmar = true)) }
    }
    fun deshacerPago(pago: Int) = viewModelScope.launch { aplicar(repo.deshacerPago(id, pago), "Pago deshecho.") }

    fun ruta(costo: String?) = viewModelScope.launch {
        when (val r = repo.ruta(id, costo)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(ruta = r.datos)
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }
    fun cerrarRuta() { _estado.value = _estado.value.copy(ruta = null) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCompraScreen(onVolver: () -> Unit, viewModel: ListaCompraViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var agregando by remember { mutableStateOf(false) }
    var precioDe by remember { mutableStateOf<ItemLista?>(null) }
    var confirmarTerminar by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var deshacer by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    LaunchedEffect(estado.salir) { if (estado.salir) onVolver() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(title = { Text(estado.lista?.nombre ?: "Lista de compras", maxLines = 1, overflow = TextOverflow.Ellipsis) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } })
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        val l = estado.lista
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            estado.error != null || l == null -> ErrorConReintento(estado.error ?: "", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (l.abierta) item { Cabecera(l, estado, viewModel) }
                if (l.pendientes.isNotEmpty()) {
                    item { Text(if (l.abierta) "POR COMPRAR" else "NO SE COMPRÓ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(l.pendientes, key = { it.id }) { FilaItem(it, l, estado.enviando == it.id, viewModel, { precioDe = it }) }
                }
                if (l.comprados.isNotEmpty()) {
                    item { Text(if (l.abierta) "EN EL CARRITO" else "COMPRADO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(l.comprados, key = { it.id }) { FilaItem(it, l, estado.enviando == it.id, viewModel, { precioDe = it }) }
                }
                if (l.pendientes.isEmpty() && l.comprados.isEmpty()) item { Text("La lista está vacía. Agrega lo que quieres comprar.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (l.pagos.isNotEmpty()) item {
                    Seccion("Pagos") {
                        l.pagos.forEach { p ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${Formato.dinero(p.total)} en ${p.tienda ?: "una tienda"}", fontWeight = FontWeight.SemiBold)
                                    Text("${Formato.fechaCorta(p.fecha)} · ${p.origenNombre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(p.gastos.joinToString(" · ") { "${it.categoria ?: "Sin categoría"} ${Formato.dinero(it.monto)}" }, style = MaterialTheme.typography.bodySmall)
                                }
                                if (l.abierta) TextButton(onClick = { deshacer = p.id }) { Text("Deshacer", color = Semantica.egreso) }
                            }
                        }
                    }
                }
                if (l.abierta) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.cargarProductos(); agregando = true }, modifier = Modifier.weight(1f)) { Text("Agregar") }
                            OutlinedButton(onClick = { viewModel.faltantes() }, modifier = Modifier.weight(1f)) { Text("Lo que falta") }
                        }
                        Button(onClick = { confirmarTerminar = true }, modifier = Modifier.fillMaxWidth()) { Text("Terminar compra") }
                    }
                }
                item { TextButton(onClick = { confirmarEliminar = true }) { Text("Eliminar lista", color = Semantica.egreso) } }
            }
        }
    }

    if (agregando) HojaAgregar(estado.productos, { agregando = false }) { body -> viewModel.agregar(body) { agregando = false } }
    precioDe?.let { item ->
        var precio by remember(item) { mutableStateOf(item.precio.orEmpty()) }
        AlertDialog(onDismissRequest = { precioDe = null }, title = { Text("Precio c/u") },
            text = { CampoMonto("Precio de ${item.descripcion}", precio, { precio = it }) },
            confirmButton = { TextButton(onClick = { viewModel.cambiar(item, ItemCambio(precio = precio.ifBlank { "" })); precioDe = null }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { precioDe = null }) { Text("Cancelar") } })
    }
    if (confirmarTerminar) {
        val porPagar = estado.lista?.resumen?.porPagar ?: 0
        Confirmar("¿Terminar la compra?", (if (porPagar > 0) "Aún no has pagado $porPagar ${if (porPagar == 1) "cosa" else "cosas"} del carrito: si terminas, no se registrará su gasto. " else "") +
            "Lo del carrito se sumará a tu despensa; lo demás queda como no comprado.", "Terminar", { confirmarTerminar = false; viewModel.terminar() }, { confirmarTerminar = false })
    }
    if (confirmarEliminar) Confirmar("¿Eliminar la lista?", "Si tiene pagos, sus gastos quedan como movimientos normales.", "Eliminar", { confirmarEliminar = false; viewModel.eliminar() }, { confirmarEliminar = false })
    deshacer?.let { p -> Confirmar("¿Deshacer este pago?", "Se eliminan sus gastos y lo comprado vuelve al carrito sin pagar. Los extras se quitan de la lista.", "Deshacer", { deshacer = null; viewModel.deshacerPago(p) }, { deshacer = null }) }
    estado.pago?.let { HojaPagar(it, estado, viewModel) }
    estado.exceso?.let { ex -> DialogoCubrirPresupuesto(ex, onDecidir = { d -> viewModel.decidir(d.modo, d.traspasos) }, onCancelar = { viewModel.sinDecidir() }) }
    estado.confirmarSobregiro?.let { texto ->
        AlertDialog(onDismissRequest = { viewModel.confirmarSobregiro(false) }, title = { Text("¿Registrar de todas formas?") }, text = { Text(texto) },
            confirmButton = { TextButton(onClick = { viewModel.confirmarSobregiro(true) }) { Text("Sí, registrar") } },
            dismissButton = { TextButton(onClick = { viewModel.confirmarSobregiro(false) }) { Text("Cancelar") } })
    }
    estado.ruta?.let { HojaRuta(it, estado.tiendas, { viewModel.cerrarRuta() }, { c -> viewModel.ruta(c) }) { t -> viewModel.elegirTienda(t); viewModel.cerrarRuta() } }
}

@Composable
private fun Cabecera(l: ListaCompra, estado: ListaEstado, vm: ListaCompraViewModel) {
    val r = l.resumen
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (estado.tiendas.isEmpty()) Text("Para anotar precios y lo que no había, agrega tus tiendas (Despensa → Tiendas).", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Desplegable("¿En qué tienda estás?", listOf<Pair<Int?, String>>(null to "Elige la tienda…") + estado.tiendas.map { it.id to it.nombre }, l.tiendaActualId, { vm.elegirTienda(it) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${r?.enCarrito ?: 0} de ${r?.totalItems ?: 0} en el carrito", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if ((r?.sinPrecio ?: 0) > 0) Text(if (r!!.sinPrecio == 1) "1 sin precio" else "${r.sinPrecio} sin precio", style = MaterialTheme.typography.bodySmall, color = Semantica.aviso)
                    if ((r?.pagado?.toDoubleOrNull() ?: 0.0) > 0) Text("Pagado: ${Formato.dinero(r!!.pagado)}", style = MaterialTheme.typography.bodySmall, color = Semantica.ingreso)
                }
                Text(Formato.dinero(r?.total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (l.tiendaActualId != null) {
                Button(onClick = { vm.abrirPago() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if ((r?.porPagar ?: 0) > 0) "Pagar aquí · ${Formato.dinero(r!!.porPagarTotal)}" else "Registrar un pago aquí")
                }
            }
            HorizontalDivider()
            Text(estado.avance ?: (l.consultasRestantes?.let { if (it == 1) "Precios en línea: te queda 1 consulta hoy." else "Precios en línea: te quedan $it consultas hoy." } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = if (estado.avance != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.consultarPrecios() }, enabled = estado.avance == null && estado.tiendas.any { it.fuente != null }, modifier = Modifier.weight(1f)) { Text("Consultar precios") }
                OutlinedButton(onClick = { vm.ruta(null) }, modifier = Modifier.weight(1f)) { Text("Ruta de compra") }
            }
        }
    }
}

@Composable
private fun FilaItem(item: ItemLista, l: ListaCompra, enviando: Boolean, vm: ListaCompraViewModel, onPrecio: () -> Unit) {
    val editable = l.abierta && !item.pagado
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val verde = Semantica.ingreso
                Box(Modifier.size(36.dp).clip(CircleShape).then(if (item.comprado) Modifier.border(18.dp, verde, CircleShape) else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
                    .clickable(enabled = editable && !enviando) { vm.cambiar(item, ItemCambio(comprado = !item.comprado)) }, contentAlignment = Alignment.Center) {
                    if (item.comprado) Icon(Icons.Default.Check, contentDescription = "En el carrito", tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Avatar(item.fotoUrl, item.descripcion, tamano = 44.dp, cuadrado = true)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.descripcion, textDecoration = if (item.comprado) TextDecoration.LineThrough else null)
                    Text(listOfNotNull(item.categoria ?: if (item.extra) "Extra" else "Anotado", "faltaba".takeIf { item.automatico }, "básico".takeIf { item.basico }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    item.noHabia?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Semantica.aviso, fontWeight = FontWeight.Medium) }
                    if (item.pagado) Insignia("Pagado", Semantica.ingreso)
                }
                if (editable) {
                    FilledTonalIconButton(onClick = { vm.cambiar(item, ItemCambio(cantidad = item.cantidad - 1)) }, enabled = !enviando && item.cantidad > 1, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, contentDescription = "Uno menos") }
                    Text("${item.cantidad}", Modifier.padding(horizontal = 6.dp), fontWeight = FontWeight.Bold)
                    FilledTonalIconButton(onClick = { vm.cambiar(item, ItemCambio(cantidad = item.cantidad + 1)) }, enabled = !enviando, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, contentDescription = "Uno más") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Más") }
                        DropdownMenu(menu, { menu = false }) {
                            if (!item.comprado && l.tiendaActualId != null) DropdownMenuItem(text = { Text(if (item.noHabia != null) "Sí había" else "No había") }, onClick = { menu = false; vm.cambiar(item, ItemCambio(noHabia = item.noHabia == null)) })
                            DropdownMenuItem(text = { Text("Quitar", color = Semantica.egreso) }, onClick = { menu = false; vm.quitar(item) })
                        }
                    }
                } else Text("× ${item.cantidad}", fontWeight = FontWeight.SemiBold)
            }
            if (item.comprado) Row(Modifier.padding(start = 46.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.precio?.let { "Precio c/u ${Formato.dinero(it)}" + (item.subtotal?.let { s -> " = ${Formato.dinero(s)}" } ?: "") } ?: "Sin precio",
                    color = if (editable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable(enabled = editable, onClick = onPrecio))
                item.tienda?.let { Text(" · en $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (!item.comprado && item.comparacion.isNotEmpty()) Column(Modifier.padding(start = 46.dp)) {
                item.comparacion.forEachIndexed { i, c ->
                    val mejor = i == 0 && item.comparacion.size > 1
                    Row {
                        Text(c.tienda, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (mejor) FontWeight.SemiBold else null, color = if (mejor) Semantica.ingreso else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${Formato.dinero(c.porUnidad)}  ${c.cuando}", style = MaterialTheme.typography.bodySmall,
                            color = if (mejor) Semantica.ingreso else if (c.viejo) Semantica.aviso else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HojaAgregar(productos: List<Producto>, onCerrar: () -> Unit, onAgregar: (ItemBody) -> Unit) {
    var producto by remember { mutableStateOf<Int?>(null) }
    var texto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    HojaFormulario("Agregar a la lista", "Agregar", false, onCerrar, {
        val c = cantidad.toIntOrNull()
        if (producto != null) onAgregar(ItemBody(productoId = producto, cantidad = c)) else if (texto.isNotBlank()) onAgregar(ItemBody(descripcion = texto.trim(), cantidad = c))
    }) {
        Desplegable("De tu despensa", productos.map<Producto, Pair<Int?, String>> { it.id to it.nombreCompleto }, producto, { producto = it; texto = "" })
        Text("o anota algo que no está en tu despensa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(texto, { texto = it.take(200); if (it.isNotBlank()) producto = null }, label = { Text("Ej. Piñata para el cumpleaños") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(cantidad, { cantidad = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

/** Una fila de extras detallados (categoría: id o "nueva:Nombre"). */
private data class FilaExtra(val descripcion: String = "", val precio: String = "", val categoria: String = "", val nueva: String = "")

@Composable
private fun HojaPagar(d: DatosPago, estado: ListaEstado, vm: ListaCompraViewModel) {
    val hayItems = d.items.isNotEmpty()
    var montoLista by remember(d) { mutableStateOf(if (hayItems) d.total else "") }
    var categoria by remember(d) { mutableStateOf(d.categoriaSugerida?.toString() ?: "") }
    var nuevaCategoria by remember(d) { mutableStateOf("") }
    var modo by remember(d) { mutableStateOf("ninguno") }
    var extrasTotal by remember(d) { mutableStateOf("") }
    var extrasCategoria by remember(d) { mutableStateOf("") }
    var extrasNueva by remember(d) { mutableStateOf("") }
    val filas = remember(d) { mutableStateListOf(FilaExtra()) }
    var origen by remember(d) { mutableStateOf(d.origenes.firstOrNull()?.clave.orEmpty()) }
    var fecha by remember(d) { mutableStateOf(minOf(Formato.hoy(), d.ciclo.fin)) }
    val opciones = d.categorias.map { it.id.toString() to it.nombre } + ("__nueva" to "+ Nueva categoría…")
    fun clave(sel: String, nueva: String) = if (sel == "__nueva") nueva.trim().takeIf { it.isNotBlank() }?.let { "nueva:$it" } ?: "" else sel
    fun num(t: String) = t.toDoubleOrNull() ?: 0.0
    val extras = when (modo) { "total" -> num(extrasTotal); "detalle" -> filas.sumOf { num(it.precio) }; else -> 0.0 }
    val total = (if (hayItems) num(montoLista) else 0.0) + extras
    val err = estado.camposPago

    HojaFormulario("Pagar en ${d.tienda.nombre}", "Pagar ${Formato.dinero(total)}", estado.pagando, { vm.cerrarPago() }, {
        vm.pagar(PagoBody(origen, fecha, null, clave(categoria, nuevaCategoria).takeIf { hayItems }, montoLista.takeIf { hayItems }, modo,
            extrasTotal.takeIf { modo == "total" }, clave(extrasCategoria, extrasNueva).takeIf { modo == "total" },
            filas.filter { it.descripcion.isNotBlank() || it.precio.isNotBlank() }.map { ExtraBody(it.descripcion.trim(), it.precio, clave(it.categoria, it.nueva)) }.takeIf { modo == "detalle" }))
    }, error = estado.errorPago) {
        if (hayItems) {
            d.items.forEach { i ->
                Row { Text("${i.descripcion} × ${i.cantidad}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(i.subtotal?.let { Formato.dinero(it) } ?: "sin precio", style = MaterialTheme.typography.bodySmall, color = if (i.subtotal == null) Semantica.aviso else Color.Unspecified) }
            }
            Text("Suma de lo anotado: ${Formato.dinero(d.total)}" + if (d.sinPrecio > 0) " (${d.sinPrecio} sin precio: escribe el total del ticket)" else "", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            CampoMonto("Total de lo de la lista", montoLista, { montoLista = it }, error = err["monto_lista"], apoyo = "Si el ticket dice otra cosa, escribe lo del ticket.")
            Desplegable("Categoría de lo de la lista", opciones, categoria, { categoria = it }, error = err["categoria"])
            if (categoria == "__nueva") OutlinedTextField(nuevaCategoria, { nuevaCategoria = it.take(100) }, label = { Text("Nombre de la categoría nueva") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        } else Text("No hay nada del carrito por pagar aquí. Puedes registrar solo los extras.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("¿Compraste algo que no estaba en la lista?", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(modo == "ninguno", { modo = "ninguno" }, { Text("No") })
            FilterChip(modo == "total", { modo = "total" }, { Text("Un solo total") })
            FilterChip(modo == "detalle", { modo = "detalle" }, { Text("Cada cosa") })
        }
        if (modo == "total") {
            CampoMonto("Total de lo extra", extrasTotal, { extrasTotal = it }, error = err["extras_total"])
            Desplegable("Categoría de lo extra", opciones, extrasCategoria, { extrasCategoria = it }, error = err["extras_categoria"])
            if (extrasCategoria == "__nueva") OutlinedTextField(extrasNueva, { extrasNueva = it.take(100) }, label = { Text("Nombre de la categoría nueva") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (modo == "detalle") {
            filas.forEachIndexed { i, f ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(f.descripcion, { filas[i] = f.copy(descripcion = it.take(200)) }, label = { Text("¿Qué fue?") }, singleLine = true, modifier = Modifier.weight(1f),
                                isError = err["extras.$i.descripcion"] != null)
                            CampoMonto("Precio", f.precio, { filas[i] = f.copy(precio = it) }, Modifier.weight(0.7f), err["extras.$i.precio"])
                        }
                        Desplegable("Categoría", opciones, f.categoria, { filas[i] = f.copy(categoria = it) }, error = err["extras.$i.categoria"])
                        if (f.categoria == "__nueva") OutlinedTextField(f.nueva, { filas[i] = f.copy(nueva = it.take(100)) }, label = { Text("Nombre, ej. Ropa") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (filas.size > 1) TextButton(onClick = { filas.removeAt(i) }) { Text("Quitar", color = Semantica.egreso) }
                    }
                }
            }
            TextButton(onClick = { filas.add(FilaExtra()) }) { Text("+ Otra cosa") }
        }
        Desplegable("¿Con qué pagas?", d.origenes.map { it.clave to it.nombre }, origen, { origen = it }, error = err["origen"] ?: err["monto"])
        CampoFecha("Fecha", fecha, { fecha = it }, d.ciclo.inicio, minOf(Formato.hoy(), d.ciclo.fin), err["fecha"])
        Text("Se registra un gasto por cada categoría, con las reglas de siempre (saldo, disponible y presupuesto).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HojaRuta(ruta: Ruta, tiendas: List<Tienda>, onCerrar: () -> Unit, onRecalcular: (String) -> Unit, onEstoyAqui: (Int) -> Unit) {
    var costo by remember(ruta) { mutableStateOf(ruta.costoVisita) }
    HojaFormulario("Ruta de compra", "Recalcular", false, onCerrar, { onRecalcular(costo) }) {
        CampoMonto("Ir a otra tienda me cuesta", costo, { costo = it }, apoyo = "Gasolina, tiempo, parqueo… Solo conviene ir a otra tienda si te ahorras más que esto.")
        if (ruta.paradas.isEmpty()) Text("Aún no hay precios de lo que falta comprar. Consulta los precios en línea o anótalos en la Despensa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            Text(if (ruta.paradas.size == 1) "Compra todo en ${ruta.paradas[0].tienda}: ${Formato.dinero(ruta.total)}" else "Ve a ${ruta.paradas.size} tiendas: ${Formato.dinero(ruta.total)}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            when {
                ruta.paradas.size > 1 && ruta.ahorro == null -> Text("Ninguna tienda tiene precio de todo: así cubres lo más que se puede.")
                ruta.ahorro != null && ruta.unaTienda != null -> Text("Todo en ${ruta.unaTienda.tienda} saldría en ${Formato.dinero(ruta.unaTienda.total)}: te ahorras ${Formato.dinero(ruta.ahorro)}.")
                ruta.paradas.size == 1 -> Text("Ir a otra tienda no te ahorra más de lo que cuesta ir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (ruta.viejos > 0) Text("${ruta.viejos} ${if (ruta.viejos == 1) "precio tiene" else "precios tienen"} más de 7 días: puede haber cambiado.", color = Semantica.aviso, style = MaterialTheme.typography.bodySmall)
            ruta.paradas.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${p.tienda} · ${Formato.dinero(p.subtotal)}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (tiendas.any { it.id == p.tiendaId }) TextButton(onClick = { onEstoyAqui(p.tiendaId) }) { Text("Estoy aquí") }
                }
                p.items.forEach { i ->
                    Row { Text("${i.descripcion} × ${i.cantidad}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("${Formato.dinero(i.subtotal)} (${i.cuando})", style = MaterialTheme.typography.bodySmall, color = if (i.viejo) Semantica.aviso else Color.Unspecified) }
                }
                HorizontalDivider()
            }
        }
        if (ruta.sinPrecio.isNotEmpty()) {
            Text("Sin precio", fontWeight = FontWeight.SemiBold)
            ruta.sinPrecio.forEach { Text("${it.descripcion} × ${it.cantidad}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
