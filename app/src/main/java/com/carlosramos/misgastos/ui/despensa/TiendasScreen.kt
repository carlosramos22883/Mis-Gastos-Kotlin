package com.carlosramos.misgastos.ui.despensa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Tienda
import com.carlosramos.misgastos.data.remote.dto.TiendaBody
import com.carlosramos.misgastos.data.remote.dto.ValoresDespensa
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.HojaFormulario
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Semantica
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TiendasEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val tiendas: List<Tienda> = emptyList(),
    val valores: ValoresDespensa? = null,
    val mensaje: String? = null,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap()
)

@HiltViewModel
class TiendasViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(TiendasEstado())
    val estado = _estado.asStateFlow()

    fun cargar() = viewModelScope.launch {
        val valores = (repo.catalogos() as? Resultado.Ok)?.datos?.despensa
        _estado.value = when (val r = repo.tiendas()) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, error = null, tiendas = r.datos, valores = valores ?: _estado.value.valores)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun comunes() = viewModelScope.launch {
        when (val r = repo.tiendasComunes()) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(tiendas = r.datos.data, mensaje = when (r.datos.agregadas) {
                0 -> "Ya tienes todas las tiendas comunes."; 1 -> "Se agregó 1 tienda."; else -> "Se agregaron ${r.datos.agregadas} tiendas." })
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }

    fun guardar(id: Int?, body: TiendaBody, alTerminar: () -> Unit) = viewModelScope.launch {
        _estado.value = _estado.value.copy(guardando = true, errores = emptyMap())
        when (val r = repo.guardarTienda(id, body)) {
            is Resultado.Ok -> { _estado.value = _estado.value.copy(guardando = false); alTerminar(); cargar() }
            is Resultado.Falla -> _estado.value = _estado.value.copy(guardando = false, errores = r.campos, mensaje = r.mensaje)
        }
    }

    fun eliminar(t: Tienda) = viewModelScope.launch {
        when (val r = repo.eliminarTienda(t.id)) {
            is Resultado.Ok -> cargar()
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = r.mensaje)
        }
    }

    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendasScreen(onVolver: () -> Unit, viewModel: TiendasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editando by remember { mutableStateOf<Tienda?>(null) }
    var nueva by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf<Tienda?>(null) }
    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Tiendas") }, windowInsets = WindowInsets(0), navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = { nueva = true }) { Icon(Icons.Default.Add, contentDescription = "Nueva tienda") } }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            OutlinedButton(onClick = { viewModel.comunes() }, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) { Text("Agregar las de siempre") }
            when {
                estado.cargando -> Cargando()
                estado.error != null -> ErrorConReintento(estado.error!!, { viewModel.cargar() })
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(estado.tiendas, key = { it.id }) { t ->
                        Row(Modifier.fillMaxWidth().clickable { editando = t }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.nombre, fontWeight = FontWeight.SemiBold)
                                Text(if (t.fuente != null) "Precios en línea" else "Precio anotado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!t.activo) Insignia("Inactiva", MaterialTheme.colorScheme.outline)
                            if ((t.preciosCount ?: 0) == 0) TextButton(onClick = { eliminar = t }) { Text("Eliminar", color = Semantica.egreso) }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (nueva || editando != null) {
        val t = editando
        var nombre by remember(t) { mutableStateOf(t?.nombre.orEmpty()) }
        var fuente by remember(t) { mutableStateOf(t?.fuente ?: "") }
        var notas by remember(t) { mutableStateOf(t?.notas.orEmpty()) }
        var activo by remember(t) { mutableStateOf(t?.activo ?: true) }
        val cerrar = { nueva = false; editando = null }
        HojaFormulario(if (t == null) "Nueva tienda" else "Editar tienda", "Guardar", estado.guardando, cerrar, {
            viewModel.guardar(t?.id, TiendaBody(nombre.trim(), fuente.ifBlank { null }, notas.trim().ifBlank { null }, activo), cerrar)
        }) {
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, isError = estado.errores["nombre"] != null,
                supportingText = estado.errores["nombre"]?.let { { Text(it) } }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Desplegable("Precios en línea", listOf("" to "No (precio anotado)") + estado.valores?.fuentes?.toList().orEmpty(), fuente, { fuente = it })
            OutlinedTextField(notas, { notas = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            if (t != null) Row(verticalAlignment = Alignment.CenterVertically) { Text("Activa", Modifier.weight(1f)); Switch(activo, { activo = it }) }
            if (t != null && (t.preciosCount ?: 0) > 0) Text("Tiene precios o compras: no se elimina, se desactiva.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    eliminar?.let { t -> Confirmar("¿Eliminar ${t.nombre}?", "Se quita de tus tiendas.", "Eliminar", { eliminar = null; viewModel.eliminar(t) }, { eliminar = null }) }
}

