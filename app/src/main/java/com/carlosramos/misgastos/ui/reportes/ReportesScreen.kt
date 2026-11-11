package com.carlosramos.misgastos.ui.reportes

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.CategoriaReporte
import com.carlosramos.misgastos.data.remote.dto.CorteReporte
import com.carlosramos.misgastos.data.remote.dto.Ejecucion
import com.carlosramos.misgastos.data.remote.dto.EstadoReporte
import com.carlosramos.misgastos.data.remote.dto.GastosReporte
import com.carlosramos.misgastos.data.remote.dto.PeriodoReporte
import com.carlosramos.misgastos.data.remote.dto.PrestamosReporte
import com.carlosramos.misgastos.data.remote.dto.Tarjeta
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Los cuatro reportes (los mismos de la web). */
val REPORTES = listOf("ejecucion" to "Ejecución", "gastos" to "Gastos", "prestamos" to "Préstamos", "estado-cuenta" to "Estado de cuenta")

data class ReportesEstado(
    val reporte: String = "ejecucion",
    /** Período pedido: ciclo (una fecha dentro) o desde/hasta. */
    val ciclo: String? = null,
    val desde: String? = null,
    val hasta: String? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val periodo: PeriodoReporte? = null,
    val ejecucion: Ejecucion? = null,
    val gastos: GastosReporte? = null,
    val prestamos: PrestamosReporte? = null,
    val estado: EstadoReporte? = null,
    val tarjetas: List<Tarjeta> = emptyList(),
    val tarjetaId: Int? = null,
    val estadoId: Int? = null,
    val cortes: List<CorteReporte> = emptyList(),
    val comunes: Comunes? = null,
    val descargando: Boolean = false,
    val mensaje: String? = null,
    /** Archivo recién descargado, para abrirlo (se consume). */
    val archivo: File? = null
)

