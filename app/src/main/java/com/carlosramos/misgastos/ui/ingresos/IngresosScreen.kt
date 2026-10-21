package com.carlosramos.misgastos.ui.ingresos

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.FuenteIngreso
import com.carlosramos.misgastos.data.remote.dto.LineaReciboBody
import com.carlosramos.misgastos.data.remote.dto.Recibo
import com.carlosramos.misgastos.data.remote.dto.ReciboBody
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.HojaFormulario
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio
import com.carlosramos.misgastos.ui.common.nombreOrigen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun String?.num() = this?.toDoubleOrNull() ?: 0.0

fun frecuencia(f: FuenteIngreso): String = when (f.frecuencia) {
    "mensual" -> "Mensual" + (f.diaPago?.let { " · día $it" } ?: "")
    "quincenal" -> "Quincenal" + (f.diaPago?.let { d -> " · días $d" + (f.diaPago2?.let { " y $it" } ?: "") } ?: "")
    "semanal" -> "Semanal"
    "eventual" -> "Eventual"
    else -> f.frecuencia.replaceFirstChar { it.uppercase() }
}

// ======================= Lista =======================

data class IngresosEstado(val cargando: Boolean = true, val error: String? = null, val fuentes: List<FuenteIngreso> = emptyList(), val miembros: Map<Int, String> = emptyMap())

