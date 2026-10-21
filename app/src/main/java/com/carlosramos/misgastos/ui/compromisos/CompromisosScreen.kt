package com.carlosramos.misgastos.ui.compromisos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.CicloSimple
import com.carlosramos.misgastos.data.remote.dto.Compromiso
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompromisosEstado(
    val cargando: Boolean = true,
    val cargandoMas: Boolean = false,
    val error: String? = null,
    val lista: List<Compromiso> = emptyList(),
    val ciclo: CicloSimple? = null,
    val pagina: Int = 1,
    val hayMas: Boolean = false,
    val periodo: String = "actual",
    val estadoFiltro: String? = null
)

@HiltViewModel
class CompromisosViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(CompromisosEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        val e = _estado.value
        _estado.value = e.copy(cargando = e.lista.isEmpty(), error = null)
        _estado.value = when (val r = repo.compromisos(e.periodo, e.estadoFiltro, 1)) {
            is Resultado.Ok -> _estado.value.copy(
                cargando = false, lista = r.datos.data, ciclo = r.datos.ciclo, pagina = 1,
                hayMas = r.datos.meta.paginaActual < r.datos.meta.ultimaPagina
            )
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun cargarMas() {
        val e = _estado.value
        if (!e.hayMas || e.cargandoMas) return
        _estado.value = e.copy(cargandoMas = true)
        viewModelScope.launch {
            _estado.value = when (val r = repo.compromisos(e.periodo, e.estadoFiltro, e.pagina + 1)) {
                is Resultado.Ok -> _estado.value.copy(
                    cargandoMas = false, lista = _estado.value.lista + r.datos.data, pagina = r.datos.meta.paginaActual,
                    hayMas = r.datos.meta.paginaActual < r.datos.meta.ultimaPagina
                )
                is Resultado.Falla -> _estado.value.copy(cargandoMas = false)
            }
        }
    }

    fun periodo(p: String) { _estado.value = _estado.value.copy(periodo = p, lista = emptyList()); cargar() }
    fun estado(e: String?) { _estado.value = _estado.value.copy(estadoFiltro = e, lista = emptyList()); cargar() }
}

/** Color y texto de cada estado visual (siempre con su etiqueta). */
@Composable
fun colorEstado(estadoVisual: String): Color = when (estadoVisual) {
    "pagado" -> Semantica.ingreso
    "vencido", "antiguo" -> Semantica.egreso
    "parcial" -> Semantica.aviso
    "cancelado" -> MaterialTheme.colorScheme.outline
    else -> Semantica.info
}

@Composable
fun CompromisosScreen(onAbrir: (Int) -> Unit, viewModel: CompromisosViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Al volver del detalle (quizá se pagó algo) se refresca la lista
    LaunchedEffect(Unit) { if (estado.lista.isNotEmpty()) viewModel.cargar() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Compromisos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            estado.ciclo?.takeIf { estado.periodo == "actual" }?.let {
                Text("Ciclo ${Formato.rango(it.inicio, it.fin)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("actual" to "Este ciclo", "proximos" to "Próximos", "todos" to "Todos").forEach { (p, t) ->
                    FilterChip(selected = estado.periodo == p, onClick = { viewModel.periodo(p) }, label = { Text(t) })
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Cualquier estado", "pendiente" to "Pendientes", "vencido" to "Vencidos", "pagado" to "Pagados", "cancelado" to "Cancelados").forEach { (e, t) ->
                    FilterChip(selected = estado.estadoFiltro == e, onClick = { viewModel.estado(e) }, label = { Text(t) })
                }
            }
        }
        when {
            estado.cargando -> Cargando()
            estado.error != null && estado.lista.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() })
            estado.lista.isEmpty() -> Vacio("No hay compromisos con estos filtros.")
            else -> {
                val lista = rememberLazyListState()
                val alFinal by remember { derivedStateOf { lista.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lista.layoutInfo.totalItemsCount - 1 } }
                LaunchedEffect(alFinal) { if (alFinal) viewModel.cargarMas() }
                LazyColumn(state = lista, contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(estado.lista, key = { it.id }) { c -> FilaCompromiso(c) { onAbrir(c.id) } }
                    if (estado.cargandoMas) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
                }
            }
        }
    }
}

@Composable
private fun FilaCompromiso(c: Compromiso, onClick: () -> Unit) {
    val color = colorEstado(c.estadoVisual)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 40.dp).clip(RoundedCornerShape(2.dp)).background(Formato.color(c.tarjeta?.color ?: c.categoria?.color, MaterialTheme.colorScheme.outlineVariant)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.descripcion, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(
                        c.fechaEsperada?.let { Formato.fechaCorta(it) } ?: "Sin fecha",
                        c.numeroCuota?.let { n -> "cuota $n" + (c.totalCuotas?.let { "/$it" } ?: "") }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Insignia(c.estadoTexto, color)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formato.dinero(if (c.estado == "pendiente") c.saldoPendiente else c.monto), fontWeight = FontWeight.SemiBold)
            if (c.estado == "pendiente" && c.saldoPendiente != c.monto) {
                Text("de ${Formato.dinero(c.monto)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider(Modifier.padding(start = 32.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
