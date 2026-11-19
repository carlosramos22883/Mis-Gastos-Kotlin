package com.carlosramos.misgastos.ui.compras

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Compras
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Seccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComprasEstado(val cargando: Boolean = true, val error: String? = null, val compras: Compras? = null, val mensaje: String? = null, val creando: Boolean = false)

@HiltViewModel
class ComprasViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ComprasEstado())
    val estado = _estado.asStateFlow()

    fun cargar() = viewModelScope.launch {
        _estado.value = when (val r = repo.compras()) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, error = null, compras = r.datos)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun nueva(alAbrir: (Int) -> Unit) = viewModelScope.launch {
        _estado.value = _estado.value.copy(creando = true)
        when (val r = repo.nuevaLista()) {
            is Resultado.Ok -> { _estado.value = _estado.value.copy(creando = false); alAbrir(r.datos.id) }
            is Resultado.Falla -> _estado.value = _estado.value.copy(creando = false, mensaje = r.mensaje)
        }
    }

    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprasScreen(onAbrir: (Int) -> Unit, onVolver: () -> Unit, viewModel: ComprasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Lista de compras") }, windowInsets = WindowInsets(0), navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        val c = estado.compras
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            estado.error != null || c == null -> ErrorConReintento(estado.error ?: "", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    val a = c.abierta
                    if (a != null) Card(Modifier.fillMaxWidth().clickable { onAbrir(a.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(a.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${a.compradosCount ?: 0} de ${a.itemsCount ?: 0} en el carrito" + (a.resumen?.let { " · ${Formato.dinero(it.total)}" } ?: ""),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Seguir comprando →", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                        }
                    } else Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (c.faltan > 0) "En tu despensa faltan ${c.faltan} ${if (c.faltan == 1) "producto" else "productos"}: la lista nueva los trae solos." else "No falta nada en la despensa. Puedes armar la lista a mano.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { viewModel.nueva(onAbrir) }, enabled = !estado.creando, modifier = Modifier.fillMaxWidth()) { Text("Nueva lista de compras") }
                        }
                    }
                }
                if (c.historial.isNotEmpty()) item {
                    Seccion("Historial") {
                        c.historial.forEach { l ->
                            Row(Modifier.fillMaxWidth().clickable { onAbrir(l.id) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(l.nombre)
                                    Text("${l.compradosCount ?: 0} de ${l.itemsCount ?: 0} comprados · ${Formato.fechaCorta(l.terminadaAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
