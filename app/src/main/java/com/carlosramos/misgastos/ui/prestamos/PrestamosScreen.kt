package com.carlosramos.misgastos.ui.prestamos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Prestamo
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrestamosEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val prestamos: List<Prestamo> = emptyList(),
    val miembros: Map<Int, String> = emptyMap()
)

@HiltViewModel
class PrestamosViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(PrestamosEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        val miembros = (repo.comunes() as? Resultado.Ok)?.datos?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }
        _estado.value = when (val r = repo.prestamos()) {
            is Resultado.Ok -> PrestamosEstado(cargando = false, prestamos = r.datos, miembros = miembros)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }
}

/** Cómo se paga: "planilla" (descuento), "efectivo", "transferencia", "por_definir"… */
fun medioPago(medio: String?): String = when (medio) {
    "planilla" -> "Descuento de planilla"
    "efectivo" -> "En ventanilla / efectivo"
    "transferencia" -> "Transferencia"
    "debito" -> "Débito automático"
    "por_definir", null -> "Por definir"
    else -> medio.replaceFirstChar { it.uppercase() }
}

private fun String?.num() = this?.toDoubleOrNull() ?: 0.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrestamosScreen(onAbrir: (Int) -> Unit, onVolver: () -> Unit, viewModel: PrestamosViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (estado.prestamos.isNotEmpty()) viewModel.cargar() }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Préstamos") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }
            )
        }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            estado.error != null && estado.prestamos.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() }, Modifier.padding(relleno))
            estado.prestamos.isEmpty() -> Vacio("No hay préstamos registrados.", Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    val activos = estado.prestamos.filter { it.activo }
                    Seccion("Resumen") {
                        FilaValor("Saldo total", Formato.dinero(activos.sumOf { it.saldo.num() }), negrita = true)
                        FilaValor("Cuotas al mes (con seguros)", Formato.dinero(activos.sumOf { it.cuotaTotal.num() }))
                        FilaValor("Intereses que faltan por pagar", Formato.dinero(activos.sumOf { it.interesesRestantes.num() }))
                    }
                }
                items(estado.prestamos, key = { it.id }) { p -> TarjetaPrestamo(p, estado.miembros[p.userId]) { onAbrir(p.id) } }
            }
        }
    }
}

@Composable
private fun TarjetaPrestamo(p: Prestamo, titular: String?, onClick: () -> Unit) {
    val color = Formato.color(p.color, MaterialTheme.colorScheme.primary)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row {
            Box(Modifier.width(6.dp).height(150.dp).background(color))
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(p.acreedor, p.tipo, titular).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!p.activo) Insignia("Pagado", MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Saldo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Formato.dinero(p.saldo), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    p.cuotaTotal?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cuota", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formato.dinero(it), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                p.avance?.let { a ->
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { (a / 100).toFloat().coerceIn(0f, 1f) }, color = color, strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                    Text(
                        "${"%.1f".format(a)} % pagado" + (p.proximaFecha?.let { " · próximo pago ${Formato.fechaCorta(it)}" } ?: "") + (p.fechaFin?.let { " · termina ${Formato.fechaCorta(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
