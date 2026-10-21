package com.carlosramos.misgastos.ui.cuentas

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.carlosramos.misgastos.data.remote.dto.CicloEfectivo
import com.carlosramos.misgastos.data.remote.dto.Cuenta
import com.carlosramos.misgastos.data.remote.dto.MovimientoCuenta
import com.carlosramos.misgastos.data.remote.dto.MovimientoCuentaBody
import com.carlosramos.misgastos.data.remote.dto.ResumenCuenta
import com.carlosramos.misgastos.data.remote.dto.TransferenciaMini
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FormMovimiento
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.FormularioMovimiento
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CuentaEstado(
    val cargando: Boolean = true,
    val cargandoMas: Boolean = false,
    val error: String? = null,
    val cuenta: Cuenta? = null,
    val ciclo: CicloEfectivo? = null,
    val resumen: ResumenCuenta? = null,
    val movimientos: List<MovimientoCuenta> = emptyList(),
    val pagina: Int = 1,
    val hayMas: Boolean = false,
    val tipo: String? = null,
    val cicloConsultado: String? = null,
    val comunes: Comunes? = null,
    val mensaje: String? = null
)

@HiltViewModel
class CuentaDetalleViewModel @Inject constructor(
    private val repo: FinanzasRepository,
    estadoGuardado: SavedStateHandle
) : ViewModel() {
    val cuentaId: Int = checkNotNull(estadoGuardado["id"])
    private val _estado = MutableStateFlow(CuentaEstado())
    val estado = _estado.asStateFlow()
    private val _form = MutableStateFlow<FormMovimiento?>(null)
    val form = _form.asStateFlow()

    init {
        viewModelScope.launch { (repo.comunes() as? Resultado.Ok)?.let { _estado.value = _estado.value.copy(comunes = it.datos) } }
        cargar()
    }

    fun cargar() = viewModelScope.launch {
        val e = _estado.value
        _estado.value = e.copy(cargando = e.cuenta == null, error = null)
        _estado.value = when (val r = repo.cuenta(cuentaId, e.cicloConsultado, e.tipo, 1)) {
            is Resultado.Ok -> _estado.value.copy(
                cargando = false, cuenta = r.datos.cuenta, ciclo = r.datos.ciclo, resumen = r.datos.resumen, movimientos = r.datos.data,
                pagina = 1, hayMas = r.datos.meta.paginaActual < r.datos.meta.ultimaPagina
            )
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun cargarMas() {
        val e = _estado.value
        if (!e.hayMas || e.cargandoMas) return
        _estado.value = e.copy(cargandoMas = true)
        viewModelScope.launch {
            _estado.value = when (val r = repo.cuenta(cuentaId, e.cicloConsultado, e.tipo, e.pagina + 1)) {
                is Resultado.Ok -> _estado.value.copy(
                    cargandoMas = false, movimientos = _estado.value.movimientos + r.datos.data, pagina = r.datos.meta.paginaActual,
                    hayMas = r.datos.meta.paginaActual < r.datos.meta.ultimaPagina
                )
                is Resultado.Falla -> _estado.value.copy(cargandoMas = false, mensaje = r.mensaje)
            }
        }
    }

    fun filtrar(tipo: String?) { _estado.value = _estado.value.copy(tipo = tipo, movimientos = emptyList()); cargar() }
    fun irACiclo(fecha: String?) { _estado.value = _estado.value.copy(cicloConsultado = fecha, movimientos = emptyList()); cargar() }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }
    fun avisar(texto: String) { _estado.value = _estado.value.copy(mensaje = texto) }

    fun nuevo() {
        _form.value = FormMovimiento(userId = _estado.value.comunes?.usuarioId, fecha = minOf(Formato.hoy(), _estado.value.ciclo?.fin ?: Formato.hoy()))
    }

    fun editar(m: MovimientoCuenta) {
        _form.value = FormMovimiento(
            id = m.id, tipo = m.tipo, descripcion = m.descripcion, monto = m.monto, fecha = m.fecha,
            categoriaId = m.categoria?.id, userId = m.userId, contactoId = m.contacto?.id
        )
    }

    fun cambiar(f: FormMovimiento) { _form.value = f }
    fun cerrarForm() { _form.value = null }

    fun guardar() {
        val f = _form.value ?: return
        val errores = buildMap {
            if (f.descripcion.isBlank()) put("descripcion", "Escribe una descripción.")
            if (!Formato.montoValido(f.monto)) put("monto", "Monto positivo con máximo dos decimales.")
            if (f.categoriaId == null) put("categoria_personal_id", "Elige una categoría.")
        }
        if (errores.isNotEmpty()) { _form.value = f.copy(errores = errores); return }
        _form.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = MovimientoCuentaBody(f.tipo, f.descripcion.trim(), f.monto, f.fecha, f.categoriaId, f.userId, f.contactoId?.toString())
            when (val r = repo.guardarMovimientoCuenta(cuentaId, f.id, body)) {
                is Resultado.Ok -> { _form.value = null; avisar(if (f.id == null) "Movimiento registrado." else "Movimiento actualizado."); cargar() }
                is Resultado.Falla -> _form.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() {
        val id = _form.value?.id ?: return
        _form.value = _form.value?.copy(guardando = true)
        viewModelScope.launch {
            when (val r = repo.eliminarMovimientoCuenta(cuentaId, id)) {
                is Resultado.Ok -> { _form.value = null; avisar("Movimiento eliminado."); cargar() }
                is Resultado.Falla -> _form.value = _form.value?.copy(guardando = false, error = r.mensaje)
            }
        }
    }

    fun eliminarTransferencia(t: TransferenciaMini) = viewModelScope.launch {
        when (val r = repo.eliminarTransferencia(t.id)) {
            is Resultado.Ok -> { avisar("Transferencia eliminada: se revirtieron sus dos movimientos."); cargar() }
            is Resultado.Falla -> avisar(r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentaDetalleScreen(onVolver: () -> Unit, onEditar: (Int) -> Unit, viewModel: CuentaDetalleViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var transferir by remember { mutableStateOf(false) }
    var borrarTransferencia by remember { mutableStateOf<TransferenciaMini?>(null) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    val c = estado.cuenta
    LaunchedEffect(Unit) { if (estado.cuenta != null) viewModel.cargar() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(c?.nombre ?: "Cuenta", maxLines = 1, overflow = TextOverflow.Ellipsis) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (c != null) IconButton(onClick = { onEditar(c.id) }) { Icon(Icons.Default.Edit, contentDescription = "Editar cuenta") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (estado.ciclo?.esActual == true && c?.activo == true) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = { transferir = true }) { Icon(Icons.Default.SwapHoriz, contentDescription = "Transferir desde esta cuenta") }
                FloatingActionButton(onClick = viewModel::nuevo) { Icon(Icons.Default.Add, contentDescription = "Registrar movimiento") }
            }
        }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            c == null -> ErrorConReintento(estado.error ?: "No se pudo cargar.", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> Column(Modifier.padding(relleno).fillMaxSize()) {
                Encabezado(c, estado, viewModel)
                Lista(estado, viewModel, onTransferencia = { borrarTransferencia = it })
            }
        }
    }
    form?.let { f ->
        estado.comunes?.let { com ->
            FormularioMovimiento(f, com, estado.ciclo?.inicio, viewModel::cambiar, viewModel::guardar, viewModel::eliminar, viewModel::cerrarForm)
        }
    }
    if (transferir) TransferenciaSheet(
        onCerrar = { transferir = false }, onHecho = { transferir = false; viewModel.avisar("Transferencia registrada."); viewModel.cargar() },
        origen = "cuenta:${viewModel.cuentaId}", destino = "efectivo"
    )
    borrarTransferencia?.let { t ->
        AlertDialog(
            onDismissRequest = { borrarTransferencia = null },
            title = { Text("Transferencia ${t.origen} → ${t.destino}") },
            text = { Text(if (t.eliminable) "Se elimina completa: se revierten sus dos movimientos." else "Es de un ciclo cerrado y ya no se puede eliminar.") },
            confirmButton = {
                if (t.eliminable) TextButton(onClick = { borrarTransferencia = null; viewModel.eliminarTransferencia(t) }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                else TextButton(onClick = { borrarTransferencia = null }) { Text("Entendido") }
            },
            dismissButton = { if (t.eliminable) TextButton(onClick = { borrarTransferencia = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Encabezado(c: Cuenta, estado: CuentaEstado, vm: CuentaDetalleViewModel) {
    val ciclo = estado.ciclo
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.banco?.logoUrl, c.banco?.nombre ?: c.nombre, Formato.color(c.color), tamano = 40.dp, cuadrado = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Saldo actual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Formato.dinero(c.saldo), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.irACiclo(ciclo?.anterior) }, enabled = ciclo?.anterior != null) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ciclo anterior") }
            Text(
                ciclo?.let { Formato.rango(it.inicio, it.fin) + if (it.esActual) " · ciclo actual" else "" }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { vm.irACiclo(ciclo?.siguiente) }, enabled = ciclo?.siguiente != null) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ciclo siguiente") }
        }
        estado.resumen?.let { r ->
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Cifra("Inicio", Formato.dinero(r.saldoInicial), Modifier.weight(1f))
                Cifra("Ingresos", Formato.dinero(r.ingresos), Modifier.weight(1f))
                Cifra("Egresos", Formato.dinero(r.egresos), Modifier.weight(1f))
                Cifra("Final", Formato.dinero(r.saldoFinal), Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Todos", "ingreso" to "Ingresos", "egreso" to "Egresos").forEach { (tipo, texto) ->
                FilterChip(selected = estado.tipo == tipo, onClick = { vm.filtrar(tipo) }, label = { Text(texto) })
            }
        }
    }
}

@Composable
private fun Cifra(etiqueta: String, valor: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Lista(estado: CuentaEstado, vm: CuentaDetalleViewModel, onTransferencia: (TransferenciaMini) -> Unit) {
    val lista = rememberLazyListState()
    val alFinal by remember { derivedStateOf { lista.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lista.layoutInfo.totalItemsCount - 1 } }
    LaunchedEffect(alFinal) { if (alFinal) vm.cargarMas() }
    LaunchedEffect(estado.movimientos.firstOrNull()?.id) { lista.scrollToItem(0) }
    val miembros = estado.comunes?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }
    if (estado.movimientos.isEmpty()) { Vacio("No hay movimientos en este ciclo."); return }
    LazyColumn(state = lista, contentPadding = PaddingValues(bottom = 140.dp), modifier = Modifier.fillMaxSize()) {
        estado.movimientos.groupBy { it.fecha }.forEach { (fecha, movimientos) ->
            item(key = "f$fecha") {
                Text(Formato.fechaLarga(fecha), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
            }
            items(movimientos, key = { it.id }) { m ->
                FilaMovimiento(m, miembros[m.userId]) {
                    when {
                        m.transferencia != null -> onTransferencia(m.transferencia)
                        m.editable -> vm.editar(m)
                        m.gestionadoEn != null -> vm.avisar("Este movimiento se modifica desde ${m.gestionadoEn}.")
                        else -> vm.avisar("Es de un ciclo cerrado: ya no se modifica.")
                    }
                }
            }
        }
        if (estado.cargandoMas) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
    }
}

@Composable
private fun FilaMovimiento(m: MovimientoCuenta, quien: String?, onClick: () -> Unit) {
    val ingreso = m.tipo == "ingreso"
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (m.transferencia != null || (m.categoria == null && m.contacto == null)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Transferencia", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        } else {
            Avatar(m.contacto?.fotoUrl, m.contacto?.nombre ?: m.categoria?.nombre ?: "?", Formato.color(m.categoria?.color), tamano = 36.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(m.descripcion, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(listOfNotNull(m.categoria?.nombre, m.contacto?.nombre, quien, m.gestionadoEn).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (!m.editable && m.transferencia == null) Icon(Icons.Default.Lock, contentDescription = "No se modifica aquí", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
        Text((if (ingreso) "+" else "−") + Formato.dinero(m.monto), color = if (ingreso) Semantica.ingreso else Semantica.egreso, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
