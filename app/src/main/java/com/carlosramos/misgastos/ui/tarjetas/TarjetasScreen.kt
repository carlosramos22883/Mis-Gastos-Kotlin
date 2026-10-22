package com.carlosramos.misgastos.ui.tarjetas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Tarjeta
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TarjetasEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val tarjetas: List<Tarjeta> = emptyList(),
    val miembros: Map<Int, String> = emptyMap(),
    val fotos: Map<Int, String?> = emptyMap(),
    val orden: String = "nombre"
)

/** Formas de ordenar (las mismas de la web). */
val ORDENES_TARJETA = listOf(
    "disponible" to "Más disponible", "deuda" to "Más endeudada", "uso" to "Más usada (% del límite)",
    "corte" to "Corte más próximo", "pago" to "Pago más próximo", "limite" to "Límite más alto", "nombre" to "Nombre"
)

@HiltViewModel
class TarjetasViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(TarjetasEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        val lista = (repo.comunes() as? Resultado.Ok)?.datos?.miembros.orEmpty()
        val miembros = lista.associate { it.id to it.nombre.substringBefore(" ") }
        _estado.value = when (val r = repo.tarjetas()) {
            is Resultado.Ok -> TarjetasEstado(cargando = false, tarjetas = r.datos, miembros = miembros, fotos = lista.associate { it.id to Formato.urlAvatar(it.avatar) }, orden = _estado.value.orden)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun ordenar(orden: String) { _estado.value = _estado.value.copy(orden = orden) }
}

private fun String.num() = toDoubleOrNull() ?: 0.0

/** Activas primero; dentro, según el orden elegido; empata por nombre. */
private fun List<Tarjeta>.ordenadas(orden: String): List<Tarjeta> = sortedWith(
    compareBy<Tarjeta> { !it.activo }.then(
        when (orden) {
            "disponible" -> compareByDescending { it.disponible.num() }
            "deuda" -> compareByDescending { it.deuda.num() }
            "uso" -> compareByDescending { it.deuda.num() / it.limite.num().coerceAtLeast(1.0) }
            "corte" -> compareBy { it.proximoCorte ?: "9999" }
            "pago" -> compareBy { it.porPagar?.fecha ?: "9999" }
            "limite" -> compareByDescending { it.limite.num() }
            else -> compareBy { it.nombre.lowercase() }
        }
    ).thenBy { it.nombre.lowercase() }
)

@Composable
fun TarjetasScreen(onAbrir: (Int) -> Unit, onNueva: () -> Unit, viewModel: TarjetasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (estado.tarjetas.isNotEmpty()) viewModel.cargar() }
    when {
        estado.cargando -> Cargando()
        estado.error != null && estado.tarjetas.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() })
        estado.tarjetas.isEmpty() -> Column { Vacio("Aún no hay tarjetas."); TextButton(onClick = onNueva, modifier = Modifier.fillMaxWidth()) { Text("Agregar tarjeta") } }
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tarjetas de crédito", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onNueva) { Icon(Icons.Default.Add, contentDescription = "Nueva tarjeta") }
                }
            }
            item {
                val activas = estado.tarjetas.filter { it.activo }
                Seccion("Resumen") {
                    FilaValor("Disponible", Formato.dinero(activas.sumOf { it.disponible.num() }), negrita = true)
                    FilaValor("Deuda total", Formato.dinero(activas.sumOf { it.deuda.num() }))
                    FilaValor("Por pagar (estados de cuenta)", Formato.dinero(activas.sumOf { it.porPagar?.monto?.num() ?: 0.0 }))
                }
            }
            item {
                com.carlosramos.misgastos.ui.common.Desplegable("Ordenar por", ORDENES_TARJETA, estado.orden, viewModel::ordenar)
            }
            items(estado.tarjetas.ordenadas(estado.orden), key = { it.id }) { t ->
                TarjetaVisual(t, estado.miembros[t.userId], foto = estado.fotos[t.userId], mostrarFoto = estado.miembros.size > 1) { onAbrir(t.id) }
            }
        }
    }
}

/** La tarjeta con su color; el texto usa el color de contraste que calcula el servidor. */
@Composable
fun TarjetaVisual(t: Tarjeta, titular: String?, foto: String? = null, mostrarFoto: Boolean = false, onClick: (() -> Unit)? = null) {
    val fondo = Formato.color(t.color, MaterialTheme.colorScheme.secondary)
    val texto = Formato.color(t.colorTexto, Formato.textoSobre(fondo))
    val uso = (t.deuda.num() / t.limite.num().coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(fondo)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(t.banco?.logoUrl, t.banco?.nombre ?: t.nombre, Color.White, tamano = 32.dp, cuadrado = true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.nombre, color = texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(titular, t.ultimosDigitos?.let { "•••• $it" }, if (!t.activo) "Inactiva" else null).joinToString(" · "), color = texto.copy(alpha = 0.85f), fontSize = 12.sp)
            }
            t.marca?.logoUrl?.let { Avatar(it, t.marca.nombre, Color.White, tamano = 28.dp, cuadrado = true) }
            // Foto del titular en la esquina: distingue tarjetas parecidas de cada miembro
            if (mostrarFoto && titular != null) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(Color.White).padding(2.dp)) {
                    Avatar(Formato.urlImagen(foto) ?: foto, titular, tamano = 34.dp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Disponible", color = texto.copy(alpha = 0.85f), fontSize = 12.sp)
        Text(Formato.dinero(t.disponible), color = texto, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { uso }, modifier = Modifier.fillMaxWidth().height(6.dp), color = texto, trackColor = texto.copy(alpha = 0.25f), strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(4.dp))
        Text("Deuda ${Formato.dinero(t.deuda)} de ${Formato.dinero(t.limite)}", color = texto.copy(alpha = 0.9f), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(texto.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                t.porPagar?.let { "Pagar ${Formato.dinero(it.monto)} antes del ${Formato.fechaCorta(it.fecha)}" }
                    ?: "Sin pagos pendientes · corta el ${Formato.fechaCorta(t.proximoCorte)}",
                color = texto, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
        }
    }
}