@HiltViewModel
class IngresosViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(IngresosEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        val miembros = (repo.comunes() as? Resultado.Ok)?.datos?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }
        _estado.value = when (val r = repo.ingresos()) {
            is Resultado.Ok -> IngresosEstado(cargando = false, fuentes = r.datos, miembros = miembros)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresosScreen(onAbrir: (Int) -> Unit, onNueva: () -> Unit, onVolver: () -> Unit, viewModel: IngresosViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (estado.fuentes.isNotEmpty()) viewModel.cargar() }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Ingresos") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { IconButton(onClick = onNueva) { Icon(Icons.Default.Add, contentDescription = "Nueva fuente de ingreso") } }
            )
        }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            estado.error != null && estado.fuentes.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() }, Modifier.padding(relleno))
            estado.fuentes.isEmpty() -> Vacio("No hay fuentes de ingreso. Créala con el botón +.", Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(estado.fuentes, key = { it.id }) { f ->
                    Card(Modifier.fillMaxWidth().clickable { onAbrir(f.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Formato.color(f.color, Semantica.ingreso)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(listOfNotNull(f.tipo, frecuencia(f), estado.miembros[f.userId]).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            f.montoBruto?.takeIf { it.num() > 0 }?.let { Text(Formato.dinero(it), fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

// ======================= Detalle y registrar pago =======================

data class LineaForm(val nombre: String, val tipo: String, val monto: String, val prestamoId: Int?)

data class FormRecibo(
    val fecha: String = Formato.hoy(),
    val bruto: String = "",
    val lineas: List<LineaForm> = emptyList(),
    val destino: String = "efectivo",
    val comentario: String = "",
    val userId: Int? = null,
    val calculando: Boolean = false,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
) {
    val neto get() = bruto.num() - lineas.filter { it.tipo == "descuento" }.sumOf { it.monto.num() } + lineas.filter { it.tipo == "adicion" }.sumOf { it.monto.num() }
}

data class IngresoEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val fuente: FuenteIngreso? = null,
    val comunes: Comunes? = null,
    val destinos: List<Pair<String, String>> = emptyList(),
    val mensaje: String? = null
)

@HiltViewModel
class IngresoDetalleViewModel @Inject constructor(
    private val repo: FinanzasRepository,
    estadoGuardado: SavedStateHandle
) : ViewModel() {
    private val id: Int = checkNotNull(estadoGuardado["id"])
    private val _estado = MutableStateFlow(IngresoEstado())
    val estado = _estado.asStateFlow()
    private val _form = MutableStateFlow<FormRecibo?>(null)
    val form = _form.asStateFlow()
    private var calculo: Job? = null

    init {
        viewModelScope.launch {
            val cajas = (repo.cajas() as? Resultado.Ok)?.datos.orEmpty().map { it.valor to "${it.nombre} (${Formato.dinero(it.saldo)})" }
            _estado.value = _estado.value.copy(comunes = (repo.comunes() as? Resultado.Ok)?.datos, destinos = cajas)
        }
        cargar()
    }

    fun cargar() = viewModelScope.launch {
        _estado.value = when (val r = repo.ingreso(id)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, fuente = r.datos, error = null)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }

    fun abrir() {
        val f = _estado.value.fuente ?: return
        _form.value = FormRecibo(bruto = f.montoBruto?.takeIf { it.num() > 0 }.orEmpty(), destino = f.destino ?: "efectivo", userId = f.userId, calculando = true)
        proponer()
    }

    /** El servidor calcula los descuentos de la plantilla (ISSS, AFP, renta, préstamos…) para el bruto. */
    private fun proponer() {
        calculo?.cancel()
        calculo = viewModelScope.launch {
            delay(400)
            val bruto = _form.value?.bruto?.takeIf { Formato.montoValido(it) }
            when (val r = repo.propuesta(id, bruto)) {
                is Resultado.Ok -> _form.value = _form.value?.copy(
                    calculando = false,
                    lineas = r.datos.lineas.map { LineaForm(it.nombre, it.tipo, if (it.monto > 0) "%.2f".format(java.util.Locale.US, it.monto) else "", it.prestamoId) }
                )
                is Resultado.Falla -> _form.value = _form.value?.copy(calculando = false)
            }
        }
    }

    fun cambiarBruto(valor: String) { _form.value = _form.value?.copy(bruto = valor, calculando = true); proponer() }
    fun cambiar(f: FormRecibo) { _form.value = f }
    fun cerrar() { _form.value = null }

    fun guardar() {
        val f = _form.value ?: return
        val errores = buildMap {
            if (!Formato.montoValido(f.bruto)) put("bruto", "Monto positivo con máximo dos decimales.")
            if (f.neto < 0) put("bruto", "Los descuentos superan el bruto.")
        }
        if (errores.isNotEmpty()) { _form.value = f.copy(errores = errores); return }
        _form.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val lineas = f.lineas.filter { it.monto.num() > 0 }.map { LineaReciboBody(it.nombre, it.tipo, it.monto, it.prestamoId) }
            when (val r = repo.registrarRecibo(id, ReciboBody(f.fecha, f.bruto, f.destino, f.comentario.trim().ifBlank { null }, lineas, f.userId))) {
                is Resultado.Ok -> { _form.value = null; _estado.value = _estado.value.copy(fuente = r.datos, mensaje = "Pago registrado: entraron ${Formato.dinero(f.neto)}.") }
                is Resultado.Falla -> _form.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar(r: Recibo) = viewModelScope.launch {
        when (val res = repo.eliminarRecibo(r.id)) {
            is Resultado.Ok -> { _estado.value = _estado.value.copy(mensaje = "Pago eliminado."); cargar() }
            is Resultado.Falla -> _estado.value = _estado.value.copy(mensaje = res.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresoDetalleScreen(onVolver: () -> Unit, onEditar: (Int) -> Unit, viewModel: IngresoDetalleViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var borrar by remember { mutableStateOf<Recibo?>(null) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    val f = estado.fuente
    LaunchedEffect(Unit) { if (estado.fuente != null) viewModel.cargar() }
    val cajas = estado.destinos.associate { (k, v) -> k to v.substringBefore(" (") }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(f?.nombre ?: "Ingreso", maxLines = 1, overflow = TextOverflow.Ellipsis) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (f != null) IconButton(onClick = { onEditar(f.id) }) { Icon(Icons.Default.Edit, contentDescription = "Editar fuente") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            f == null -> ErrorConReintento(estado.error ?: "No se pudo cargar.", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> Column(Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Seccion(listOfNotNull(f.tipo, f.pagador).joinToString(" · ").ifBlank { "Fuente de ingreso" }) {
                    FilaValor("Frecuencia", frecuencia(f))
                    f.montoBruto?.takeIf { it.num() > 0 }?.let { FilaValor("Bruto habitual", Formato.dinero(it)) }
                    FilaValor("Entra a", nombreOrigen(f.destino, cajas))
                    if (f.conceptos.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        f.conceptos.forEach { c ->
                            FilaValor(
                                (if (c.tipo == "descuento") "− " else "+ ") + c.nombre,
                                when {
                                    c.monto.num() > 0 -> Formato.dinero(c.monto)
                                    c.porcentaje != null -> "${c.porcentaje} %" + (c.tope?.takeIf { it.num() > 0 }?.let { " (máx. ${Formato.dinero(it)})" } ?: "")
                                    else -> "Variable"
                                }
                            )
                        }
                    }
                }
                if (f.activo) Button(onClick = viewModel::abrir, modifier = Modifier.fillMaxWidth()) { Text("Registrar pago recibido") }
                Seccion("Pagos recibidos") {
                    if (f.recibos.isNullOrEmpty()) Text("Aún no hay pagos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    f.recibos.orEmpty().forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Column(Modifier.fillMaxWidth().clickable { borrar = r }.padding(vertical = 6.dp)) {
                            Row {
                                Text("${Formato.fechaCorta(r.fecha)} · ${nombreOrigen(r.destino, cajas)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(Formato.dinero(r.neto), color = Semantica.ingreso, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Bruto ${Formato.dinero(r.bruto)}" + r.lineas.joinToString("") { " · ${it.nombre} ${Formato.dinero(it.monto)}" },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    form?.let { r ->
        HojaFormulario(
            titulo = "Pago recibido", accion = "Guardar ${Formato.dinero(r.neto)}", guardando = r.guardando || r.calculando,
            onCerrar = viewModel::cerrar, onAccion = viewModel::guardar, error = r.error?.takeIf { r.errores.isEmpty() }
        ) {
            CampoFecha("Fecha", r.fecha, { viewModel.cambiar(r.copy(fecha = it)) }, desde = null, hasta = Formato.hoy(), error = r.errores["fecha"])
            CampoMonto("Bruto", r.bruto, viewModel::cambiarBruto, error = r.errores["bruto"], apoyo = "Los descuentos se calculan solos; ajústalos a tu boleta")
            r.lineas.forEachIndexed { i, l ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CampoMonto((if (l.tipo == "descuento") "− " else "+ ") + l.nombre, l.monto, { v ->
                        viewModel.cambiar(r.copy(lineas = r.lineas.toMutableList().also { it[i] = l.copy(monto = v) }))
                    }, Modifier.weight(1f), error = r.errores["lineas.$i.monto"], apoyo = if (l.prestamoId != null) "Se registra como pago del préstamo" else null)
                    IconButton(onClick = { viewModel.cambiar(r.copy(lineas = r.lineas.filterIndexed { j, _ -> j != i })) }) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar ${l.nombre}")
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Neto que entra", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(Formato.dinero(r.neto), fontWeight = FontWeight.Bold, color = if (r.neto < 0) Semantica.egreso else Semantica.ingreso)
            }
            Desplegable("¿A dónde entra?", estado.destinos + ("ninguno" to "Solo anotarlo"), r.destino, { viewModel.cambiar(r.copy(destino = it)) }, error = r.errores["destino"])
            OutlinedTextField(value = r.comentario, onValueChange = { viewModel.cambiar(r.copy(comentario = it)) }, label = { Text("Comentario (opcional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
        }
    }
    borrar?.let { r ->
        Confirmar("¿Eliminar este pago recibido?", "${Formato.dinero(r.neto)} del ${Formato.fechaCorta(r.fecha)}. Se revierte el ingreso y los pagos de préstamo que generó.", "Eliminar",
            onConfirmar = { borrar = null; viewModel.eliminar(r) }, onCancelar = { borrar = null })
    }
}
