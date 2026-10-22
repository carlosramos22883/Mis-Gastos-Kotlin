package com.carlosramos.misgastos.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Categoria
import com.carlosramos.misgastos.data.remote.dto.CategoriaBody
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.altas.FilaInterruptor
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.HojaFormulario
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.SelectorColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormCategoria(
    val id: Int? = null,
    val nombre: String = "",
    val tipo: String = "egreso",
    val color: String = "#2563EB",
    val activo: Boolean = true,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class CategoriasEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val categorias: List<Categoria> = emptyList(),
    val tipo: String = "egreso",
    val mensaje: String? = null
)

@HiltViewModel
class CategoriasViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _e = MutableStateFlow(CategoriasEstado())
    val estado = _e.asStateFlow()
    private val _form = MutableStateFlow<FormCategoria?>(null)
    val form = _form.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _e.value = when (val r = repo.categorias()) {
            is Resultado.Ok -> _e.value.copy(cargando = false, categorias = r.datos.sortedBy { it.nombre }, error = null)
            is Resultado.Falla -> _e.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun tipo(t: String) { _e.value = _e.value.copy(tipo = t) }
    fun mensajeVisto() { _e.value = _e.value.copy(mensaje = null) }
    fun nueva() { _form.value = FormCategoria(tipo = _e.value.tipo) }
    fun editar(c: Categoria) {
        if (!c.editable) { _e.value = _e.value.copy(mensaje = "«${c.nombre}» la usa el sistema y no se modifica."); return }
        _form.value = FormCategoria(c.id, c.nombre, c.tipo, c.color ?: "#2563EB", c.activo)
    }
    fun cambiar(f: FormCategoria) { _form.value = f }
    fun cerrar() { _form.value = null }

    fun guardar() {
        val f = _form.value ?: return
        if (f.nombre.isBlank()) { _form.value = f.copy(errores = mapOf("nombre" to "Escribe el nombre.")); return }
        _form.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            when (val r = repo.guardarCategoria(f.id, CategoriaBody(f.nombre.trim(), f.tipo, f.color, f.activo))) {
                is Resultado.Ok -> { _form.value = null; _e.value = _e.value.copy(mensaje = if (f.id == null) "Categoría creada." else "Categoría actualizada."); cargar() }
                is Resultado.Falla -> _form.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() {
        val f = _form.value ?: return
        val id = f.id ?: return
        _form.value = f.copy(guardando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.eliminarCategoria(id)) {
                is Resultado.Ok -> { _form.value = null; _e.value = _e.value.copy(mensaje = "Categoría eliminada."); cargar() }
                is Resultado.Falla -> _form.value = f.copy(guardando = false, error = r.mensaje)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(onVolver: () -> Unit, viewModel: CategoriasViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val aviso = remember { SnackbarHostState() }
    var borrar by remember { mutableStateOf(false) }
    LaunchedEffect(e.mensaje) { e.mensaje?.let { aviso.showSnackbar(it); viewModel.mensajeVisto() } }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Categorías") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { IconButton(onClick = viewModel::nueva) { Icon(Icons.Default.Add, contentDescription = "Nueva categoría") } }
            )
        },
        snackbarHost = { SnackbarHost(aviso) }
    ) { relleno ->
        when {
            e.cargando -> Cargando(Modifier.padding(relleno))
            e.error != null && e.categorias.isEmpty() -> ErrorConReintento(e.error!!, { viewModel.cargar() }, Modifier.padding(relleno))
            else -> Column(Modifier.padding(relleno).fillMaxSize()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("egreso" to "Gastos", "ingreso" to "Ingresos").forEach { (t, texto) ->
                        FilterChip(selected = e.tipo == t, onClick = { viewModel.tipo(t) }, label = { Text("$texto (${e.categorias.count { it.tipo == t }})") })
                    }
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(e.categorias.filter { it.tipo == e.tipo }, key = { it.id }) { c ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.editar(c) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(18.dp).clip(CircleShape).background(Formato.color(c.color)))
                            Spacer(Modifier.width(14.dp))
                            Text(c.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (!c.activo) Insignia("Inactiva", MaterialTheme.colorScheme.outline)
                            if (!c.editable) Icon(Icons.Default.Lock, contentDescription = "De sistema", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
    form?.let { f ->
        HojaFormulario(
            titulo = if (f.id == null) "Nueva categoría" else "Editar categoría", accion = "Guardar", guardando = f.guardando,
            onCerrar = viewModel::cerrar, onAccion = viewModel::guardar, error = f.error?.takeIf { f.errores.isEmpty() },
            extra = if (f.id != null) ({ OutlinedButton(onClick = { borrar = true }, enabled = !f.guardando) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } }) else null
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("egreso" to "Gasto", "ingreso" to "Ingreso").forEachIndexed { i, (t, texto) ->
                    SegmentedButton(selected = f.tipo == t, onClick = { viewModel.cambiar(f.copy(tipo = t)) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(texto) }
                }
            }
            OutlinedTextField(f.nombre, { viewModel.cambiar(f.copy(nombre = it)) }, label = { Text("Nombre") }, singleLine = true,
                isError = f.errores["nombre"] != null, supportingText = f.errores["nombre"]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
            SelectorColor(f.color) { viewModel.cambiar(f.copy(color = it)) }
            if (f.id != null) FilaInterruptor("Activa", f.activo, { viewModel.cambiar(f.copy(activo = it)) }, "Una inactiva ya no aparece al registrar movimientos")
        }
    }
    if (borrar) Confirmar("¿Eliminar esta categoría?", "Solo si no tiene movimientos; si los tiene, desactívala.", "Eliminar",
        onConfirmar = { borrar = false; viewModel.eliminar() }, onCancelar = { borrar = false })
}
