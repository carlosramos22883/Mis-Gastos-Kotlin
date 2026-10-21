package com.carlosramos.misgastos.ui.compromisos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Compromiso
import com.carlosramos.misgastos.data.remote.dto.PagoCompromisoBody
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormPago(
    val origenes: List<Pair<String, String>> = emptyList(),
    val origen: String? = null,
    val descripcion: String = "",
    val monto: String = "",
    val fecha: String = Formato.hoy(),
    val categoriaId: Int? = null,
    val userId: Int? = null,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class DetalleEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val compromiso: Compromiso? = null,
    val comunes: Comunes? = null,
    val pago: FormPago? = null,
    val ocupado: Boolean = false,
    val mensaje: String? = null
)

@HiltViewModel
class CompromisoDetalleViewModel @Inject constructor(
    private val repo: FinanzasRepository,
    estadoGuardado: SavedStateHandle
) : ViewModel() {
    private val id: Int = checkNotNull(estadoGuardado["id"])
    private val _estado = MutableStateFlow(DetalleEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        val comunes = (repo.comunes() as? Resultado.Ok)?.datos
        _estado.value = when (val r = repo.compromiso(id)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, compromiso = r.datos, comunes = comunes)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun abrirPago() {
        val c = _estado.value.compromiso ?: return
        _estado.value = _estado.value.copy(
            pago = FormPago(
                descripcion = c.descripcion, monto = c.saldoPendiente, categoriaId = c.categoria?.id,
                userId = _estado.value.comunes?.usuarioId, guardando = true
            )
        )
        viewModelScope.launch {
            when (val r = repo.origenesPago(id)) {
                is Resultado.Ok -> {
                    val origenes = r.datos.toList()
                    _estado.value.pago?.let { _estado.value = _estado.value.copy(pago = it.copy(origenes = origenes, origen = origenes.firstOrNull()?.first, guardando = false)) }
                }
                is Resultado.Falla -> _estado.value.pago?.let { _estado.value = _estado.value.copy(pago = it.copy(guardando = false, error = r.mensaje)) }
            }
        }
    }

    fun cambiarPago(f: FormPago) { _estado.value = _estado.value.copy(pago = f) }
    fun cerrarPago() { _estado.value = _estado.value.copy(pago = null) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }

    fun pagar() {
        val f = _estado.value.pago ?: return
        val c = _estado.value.compromiso ?: return
        val errores = buildMap {
            if (f.origen == null) put("origen", "Elige de dónde sale el dinero.")
            if (f.descripcion.isBlank()) put("descripcion", "Escribe una descripción.")
            if (!Formato.montoValido(f.monto)) put("monto", "Monto positivo con máximo dos decimales.")
            if (c.tarjeta == null && f.categoriaId == null) put("categoria_personal_id", "Elige una categoría.")
        }
        if (errores.isNotEmpty()) { cambiarPago(f.copy(errores = errores)); return }
        cambiarPago(f.copy(guardando = true, errores = emptyMap(), error = null))
        viewModelScope.launch {
            val body = PagoCompromisoBody(f.origen!!, f.descripcion.trim(), f.monto, f.fecha, if (c.tarjeta == null) f.categoriaId else null, f.userId)
            when (val r = repo.pagarCompromiso(id, body)) {
                is Resultado.Ok -> {
                    _estado.value = _estado.value.copy(pago = null, mensaje = "Pago registrado.")
                    cargar()
                }
                is Resultado.Falla -> cambiarPago(f.copy(guardando = false, errores = r.campos, error = r.mensaje))
            }
        }
    }

    fun cancelar(serie: Boolean, alTerminar: () -> Unit) {
        _estado.value = _estado.value.copy(ocupado = true)
        viewModelScope.launch {
            when (val r = repo.cancelarCompromiso(id, serie)) {
                is Resultado.Ok -> alTerminar()
                is Resultado.Falla -> _estado.value = _estado.value.copy(ocupado = false, mensaje = r.mensaje)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompromisoDetalleScreen(onVolver: () -> Unit, viewModel: CompromisoDetalleViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var cancelar by remember { mutableStateOf<Boolean?>(null) } // false = este pago · true = la serie
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Compromiso") },
                windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        val c = estado.compromiso
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            c == null -> ErrorConReintento(estado.error ?: "No se pudo cargar.", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> Column(
                Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(c.descripcion, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row { Insignia(c.estadoTexto, colorEstado(c.estadoVisual)) }
                Text(
                    Formato.dinero(if (c.estado == "pendiente") c.saldoPendiente else c.monto),
                    fontSize = 32.sp, fontWeight = FontWeight.Bold
                )
                Seccion("Detalle") {
                    FilaValor("Monto", Formato.dinero(c.monto))
                    FilaValor("Pagado", Formato.dinero(c.montoPagado))
                    FilaValor("Pendiente", Formato.dinero(c.saldoPendiente), negrita = true)
                    FilaValor("Fecha esperada", c.fechaEsperada?.let { Formato.fechaLarga(it) } ?: "Sin fecha")
                    c.numeroCuota?.let { FilaValor("Cuota", "$it" + (c.totalCuotas?.let { t -> " de $t" } ?: "")) }
                    c.tarjeta?.let { FilaValor("Tarjeta", it.nombre ?: "") }
                    c.categoria?.let { FilaValor("Categoría", it.nombre) }
                    c.contacto?.let { FilaValor("Con", it.nombre) }
                    c.recurrente?.let { FilaValor("Se repite", it.descripcionFrecuencia ?: it.frecuencia) }
                    estado.comunes?.miembros?.firstOrNull { it.id == c.userId }?.let { FilaValor("Registrado por", it.nombre) }
                }
                c.comentario?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (!c.abonos.isNullOrEmpty()) Seccion("Pagos en efectivo") {
                    c.abonos.forEach { FilaValor("${Formato.fechaCorta(it.fecha)} · ${it.descripcion}", Formato.dinero(it.monto)) }
                }
                if (c.estado == "pendiente") {
                    Button(onClick = viewModel::abrirPago, modifier = Modifier.fillMaxWidth()) { Text("Pagar") }
                    OutlinedButton(onClick = { cancelar = false }, enabled = !estado.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Cancelar este pago") }
                    if (c.recurrente?.activo == true) OutlinedButton(onClick = { cancelar = true }, enabled = !estado.ocupado, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar la serie", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    estado.pago?.let { f -> estado.compromiso?.let { c -> FormularioPago(f, c, estado.comunes, viewModel) } }
    cancelar?.let { serie ->
        AlertDialog(
            onDismissRequest = { cancelar = null },
            title = { Text(if (serie) "¿Cancelar la serie?" else "¿Cancelar este pago?") },
            text = { Text(if (serie) "Se cancela este pago y ya no se generan los siguientes." else "Este pago queda cancelado; los demás de la serie siguen igual.") },
            confirmButton = { TextButton(onClick = { cancelar = null; viewModel.cancelar(serie, onVolver) }) { Text("Sí, cancelar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { cancelar = null }) { Text("No") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioPago(f: FormPago, c: Compromiso, comunes: Comunes?, vm: CompromisoDetalleViewModel) {
    ModalBottomSheet(onDismissRequest = vm::cerrarPago, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Pagar compromiso", style = MaterialTheme.typography.titleLarge)
            if (f.origenes.isEmpty() && f.guardando) CircularProgressIndicator()
            Desplegable("¿De dónde sale el dinero?", f.origenes, f.origen, { vm.cambiarPago(f.copy(origen = it)) }, error = f.errores["origen"])
            if (f.origen?.startsWith("tarjeta:") == true) Text(
                "Se registra como una compra en esa tarjeta (por ejemplo, pagar Credisiman con la Atlántida por NPE).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = f.monto, onValueChange = { vm.cambiarPago(f.copy(monto = it.replace(',', '.').filter { ch -> ch.isDigit() || ch == '.' })) },
                label = { Text("Monto") }, prefix = { Text("$ ") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = f.errores["monto"] != null,
                supportingText = { Text(f.errores["monto"] ?: "Pendiente: ${Formato.dinero(c.saldoPendiente)} (un monto menor es un abono)") },
                modifier = Modifier.fillMaxWidth()
            )
            CampoFecha("Fecha", f.fecha, { vm.cambiarPago(f.copy(fecha = it)) }, desde = null, hasta = Formato.hoy(), error = f.errores["fecha"])
            OutlinedTextField(
                value = f.descripcion, onValueChange = { vm.cambiarPago(f.copy(descripcion = it)) }, label = { Text("Descripción") },
                isError = f.errores["descripcion"] != null, supportingText = f.errores["descripcion"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            if (c.tarjeta == null && comunes != null) Desplegable(
                "Categoría", comunes.categoriasDe("egreso").map { it.id to it.nombre }, f.categoriaId,
                { vm.cambiarPago(f.copy(categoriaId = it)) }, error = f.errores["categoria_personal_id"]
            )
            if ((comunes?.miembros?.size ?: 0) > 1) Desplegable(
                "¿Quién pagó?", comunes!!.miembros.map { it.id to it.nombre }, f.userId, { vm.cambiarPago(f.copy(userId = it)) }, error = f.errores["user_id"]
            )
            f.error?.takeIf { f.errores.isEmpty() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = vm::cerrarPago) { Text("Cancelar") }
                Button(onClick = vm::pagar, enabled = !f.guardando && f.origenes.isNotEmpty()) {
                    if (f.guardando && f.origenes.isNotEmpty()) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Registrar pago")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
