package com.carlosramos.misgastos.ui.proyecciones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.ComparacionEscenario
import com.carlosramos.misgastos.data.remote.dto.ProyeccionDeudas
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Escenarios que se comparan a la vez con "Si sigo igual" (igual que la web). */
private const val MAX_COMPARAR = 3

data class ProyeccionesEstado(val cargando: Boolean = true, val error: String? = null, val datos: ProyeccionDeudas? = null)

@HiltViewModel
class ProyeccionesViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ProyeccionesEstado())
    val estado = _estado.asStateFlow()

    init { cargar(null) }

    fun cargar(ver: List<Int>?) = viewModelScope.launch {
        _estado.value = _estado.value.copy(cargando = _estado.value.datos == null, error = null)
        _estado.value = when (val r = repo.proyecciones(ver)) {
            is Resultado.Ok -> ProyeccionesEstado(cargando = false, datos = r.datos)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    /** Agrega o quita un escenario de la comparación (hasta MAX_COMPARAR). */
    fun alternar(id: Int) {
        val actual = _estado.value.datos?.ver.orEmpty()
        cargar(if (id in actual) actual - id else (actual + id).takeLast(MAX_COMPARAR))
    }
}

private val mesesCortos = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
private fun mesTexto(m: String?): String = m?.let { mesesCortos[it.substring(5, 7).toInt() - 1] + " " + it.substring(0, 4) } ?: "Más de 15 años"

/** Paleta categórica validada (claro / oscuro), en orden fijo: "Si sigo igual" siempre es la primera. */
@Composable
private fun colorSerie(i: Int): Color {
    val oscuro = isSystemInDarkTheme()
    val claro = listOf(0xFF2A78D6, 0xFFEB6834, 0xFF1BAF7A, 0xFFEDA100)
    val noche = listOf(0xFF3987E5, 0xFFD95926, 0xFF199E70, 0xFFC98500)
    return Color((if (oscuro) noche else claro)[i % 4])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProyeccionesScreen(onVolver: () -> Unit, viewModel: ProyeccionesViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val d = estado.datos
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(title = { Text("Proyecciones") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } })
        }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            d == null -> ErrorConReintento(estado.error ?: "No se pudo cargar.", { viewModel.cargar(null) }, Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("¿Cuándo salgo de mis deudas? Con tus datos reales desde ${mesTexto(d.base.mesInicio)}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Seccion("Tu situación hoy") {
                        FilaValor("Entra al mes", Formato.dinero(d.base.ingreso))
                        FilaValor(if (d.base.gastoOrigen == "tope") "Gasto de vida (el tope)" else "Gasto de vida (promedio)", Formato.dinero(d.base.gasto))
                        FilaValor("Cuotas en efectivo", Formato.dinero(d.base.cuotasEfectivo))
                        val sobra = d.base.sobra.toDoubleOrNull() ?: 0.0
                        FilaValor("Sobra para deudas", Formato.dinero(sobra), colorValor = if (sobra < 0) MaterialTheme.colorScheme.error else Color.Unspecified, negrita = true)
                        Spacer(Modifier.height(6.dp))
                        FilaValor("Tarjetas", Formato.dinero(d.base.grupos.tarjetas))
                        FilaValor("Préstamos del banco", Formato.dinero(d.base.grupos.bancos))
                        FilaValor("Personas", Formato.dinero(d.base.grupos.personas))
                        FilaValor("Hipoteca (va aparte)", Formato.dinero(d.base.grupos.hipoteca))
                    }
                }
                item {
                    Seccion("Comparar con «Si sigo igual»") {
                        if (d.escenarios.isEmpty()) Text("Aún no tienes escenarios. Créalos en la web, en Proyecciones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else {
                            Text("Elige hasta $MAX_COMPARAR. Los escenarios se crean y editan en la web.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                d.escenarios.forEach { e -> FilterChip(selected = e.id in d.ver, onClick = { viewModel.alternar(e.id) }, label = { Text(e.nombre) }) }
                            }
                        }
                    }
                }
                items(d.comparacion.size) { i -> TarjetaEscenario(d.comparacion[i], colorSerie(i)) }
                item { Grafica(d.comparacion) }
                item {
                    Seccion("Cuándo queda en cero cada deuda") {
                        d.comparacion.first().deudas.forEach { deuda ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(deuda.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                d.comparacion.forEachIndexed { i, e ->
                                    val cero = e.deudas.firstOrNull { it.clave == deuda.clave }?.cero
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(width = 12.dp, height = 2.dp).background(colorSerie(i)))
                                        Spacer(Modifier.width(6.dp))
                                        Text(e.nombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                        Text(mesTexto(cero), style = MaterialTheme.typography.bodySmall, color = if (cero == null) MaterialTheme.colorScheme.error else Color.Unspecified)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Es una proyección con supuestos: el ingreso y el gasto se mantienen, lo que sobra paga deudas según el orden del escenario y lo que falta se va a las tarjetas.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TarjetaEscenario(e: ComparacionEscenario, color: Color) {
    val ordenes = mapOf("mas_cara" to "la más cara primero", "mas_pequena" to "la más pequeña primero", "personalizado" to "orden propio")
    Seccion(e.nombre) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 16.dp, height = 3.dp).background(color))
            Spacer(Modifier.width(8.dp))
            Text((if (e.modoTarjetas == "rotar") "Tarjetas rotando con avances" else "Tarjetas pagando intereses") + " · " + (ordenes[e.orden] ?: e.orden),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        val fecha = @Composable { etiqueta: String, mes: String? ->
            FilaValor(etiqueta, mesTexto(mes), colorValor = if (mes == null) MaterialTheme.colorScheme.error else Color.Unspecified, negrita = true)
        }
        fecha("Tarjetas en $0", e.hitos.tarjetas)
        fecha("Personas pagadas", e.hitos.personas)
        fecha("Sin deudas (menos la hipoteca)", e.hitos.sinDeudas)
        FilaValor("Intereses de tarjetas", Formato.dinero(e.totales.interesesTarjetas))
        if ((e.totales.perdidaRotar.toDoubleOrNull() ?: 0.0) > 0) FilaValor("Perdido al rotar", Formato.dinero(e.totales.perdidaRotar))
        if ((e.totales.prestamosNuevos.toDoubleOrNull() ?: 0.0) > 0) FilaValor("Préstamos nuevos: intereses y comisiones", Formato.dinero(e.totales.prestamosNuevos))
        if (e.totales.mesesConFaltante > 0) FilaValor("No alcanzó en ${e.totales.mesesConFaltante} mes(es)", Formato.dinero(e.totales.faltante), colorValor = MaterialTheme.colorScheme.error)
        e.meta?.let { m ->
            val que = mapOf("tarjetas" to "Tarjetas en $0", "personas" to "Personas pagadas", "sin_deudas" to "Sin deudas")[m.que]
                ?: ("Pagar " + (e.deudas.firstOrNull { it.clave == m.que }?.nombre ?: m.que))
            Spacer(Modifier.height(4.dp))
            Text("Meta: $que para ${mesTexto(m.mes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when {
                    m.extra == null -> "No se logra ni con $ 20,000.00 más al mes"
                    (m.extra.toDoubleOrNull() ?: 0.0) == 0.0 -> "Ya lo logras (${mesTexto(m.logradoEn)})"
                    else -> "Necesitas ${Formato.dinero(m.extra)} más al mes"
                },
                fontWeight = FontWeight.SemiBold,
                color = when { m.extra == null -> MaterialTheme.colorScheme.error; (m.extra.toDoubleOrNull() ?: 0.0) == 0.0 -> Semantica.ingreso; else -> Color.Unspecified }
            )
        }
    }
}

/** Deuda total sin hipoteca, mes a mes: una línea por escenario. Tocar o arrastrar muestra los valores de ese mes. */
@Composable
private fun Grafica(comparacion: List<ComparacionEscenario>) {
    val meses = comparacion.maxByOrNull { it.serie.size }?.serie?.map { it.mes }.orEmpty()
    if (meses.size < 2) return
    val series = comparacion.map { e -> e.serie.map { it.total.toDoubleOrNull() ?: 0.0 } }
    val maximo = series.flatten().maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val colores = comparacion.indices.map { colorSerie(it) }
    val rejilla = MaterialTheme.colorScheme.outlineVariant
    val cruz = MaterialTheme.colorScheme.onSurfaceVariant
    var indice by remember { mutableStateOf<Int?>(null) }

    Seccion("Cuánto debes, mes a mes (sin hipoteca)") {
        comparacion.forEachIndexed { i, e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 16.dp, height = 2.dp).background(colores[i]))
                Spacer(Modifier.width(6.dp))
                Text(e.nombre, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                indice?.let { k -> Text(series[i].getOrNull(k)?.let { Formato.dinero(it) } ?: "—", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
            }
        }
        Text(indice?.let { mesTexto(meses[it]) } ?: "Toca la gráfica para ver un mes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val ultimo = meses.size - 1
        fun indiceDe(x: Float, ancho: Float) = (x / ancho * ultimo).toInt().coerceIn(0, ultimo)
        Canvas(
            Modifier.fillMaxWidth().height(200.dp).padding(top = 8.dp)
                .semantics { contentDescription = "Gráfica de la deuda total por mes en cada escenario" }
                .pointerInput(meses.size) { detectTapGestures { indice = indiceDe(it.x, size.width.toFloat()) } }
                .pointerInput(meses.size) { detectDragGestures(onDragEnd = {}) { cambio, _ -> indice = indiceDe(cambio.position.x, size.width.toFloat()) } }
        ) {
            val x = { k: Int -> size.width * k / ultimo }
            val y = { v: Double -> (size.height * (1 - v / maximo)).toFloat() }
            listOf(0.0, 0.5, 1.0).forEach { f -> drawLine(rejilla, Offset(0f, y(maximo * f)), Offset(size.width, y(maximo * f)), strokeWidth = 1f) }
            series.forEachIndexed { i, valores ->
                val camino = Path().apply { valores.forEachIndexed { k, v -> if (k == 0) moveTo(x(k), y(v)) else lineTo(x(k), y(v)) } }
                drawPath(camino, colores[i], style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                valores.lastOrNull()?.let { drawCircle(colores[i], radius = 4.dp.toPx(), center = Offset(x(valores.size - 1), y(it))) }
            }
            indice?.let { k -> drawLine(cruz, Offset(x(k), 0f), Offset(x(k), size.height), strokeWidth = 1f) }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(mesTexto(meses.first()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(mesTexto(meses.last()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Máximo: ${Formato.dinero(maximo)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
