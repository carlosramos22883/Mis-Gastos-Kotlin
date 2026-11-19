package com.carlosramos.misgastos.ui.despensa

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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Producto
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Escaner
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** todos | faltan | archivados */
data class DespensaEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val productos: List<Producto> = emptyList(),
    val pestana: String = "todos",
    val faltan: Int = 0,
    val porVencer: Int = 0,
    val mensaje: String? = null,
    val enviando: Int? = null
)

@HiltViewModel
class DespensaViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(DespensaEstado())
    val estado = _estado.asStateFlow()

    fun cargar() = viewModelScope.launch {
        val filtros = when (_estado.value.pestana) { "faltan" -> mapOf("faltan" to "1"); "archivados" -> mapOf("archivados" to "1"); else -> emptyMap() }
        _estado.value = when (val r = repo.productos(filtros)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, error = null, productos = r.datos.data, faltan = r.datos.meta.resumen.faltan, porVencer = r.datos.meta.resumen.porVencer)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun pestana(p: String) { _estado.value = _estado.value.copy(pestana = p, cargando = true, productos = emptyList()); cargar() }

    /** sumar | restar | se_acabo | por_acabarse (valor 1/0) */
    fun ajustar(p: Producto, accion: String, valor: String? = null) = viewModelScope.launch {
        _estado.value = _estado.value.copy(enviando = p.id)
        when (val r = repo.ajustarDespensa(p.id, accion, valor)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(enviando = null, productos = _estado.value.productos.map { if (it.id == p.id) it.copy(estado = r.datos.estado) else it })
            is Resultado.Falla -> _estado.value = _estado.value.copy(enviando = null, mensaje = r.mensaje)
        }
    }

    /** Al escanear: si el código está en la despensa, su id; si no, null (crearlo con ese código). */
    fun buscarCodigo(codigo: String, alEncontrar: (Int?) -> Unit) = viewModelScope.launch {
        when (val r = repo.productos(mapOf("buscar" to codigo, "archivados" to "0"))) {
            is Resultado.Ok -> alEncontrar(r.datos.data.firstOrNull { it.codigoBarras == codigo }?.id)
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }

    fun avisar(texto: String) { _estado.value = _estado.value.copy(mensaje = texto) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespensaScreen(
    onAbrir: (Int) -> Unit,
    onNuevo: (String?) -> Unit,
    onLista: () -> Unit,
    onTiendas: () -> Unit,
    onVolver: () -> Unit,
    viewModel: DespensaViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var busqueda by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    val lista = estado.productos.filter { busqueda.isBlank() || it.nombreCompleto.contains(busqueda.trim(), true) || it.codigoBarras?.contains(busqueda.trim()) == true }
    val escanear = { Escaner.escanear(contexto, { codigo -> viewModel.buscarCodigo(codigo) { id -> if (id != null) onAbrir(id) else onNuevo(codigo) } }, { viewModel.avisar(it) }) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Despensa") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    IconButton(onClick = escanear) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear código de barras") }
                    IconButton(onClick = onTiendas) { Icon(Icons.Default.Store, contentDescription = "Tiendas") }
                    IconButton(onClick = onLista) { Icon(Icons.Default.ShoppingCart, contentDescription = "Lista de compras") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = { onNuevo(null) }) { Icon(Icons.Default.Add, contentDescription = "Nuevo producto") } }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = busqueda, onValueChange = { busqueda = it }, placeholder = { Text("Buscar por producto, marca o código") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = estado.pestana == "todos", onClick = { viewModel.pestana("todos") }, label = { Text("Todos") })
                    FilterChip(selected = estado.pestana == "faltan", onClick = { viewModel.pestana("faltan") }, label = { Text(if (estado.faltan > 0) "Faltan (${estado.faltan})" else "Faltan") })
                    FilterChip(selected = estado.pestana == "archivados", onClick = { viewModel.pestana("archivados") }, label = { Text("Ya no los compro") })
                }
                if (estado.porVencer > 0 && estado.pestana != "archivados") {
                    Text(if (estado.porVencer == 1) "Un producto vence pronto o ya venció." else "${estado.porVencer} productos vencen pronto o ya vencieron.",
                        color = Semantica.aviso, style = MaterialTheme.typography.bodySmall)
                }
            }
            when {
                estado.cargando -> Cargando()
                estado.error != null && estado.productos.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() })
                lista.isEmpty() -> Vacio(when {
                    busqueda.isNotBlank() -> "Ningún producto coincide con la búsqueda."
                    estado.pestana == "faltan" -> "No falta nada. La despensa está completa."
                    estado.pestana == "archivados" -> "No hay productos archivados."
                    else -> "Aún no tienes productos. Agrega lo que compras siempre o escanea su código de barras."
                })
                else -> LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(lista, key = { it.id }) { p -> TarjetaProducto(p, estado.enviando == p.id, { onAbrir(p.id) }, { accion, valor -> viewModel.ajustar(p, accion, valor) }) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaProducto(p: Producto, enviando: Boolean, onAbrir: () -> Unit, ajustar: (String, String?) -> Unit) {
    val e = p.estado
    Card(Modifier.fillMaxWidth().clickable(onClick = onAbrir)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(p.fotoUrl, p.nombre, tamano = 56.dp, cuadrado = true)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(p.marca, p.presentacion).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin marca" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (p.basico) Insignia("Básico", MaterialTheme.colorScheme.primary)
                        if (e?.falta == true) Insignia("Falta · comprar ${e.porComprar}", Semantica.egreso)
                        e?.vencimiento?.let { if (it.aviso) Insignia(it.texto, Semantica.aviso) }
                    }
                }
            }
            if (p.activo && e != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("En casa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalIconButton(onClick = { ajustar("restar", null) }, enabled = !enviando && (e.enCasa ?: 0) > 0, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Remove, contentDescription = "Uno menos") }
                    Text(e.enCasa?.toString() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FilledTonalIconButton(onClick = { ajustar("sumar", null) }, enabled = !enviando, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, contentDescription = "Uno más") }
                    p.cantidadIdeal?.let { Text("de $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { ajustar("por_acabarse", if (e.porAcabarse) "0" else "1") }, enabled = !enviando,
                        label = { Text(if (e.porAcabarse) "Se está acabando ✓" else "Se está acabando") })
                    if (e.enCasa != 0) TextButton(onClick = { ajustar("se_acabo", null) }, enabled = !enviando) { Text("Se acabó", color = Semantica.egreso) }
                }
            }
        }
    }
}
