package com.carlosramos.misgastos.ui.prestamos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Prestamo
import com.carlosramos.misgastos.data.remote.dto.PrestamoPago
import com.carlosramos.misgastos.data.remote.dto.PrestamoPagoBody
import com.carlosramos.misgastos.data.remote.dto.Proyeccion
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
import com.carlosramos.misgastos.ui.common.nombreOrigen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormPagoPrestamo(
    val fecha: String = Formato.hoy(),
    val capital: String = "",
    val interes: String = "",
    val cargos: String = "",
    val mora: String = "",
    val origen: String = "efectivo",
    val comentario: String = "",
    val userId: Int? = null,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
) {
    val total get() = listOf(capital, interes, cargos, mora).sumOf { it.toDoubleOrNull() ?: 0.0 }
}

data class PrestamoEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val prestamo: Prestamo? = null,
    val comunes: Comunes? = null,
    val origenes: List<Pair<String, String>> = emptyList(),
    val extra: String = "",
    val abono: String = "",
    val modo: String = "plazo",
    val proyeccion: Proyeccion? = null,
    val mensaje: String? = null
)

@HiltViewModel
class PrestamoDetalleViewModel @Inject constructor(
    private val repo: FinanzasRepository,
    estadoGuardado: SavedStateHandle
) : ViewModel() {
    private val id: Int = checkNotNull(estadoGuardado["id"])
    private val _estado = MutableStateFlow(PrestamoEstado())
    val estado = _estado.asStateFlow()
    private val _pago = MutableStateFlow<FormPagoPrestamo?>(null)
    val pago = _pago.asStateFlow()
    private var simular: Job? = null

    init {
        viewModelScope.launch {
            val cajas = (repo.cajas() as? Resultado.Ok)?.datos.orEmpty().map { it.valor to "${it.nombre} (${Formato.dinero(it.saldo)})" }
            _estado.value = _estado.value.copy(comunes = (repo.comunes() as? Resultado.Ok)?.datos, origenes = cajas)
        }
        cargar()
    }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        _estado.value = when (val r = repo.prestamo(id)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, prestamo = r.datos)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
        proyectar()
    }

    fun avisar(t: String) { _estado.value = _estado.value.copy(mensaje = t) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }

    /** Recalcula los escenarios (con una pausa corta mientras se escribe). */
    fun escenario(extra: String = _estado.value.extra, abono: String = _estado.value.abono, modo: String = _estado.value.modo) {
        _estado.value = _estado.value.copy(extra = extra, abono = abono, modo = modo)
        simular?.cancel()
        simular = viewModelScope.launch { delay(450); proyectar() }
    }

    private suspend fun proyectar() {
        val e = _estado.value
        val r = repo.proyeccion(id, e.extra.takeIf { Formato.montoValido(it) }, e.abono.takeIf { Formato.montoValido(it) }, e.modo)
        if (r is Resultado.Ok) _estado.value = _estado.value.copy(proyeccion = r.datos)
    }

    fun abrirPago() {
        val p = _estado.value.prestamo ?: return
        val s = p.sugerenciaPago
        val origen = when (p.medioPago) { "planilla" -> "planilla"; else -> "efectivo" }
        _pago.value = FormPagoPrestamo(
            capital = s?.capital.orEmpty(), interes = s?.interes.orEmpty(), cargos = s?.cargos?.takeIf { it != "0.00" }.orEmpty(),
            mora = s?.mora?.takeIf { it != "0.00" }.orEmpty(), origen = origen, userId = p.userId ?: _estado.value.comunes?.usuarioId
        )
    }

    fun cambiarPago(f: FormPagoPrestamo) { _pago.value = f }
    fun cerrarPago() { _pago.value = null }

    fun guardarPago() {
        val f = _pago.value ?: return
        val valido = { t: String -> t.isBlank() || Regex("^\\d+(\\.\\d{1,2})?$").matches(t) }
        val errores = buildMap {
            if (!valido(f.capital) || f.capital.isBlank()) put("capital", "Monto con máximo dos decimales (0 si no abona capital).")
            if (!valido(f.interes) || f.interes.isBlank()) put("interes", "Monto con máximo dos decimales (0 si no hay interés).")
            if (!valido(f.cargos)) put("cargos", "Máximo dos decimales.")
            if (!valido(f.mora)) put("mora", "Máximo dos decimales.")
            if (isEmpty() && f.total <= 0) put("capital", "El pago debe ser mayor que cero.")
        }
        if (errores.isNotEmpty()) { _pago.value = f.copy(errores = errores); return }
        _pago.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = PrestamoPagoBody(f.fecha, f.capital, f.interes, f.cargos.ifBlank { null }, f.mora.ifBlank { null }, f.origen, f.comentario.trim().ifBlank { null }, f.userId)
            when (val r = repo.pagarPrestamo(id, body)) {
                is Resultado.Ok -> { _pago.value = null; _estado.value = _estado.value.copy(prestamo = r.datos, mensaje = "Pago registrado."); proyectar() }
                is Resultado.Falla -> _pago.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminarPago(p: PrestamoPago) = viewModelScope.launch {
        when (val r = repo.eliminarPagoPrestamo(p.id)) {
            is Resultado.Ok -> { avisar("Pago eliminado."); cargar() }
            is Resultado.Falla -> avisar(r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrestamoDetalleScreen(onVolver: () -> Unit, onEditar: (Int) -> Unit, viewModel: PrestamoDetalleViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val pago by viewModel.pago.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var borrar by remember { mutableStateOf<PrestamoPago?>(null) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    val p = estado.prestamo
    LaunchedEffect(Unit) { if (estado.prestamo != null) viewModel.cargar() }
    val cajas = estado.origenes.associate { (k, v) -> k to v.substringBefore(" (") }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(p?.nombre ?: "Préstamo", maxLines = 1, overflow = TextOverflow.Ellipsis) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (p != null) IconButton(onClick = { onEditar(p.id) }) { Icon(Icons.Default.Edit, contentDescription = "Editar préstamo") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            p == null -> ErrorConReintento(estado.error ?: "No se pudo cargar.", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> Column(Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Seccion("${p.acreedor ?: ""} · ${p.tipo ?: ""}".trim(' ', '·')) {
                    FilaValor("Saldo", Formato.dinero(p.saldo), negrita = true)
                    FilaValor("Monto original", Formato.dinero(p.montoOriginal))
                    p.tasaAnual?.let { FilaValor("Tasa anual", "$it %") }
                    p.cuota?.let { FilaValor("Cuota (capital + interés)", Formato.dinero(it)) }
                    p.cargos.forEach { c -> FilaValor(c.nombre, Formato.dinero(c.monto)) }
                    p.cuotaTotal?.let { FilaValor("Total al mes", Formato.dinero(it), negrita = true) }
                    p.montoPlanilla?.takeIf { it.num() > 0 }?.let { FilaValor("Descontado de planilla", Formato.dinero(it)) }
                    FilaValor("Cómo se paga", medioPago(p.medioPago))
                    p.proximaFecha?.let { FilaValor("Próximo pago", Formato.fechaLarga(it)) }
                    p.fechaFin?.let { FilaValor("Termina", "${Formato.fechaCorta(it)} (${p.mesesRestantes ?: "?"} meses)") }
                    p.interesesRestantes?.let { FilaValor("Intereses por pagar", Formato.dinero(it), Semantica.egreso) }
                }
                if (p.activo) Button(onClick = viewModel::abrirPago, modifier = Modifier.fillMaxWidth()) { Text("Registrar pago") }
                Simulador(estado, viewModel)
                Seccion("Pagos registrados") {
                    if (p.pagos.isNullOrEmpty()) Text("Aún no hay pagos registrados en el sistema.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    p.pagos.orEmpty().forEachIndexed { i, pg ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Column(Modifier.fillMaxWidth().clickable(enabled = i == 0) { borrar = pg }.padding(vertical = 6.dp)) {
                            Row {
                                Text("${Formato.fechaCorta(pg.fecha)} · ${nombreOrigen(pg.origen, cajas)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(Formato.dinero(pg.total), fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "Capital ${Formato.dinero(pg.capital)} · interés ${Formato.dinero(pg.interes)}" +
                                    (pg.cargos?.takeIf { it.num() > 0 }?.let { " · cargos ${Formato.dinero(it)}" } ?: "") +
                                    (pg.mora?.takeIf { it.num() > 0 }?.let { " · mora ${Formato.dinero(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (i == 0) Text("Toca para eliminar el último pago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }

    pago?.let { f ->
        HojaFormulario(
            titulo = "Registrar pago", accion = "Guardar ${Formato.dinero(f.total)}", guardando = f.guardando,
            onCerrar = viewModel::cerrarPago, onAccion = viewModel::guardarPago, error = f.error?.takeIf { f.errores.isEmpty() }
        ) {
            Text("Los montos vienen de la sugerencia del sistema (interés = saldo × tasa × días / 365). Ajústalos al recibo del banco.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CampoFecha("Fecha", f.fecha, { viewModel.cambiarPago(f.copy(fecha = it)) }, desde = null, hasta = Formato.hoy(), error = f.errores["fecha"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoMonto("Capital", f.capital, { viewModel.cambiarPago(f.copy(capital = it)) }, Modifier.weight(1f), error = f.errores["capital"])
                CampoMonto("Interés", f.interes, { viewModel.cambiarPago(f.copy(interes = it)) }, Modifier.weight(1f), error = f.errores["interes"])
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoMonto("Seguros y cargos", f.cargos, { viewModel.cambiarPago(f.copy(cargos = it)) }, Modifier.weight(1f), error = f.errores["cargos"])
                CampoMonto("Mora", f.mora, { viewModel.cambiarPago(f.copy(mora = it)) }, Modifier.weight(1f), error = f.errores["mora"])
            }
            Desplegable(
                "¿De dónde salió?", estado.origenes + listOf("planilla" to "Descuento de planilla", "ninguno" to "Solo anotarlo"),
                f.origen, { viewModel.cambiarPago(f.copy(origen = it)) }, error = f.errores["origen"]
            )
            if (f.origen == "planilla") Text("Si este préstamo se descuenta en un sueldo, lo normal es que se registre solo al registrar ese ingreso.",
                style = MaterialTheme.typography.bodySmall, color = Semantica.aviso)
            OutlinedTextField(value = f.comentario, onValueChange = { viewModel.cambiarPago(f.copy(comentario = it)) }, label = { Text("Comentario (opcional)") }, modifier = Modifier.fillMaxWidth())
        }
    }
    borrar?.let { pg ->
        Confirmar("¿Eliminar el último pago?", "${Formato.dinero(pg.total)} del ${Formato.fechaCorta(pg.fecha)}. Se revierte su egreso y el saldo vuelve a como estaba.", "Eliminar",
            onConfirmar = { borrar = null; viewModel.eliminarPago(pg) }, onCancelar = { borrar = null })
    }
}

private fun String?.num() = this?.toDoubleOrNull() ?: 0.0

/** "¿Y si…?": pagar todo hoy, abonar algo extra cada mes o hacer un abono único. */
@Composable
private fun Simulador(e: PrestamoEstado, vm: PrestamoDetalleViewModel) {
    val pr = e.proyeccion
    Seccion("¿Y si…?") {
        pr?.pagarHoy?.let { h ->
            Text("Pagarlo todo hoy", style = MaterialTheme.typography.titleSmall)
            FilaValor("Habría que pagar", Formato.dinero(h.monto), negrita = true)
            FilaValor("Ahorro en intereses y cargos", Formato.dinero(h.ahorro), Semantica.ingreso)
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
        Text("Abonar algo extra cada mes", style = MaterialTheme.typography.titleSmall)
        CampoMonto("Extra mensual", e.extra, { vm.escenario(extra = it) })
        pr?.extraMensual?.let { x ->
            FilaValor("Terminaría", "${Formato.fechaCorta(x.tabla.fechaFin)} (${x.mesesMenos} meses antes)")
            FilaValor("Ahorro", Formato.dinero(x.ahorro), Semantica.ingreso, negrita = true)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Text("Hacer un abono único hoy", style = MaterialTheme.typography.titleSmall)
        CampoMonto("Abono", e.abono, { vm.escenario(abono = it) })
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("plazo" to "Acortar el plazo", "cuota" to "Bajar la cuota").forEachIndexed { i, (m, t) ->
                SegmentedButton(selected = e.modo == m, onClick = { vm.escenario(modo = m) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(t) }
            }
        }
        pr?.abono?.let { a ->
            FilaValor("Terminaría", "${Formato.fechaCorta(a.tabla.fechaFin)}" + if (a.mesesMenos > 0) " (${a.mesesMenos} meses antes)" else "")
            a.cuotaNueva?.let { FilaValor("Cuota", Formato.dinero(it)) }
            FilaValor("Ahorro", Formato.dinero(a.ahorro), Semantica.ingreso, negrita = true)
        }
    }
}