@HiltViewModel
class ReportesViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _e = MutableStateFlow(ReportesEstado())
    val estado = _e.asStateFlow()

    init {
        viewModelScope.launch { (repo.comunes() as? Resultado.Ok)?.let { _e.value = _e.value.copy(comunes = it.datos) } }
        cargar()
    }

    private fun periodo(): Map<String, String> {
        val e = _e.value
        return if (e.desde != null && e.hasta != null) mapOf("desde" to e.desde, "hasta" to e.hasta) else listOfNotNull(e.ciclo?.let { "ciclo" to it }).toMap()
    }

    fun cargar() = viewModelScope.launch {
        _e.value = _e.value.copy(cargando = true, error = null)
        val e = _e.value
        _e.value = when (e.reporte) {
            "gastos" -> when (val r = repo.reporteGastos(periodo())) {
                is Resultado.Ok -> e.copy(cargando = false, periodo = r.datos.periodo, gastos = r.datos.data)
                is Resultado.Falla -> e.copy(cargando = false, error = r.mensaje)
            }
            "prestamos" -> when (val r = repo.reportePrestamos()) {
                is Resultado.Ok -> e.copy(cargando = false, periodo = r.datos.periodo, prestamos = r.datos.data)
                is Resultado.Falla -> e.copy(cargando = false, error = r.mensaje)
            }
            "estado-cuenta" -> {
                val tarjetas = e.tarjetas.ifEmpty { (repo.tarjetas() as? Resultado.Ok)?.datos.orEmpty().sortedBy { it.nombre.lowercase() } }
                if (e.tarjetaId == null) e.copy(cargando = false, tarjetas = tarjetas, estado = null, cortes = emptyList())
                else when (val r = repo.reporteEstadoCuenta(e.tarjetaId, e.estadoId)) {
                    is Resultado.Ok -> e.copy(cargando = false, tarjetas = tarjetas, estado = r.datos.data, cortes = r.datos.cortes.orEmpty())
                    is Resultado.Falla -> e.copy(cargando = false, tarjetas = tarjetas, error = r.mensaje)
                }
            }
            else -> when (val r = repo.reporteEjecucion(periodo())) {
                is Resultado.Ok -> e.copy(cargando = false, periodo = r.datos.periodo, ejecucion = r.datos.data)
                is Resultado.Falla -> e.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun elegir(reporte: String) { if (reporte != _e.value.reporte) { _e.value = _e.value.copy(reporte = reporte); cargar() } }
    fun irACiclo(fecha: String?) { _e.value = _e.value.copy(ciclo = fecha, desde = null, hasta = null); cargar() }
    fun rango(desde: String, hasta: String) {
        if (hasta < desde) { avisar("La fecha final debe ser igual o posterior a la inicial."); return }
        _e.value = _e.value.copy(desde = desde, hasta = hasta, ciclo = null); cargar()
    }
    fun elegirTarjeta(id: Int?) { _e.value = _e.value.copy(tarjetaId = id, estadoId = null); cargar() }
    fun elegirCorte(id: Int?) { _e.value = _e.value.copy(estadoId = id); cargar() }
    fun avisar(t: String) { _e.value = _e.value.copy(mensaje = t) }
    fun mensajeVisto() { _e.value = _e.value.copy(mensaje = null) }
    fun archivoAbierto() { _e.value = _e.value.copy(archivo = null) }

    /** Descarga el reporte en PDF, XLSX o CSV a la caché para abrirlo o compartirlo. */
    fun descargar(formato: String, cache: File) = viewModelScope.launch {
        val e = _e.value
        val parametros = buildMap {
            putAll(periodo()); put("formato", formato)
            e.tarjetaId?.let { put("tarjeta", it.toString()) }; e.estadoId?.let { put("estado", it.toString()) }
        }
        _e.value = e.copy(descargando = true)
        _e.value = when (val r = repo.descargarReporte(e.reporte, parametros, File(cache, "reportes"))) {
            is Resultado.Ok -> _e.value.copy(descargando = false, archivo = r.datos)
            is Resultado.Falla -> _e.value.copy(descargando = false, mensaje = r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(onVolver: () -> Unit, onAbrir: (String) -> Unit, viewModel: ReportesViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(e.mensaje) { e.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    // Al descargar: se abre con la app que el usuario elija (visor de PDF, hojas de cálculo, compartir…)
    LaunchedEffect(e.archivo) {
        e.archivo?.let { f ->
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", f)
            val tipo = when (f.extension) { "pdf" -> "application/pdf"; "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; else -> "text/csv" }
            val ver = Intent(Intent.ACTION_VIEW).setDataAndType(uri, tipo).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            runCatching { contexto.startActivity(Intent.createChooser(ver, f.name)) }.onFailure { viewModel.avisar("Se guardó ${f.name}, pero no hay una app para abrirlo.") }
            viewModel.archivoAbierto()
        }
    }
    val hayDatos = when (e.reporte) { "gastos" -> e.gastos != null; "prestamos" -> e.prestamos != null; "estado-cuenta" -> e.estado != null; else -> e.ejecucion != null }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Reportes") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    if (hayDatos) Box {
                        if (e.descargando) CircularProgressIndicator(Modifier.size(24.dp).padding(2.dp), strokeWidth = 2.dp)
                        else IconButton(onClick = { menu = true }) { Icon(Icons.Default.Download, contentDescription = "Descargar") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf("pdf" to "PDF", "xlsx" to "Excel", "csv" to "CSV").forEach { (f, t) ->
                                DropdownMenuItem(text = { Text("Descargar en $t") }, onClick = { menu = false; viewModel.descargar(f, contexto.cacheDir) })
                            }
                        }
                    }
                }
            )
        }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REPORTES.forEach { (clave, texto) -> FilterChip(selected = e.reporte == clave, onClick = { viewModel.elegir(clave) }, label = { Text(texto) }) }
            }
            if (e.reporte == "ejecucion" || e.reporte == "gastos") SelectorPeriodo(e, viewModel)
            when {
                e.cargando -> Cargando()
                e.error != null -> ErrorConReintento(e.error!!, { viewModel.cargar() })
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val quien = { id: Int? -> e.comunes?.miembros?.takeIf { it.size > 1 }?.firstOrNull { it.id == id }?.nombre?.substringBefore(" ") }
                    when (e.reporte) {
                        "gastos" -> e.gastos?.let { VistaGastos(it, quien, onAbrir) }
                        "prestamos" -> e.prestamos?.let { VistaPrestamos(it, onAbrir) }
                        "estado-cuenta" -> VistaEstado(e, viewModel, onAbrir)
                        else -> e.ejecucion?.let { VistaEjecucion(it, quien, onAbrir) }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** "‹ Ciclo del 23/09 al 22/10 ›" o un rango de fechas. */
@Composable
private fun SelectorPeriodo(e: ReportesEstado, vm: ReportesViewModel) {
    var rango by remember { mutableStateOf(e.desde != null) }
    var desde by remember { mutableStateOf(e.desde ?: e.periodo?.desde ?: Formato.hoy()) }
    var hasta by remember { mutableStateOf(e.hasta ?: e.periodo?.hasta ?: Formato.hoy()) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (!rango) Row(verticalAlignment = Alignment.CenterVertically) {
            val p = e.periodo
            IconButton(onClick = { vm.irACiclo(p?.anterior) }, enabled = p?.esCiclo == true && p.anterior != null) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ciclo anterior") }
            Text(p?.texto.orEmpty(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.irACiclo(p?.siguiente) }, enabled = p?.esCiclo == true && p.siguiente != null) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ciclo siguiente") }
        } else {
            CampoFecha("Desde", desde, { desde = it }, desde = null, hasta = Formato.hoy())
            CampoFecha("Hasta", hasta, { hasta = it }, desde = desde, hasta = null)
            Button(onClick = { vm.rango(desde, hasta) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("Ver") }
        }
        TextButton(onClick = { rango = !rango; if (!rango && e.desde != null) vm.irACiclo(null) }) { Text(if (rango) "Por ciclo" else "Elegir fechas") }
    }
}

@Composable
private fun Cifra(etiqueta: String, valor: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

/** Barra de una categoría: nombre, barra con su color, monto y porcentaje. */
@Composable
private fun BarraCategoria(c: CategoriaReporte, maximo: Double, onClick: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 4.dp)) {
        Row {
            Text(c.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${Formato.dinero(c.monto)}  ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${c.porcentaje} %", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth((c.monto / maximo).toFloat().coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape).background(Formato.color(c.color, MaterialTheme.colorScheme.primary)))
        }
    }
}

@Composable
private fun VistaEjecucion(d: Ejecucion, quien: (Int?) -> String?, onAbrir: (String) -> Unit) {
    val t = d.totales
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cifra("Ingresos", Formato.dinero(t.ingresos), Modifier.weight(1f), Semantica.ingreso)
        Cifra("Gastos", Formato.dinero(t.gastos), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cifra("Ahorro", Formato.dinero(t.ahorro), Modifier.weight(1f), if (t.ahorro < 0) Semantica.egreso else Semantica.ingreso)
        Cifra("Al pagar compromisos", Formato.dinero(t.liquidez), Modifier.weight(1f), if (t.liquidez < 0) Semantica.egreso else MaterialTheme.colorScheme.onSurface)
    }
    Seccion("Ingresos · ${Formato.dinero(t.ingresos)}") {
        if (d.ingresos.isEmpty()) Text("No hubo ingresos en este período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        d.ingresos.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(m.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(Formato.fechaCorta(m.fecha), m.categoria, m.donde, quien(m.userId)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(Formato.dinero(m.monto), color = Semantica.ingreso, fontWeight = FontWeight.Medium)
            }
        }
    }
    Seccion("Gastos por categoría · ${Formato.dinero(t.gastos)}") {
        if (d.gastos.isEmpty()) Text("No hubo gastos en este período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val maximo = d.gastos.maxOfOrNull { it.monto }?.coerceAtLeast(1.0) ?: 1.0
        d.gastos.forEach { BarraCategoria(it, maximo) }
    }
    Seccion("Efectivo y cuentas") {
        FilaValor("Efectivo: ${Formato.dinero(d.efectivo.saldoInicial)} → ", Formato.dinero(d.efectivo.saldoFinal), negrita = true)
        Text("Entró ${Formato.dinero(d.efectivo.ingresos)} · salió ${Formato.dinero(d.efectivo.egresos)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        d.cuentas.forEach { c ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            FilaValor("${c.nombre}: ${Formato.dinero(c.saldoInicial)} → ", Formato.dinero(c.saldoFinal), negrita = true, onClick = { onAbrir("cuenta/${c.id}") })
            Text("Entró ${Formato.dinero(c.ingresos)} · salió ${Formato.dinero(c.egresos)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        FilaValor("Total al final", Formato.dinero(t.efectivo + t.cuentas), negrita = true)
    }
    Seccion("Compromisos") {
        if (d.compromisos.isEmpty()) Text("No hubo compromisos con fecha en este período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        d.compromisos.forEach { c ->
            Row(Modifier.fillMaxWidth().clickable { onAbrir("compromiso/${c.id}") }.padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(c.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${Formato.fechaCorta(c.fecha)} · ${c.estadoTexto}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(Formato.dinero(if (c.pendiente > 0) c.pendiente else c.monto), fontWeight = FontWeight.Medium)
            }
        }
        if (d.compromisos.isNotEmpty()) FilaValor("Por pagar", Formato.dinero(t.porPagar), negrita = true)
    }
    Seccion("Tarjetas (hoy, por fecha de pago)") {
        d.tarjetas.forEach { tj ->
            Row(Modifier.fillMaxWidth().clickable { onAbrir("tarjeta/${tj.id}") }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 4.dp, height = 28.dp).clip(CircleShape).background(Formato.color(tj.color)))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(tj.nombre, quien(tj.userId)).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(tj.fechaLimite?.let { "Pagar antes del ${Formato.fechaCorta(it)}" } ?: "Sin pagos pendientes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (tj.porPagar > 0) Formato.dinero(tj.porPagar) else "—", fontWeight = FontWeight.Medium)
                    Text("disp. ${Formato.dinero(tj.disponible)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider()
        FilaValor("Por pagar", Formato.dinero(t.tarjetasPorPagar), negrita = true)
        FilaValor("Disponible", Formato.dinero(t.tarjetasDisponible))
    }
    Seccion("Nos deben (hoy) · ${Formato.dinero(t.nosDeben)}") {
        if (d.nosDeben.isEmpty()) Text("Nadie nos debe. 🎉", color = MaterialTheme.colorScheme.onSurfaceVariant)
        d.nosDeben.forEach { c -> FilaValor(c.nombre, Formato.dinero(c.pendiente), onClick = { onAbrir("contacto/${c.id}") }) }
    }
}

@Composable
private fun VistaGastos(d: GastosReporte, quien: (Int?) -> String?, onAbrir: (String) -> Unit) {
    var abierta by remember { mutableStateOf<String?>(null) }
    val varios = d.ciclos.size > 1
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cifra("Gastado", Formato.dinero(d.total), Modifier.weight(1f))
        if (varios) Cifra("Promedio por ciclo", Formato.dinero(d.promedioPorCiclo), Modifier.weight(1f))
        else Cifra("Mayor gasto", d.categorias.firstOrNull()?.nombre ?: "—", Modifier.weight(1f))
    }
    if (d.categorias.isEmpty()) { Vacio("No hubo gastos en este período."); return }
    if (varios) Seccion("Total por ciclo") {
        d.ciclos.forEachIndexed { i, c -> FilaValor(Formato.rango(c.inicio, c.fin), Formato.dinero(d.porCiclo.getOrElse(i) { 0.0 })) }
    }
    Seccion("Por categoría") {
        Text("Toca una categoría para ver sus movimientos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val maximo = d.categorias.maxOf { it.monto }.coerceAtLeast(1.0)
        d.categorias.forEach { c ->
            BarraCategoria(c, maximo) { abierta = if (abierta == c.nombre) null else c.nombre }
            if (abierta == c.nombre) Column(Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                if (varios) Text(d.ciclos.indices.joinToString(" · ") { "${Formato.fechaCorta(d.ciclos[it].fin)}: ${Formato.dinero(c.porCiclo?.getOrNull(it) ?: 0.0)}" },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                c.movimientos.orEmpty().forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(Formato.fechaCorta(m.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.descripcion, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(m.donde, quien(m.userId)).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Formato.dinero(m.monto), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaPrestamos(d: PrestamosReporte, onAbrir: (String) -> Unit) {
    val t = d.totales
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cifra("Saldo", Formato.dinero(t.saldo), Modifier.weight(1f))
        Cifra("Cuotas al mes", Formato.dinero(t.cuotasAlMes), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cifra("Intereses pagados", Formato.dinero(t.pagadoIntereses), Modifier.weight(1f))
        Cifra("Intereses que faltan", Formato.dinero(t.interesesRestantes), Modifier.weight(1f), Semantica.egreso)
    }
    if (d.prestamos.isEmpty()) Vacio("No hay préstamos registrados.")
    d.prestamos.forEach { p ->
        Seccion(p.nombre + if (p.activo) "" else " (pagado)") {
            Text(listOfNotNull(p.acreedor, Formato.dinero(p.montoOriginal), p.tasaAnual?.let { "${it} % anual" }, p.plazoMeses?.let { "$it meses" }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { (p.avance / 100).toFloat().coerceIn(0f, 1f) }, color = Formato.color(p.color, MaterialTheme.colorScheme.primary),
                strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            FilaValor("Saldo (${p.avance} % pagado)", Formato.dinero(p.saldo), negrita = true, onClick = { onAbrir("prestamo/${p.id}") })
            p.cuotaTotal?.let { FilaValor("Cuota al mes", Formato.dinero(it)) }
            FilaValor("Pagos registrados", p.pagosRealizados.toString())
            FilaValor("Capital pagado", Formato.dinero(p.pagadoCapital))
            FilaValor("Intereses pagados", Formato.dinero(p.pagadoIntereses))
            p.interesesRestantes?.let { FilaValor("Intereses que faltan", Formato.dinero(it), Semantica.egreso) }
            p.mesesRestantes?.let { FilaValor("Faltan", "$it meses · ${Formato.fechaCorta(p.fechaFin)}") }
        }
    }
}

@Composable
private fun VistaEstado(e: ReportesEstado, vm: ReportesViewModel, onAbrir: (String) -> Unit) {
    Desplegable("Tarjeta", listOf<Pair<Int?, String>>(null to "Elegir…") + e.tarjetas.map { it.id to it.nombre + if (it.activo) "" else " (inactiva)" }, e.tarjetaId, vm::elegirTarjeta)
    if (e.tarjetaId != null) {
        if (e.cortes.isEmpty()) Text("Los estados de cuenta se generan solos en cada corte; esta tarjeta aún no ha cortado desde que se registró.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Desplegable("Corte", listOf<Pair<Int?, String>>(null to "Elegir…") + e.cortes.map { it.id to "${Formato.fechaCorta(it.fechaCorte)} · ${Formato.dinero(it.total)}" }, e.estadoId, vm::elegirCorte)
    }
    val d = e.estado ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cifra("Total", Formato.dinero(d.total), Modifier.weight(1f))
        Cifra("Pagado", Formato.dinero(d.pagado), Modifier.weight(1f), Semantica.ingreso)
        Cifra("Pendiente", Formato.dinero(d.pendiente), Modifier.weight(1f))
    }
    Seccion("Del ${Formato.fechaCorta(d.fechaInicio)} al ${Formato.fechaCorta(d.fechaCorte)}") {
        Text("Pagar antes del ${Formato.fechaLarga(d.fechaLimitePago)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        d.lineas.forEach { l ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(l.descripcion + (l.cuota?.let { " ($it)" } ?: ""), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(Formato.fechaCorta(l.fecha), l.categoria).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(Formato.dinero(l.monto), color = if (l.monto < 0) Semantica.ingreso else MaterialTheme.colorScheme.onSurface)
            }
        }
        HorizontalDivider()
        FilaValor("Total del corte", Formato.dinero(d.total), negrita = true)
        if (d.estado == "pendiente" && d.compromisoId != null) TextButton(onClick = { onAbrir("compromiso/${d.compromisoId}") }) { Text("Ir a pagarlo") }
    }
}
