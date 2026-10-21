package com.carlosramos.misgastos.ui.tarjetas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.MovimientoTarjeta
import com.carlosramos.misgastos.data.remote.dto.MovimientoTarjetaBody
import com.carlosramos.misgastos.data.remote.dto.TarjetaDetalle
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.cuentas.TransferenciaSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormMovTarjeta(
    val id: Int? = null,
    val tipo: String = "compra",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: String = Formato.hoy(),
    val cuotas: String = "",
    val categoriaId: Int? = null,
    val comentario: String = "",
    val userId: Int? = null,
    val contactoId: Int? = null,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class TarjetaEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val detalle: TarjetaDetalle? = null,
    val comunes: Comunes? = null,
    val mensaje: String? = null
)

@HiltViewModel
class TarjetaDetalleViewModel @Inject constructor(
    private val repo: FinanzasRepository,
    estadoGuardado: SavedStateHandle
) : ViewModel() {
    val tarjetaId: Int = checkNotNull(estadoGuardado["id"])
    private val _estado = MutableStateFlow(TarjetaEstado())
    val estado = _estado.asStateFlow()
    private val _form = MutableStateFlow<FormMovTarjeta?>(null)
    val form = _form.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        val comunes = (repo.comunes() as? Resultado.Ok)?.datos
        _estado.value = when (val r = repo.tarjeta(tarjetaId)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, detalle = r.datos, comunes = comunes)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun avisar(t: String) { _estado.value = _estado.value.copy(mensaje = t) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }

    fun nuevo() { _form.value = FormMovTarjeta(userId = _estado.value.detalle?.tarjeta?.userId ?: _estado.value.comunes?.usuarioId) }

    fun abrir(m: MovimientoTarjeta) {
        when {
            m.transferenciaId != null -> avisar("Es un avance: se elimina desde la cuenta o el efectivo al que fue (su transferencia).")
            m.gestionadoEn != null -> avisar("Este movimiento se modifica desde ${m.gestionadoEn}.")
            !m.editable -> avisar("Ya está en un estado de cuenta: no se modifica.")
            else -> _form.value = FormMovTarjeta(
                id = m.id, tipo = m.tipo, descripcion = m.descripcion, monto = m.monto, fecha = m.fecha, cuotas = m.cuotas?.takeIf { it > 1 }?.toString().orEmpty(),
                categoriaId = m.categoria?.id, comentario = m.comentario.orEmpty(), userId = m.userId, contactoId = m.contacto?.id
            )
        }
    }

    fun cambiar(f: FormMovTarjeta) { _form.value = f }
    fun cerrarForm() { _form.value = null }

    fun guardar() {
        val f = _form.value ?: return
        val cuotas = f.cuotas.toIntOrNull()
        val errores = buildMap {
            if (f.descripcion.isBlank()) put("descripcion", "Escribe una descripción.")
            if (!Formato.montoValido(f.monto)) put("monto", "Monto positivo con máximo dos decimales.")
            if (f.cuotas.isNotBlank() && (cuotas == null || cuotas !in 2..60)) put("cuotas", "Entre 2 y 60 cuotas (vacío = de contado).")
        }
        if (errores.isNotEmpty()) { _form.value = f.copy(errores = errores); return }
        _form.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = MovimientoTarjetaBody(
                f.tipo, f.descripcion.trim(), f.monto, f.fecha, if (f.tipo == "compra") cuotas else null,
                if (f.tipo == "credito") null else f.categoriaId, f.comentario.trim().ifBlank { null }, f.userId, f.contactoId?.toString()
            )
            when (val r = repo.guardarMovimientoTarjeta(tarjetaId, f.id, body)) {
                is Resultado.Ok -> { _form.value = null; avisar(if (f.id == null) "Movimiento registrado." else "Movimiento actualizado."); cargar() }
                is Resultado.Falla -> _form.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() {
        val id = _form.value?.id ?: return
        _form.value = _form.value?.copy(guardando = true)
        viewModelScope.launch {
            when (val r = repo.eliminarMovimientoTarjeta(tarjetaId, id)) {
                is Resultado.Ok -> { _form.value = null; avisar("Movimiento eliminado."); cargar() }
                is Resultado.Falla -> _form.value = _form.value?.copy(guardando = false, error = r.mensaje)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarjetaDetalleScreen(onVolver: () -> Unit, onAbrirCompromiso: (Int) -> Unit, onEditar: (Int) -> Unit, viewModel: TarjetaDetalleViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var avance by remember { mutableStateOf(false) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    val d = estado.detalle
    LaunchedEffect(Unit) { if (estado.detalle != null) viewModel.cargar() }
    val miembros = estado.comunes?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(d?.tarjeta?.nombre ?: "Tarjeta", maxLines = 1, overflow = TextOverflow.Ellipsis) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (d != null) IconButton(onClick = { onEditar(d.tarjeta.id) }) { Icon(Icons.Default.Edit, contentDescription = "Editar tarjeta") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (d?.tarjeta?.activo == true) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (d.tarjeta.permiteAvances) SmallFloatingActionButton(onClick = { avance = true }) { Icon(Icons.Default.SwapHoriz, contentDescription = "Avance de efectivo") }
                FloatingActionButton(onClick = viewModel::nuevo) { Icon(Icons.Default.Add, contentDescription = "Registrar compra o cargo") }
            }
        }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            d == null -> ErrorConReintento(estado.error ?: "No se pudo cargar.", { viewModel.cargar() }, Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { TarjetaVisual(d.tarjeta, miembros[d.tarjeta.userId]) }
                if (d.porPagar.isNotEmpty()) item {
                    Seccion("Por pagar") {
                        d.porPagar.forEach { p ->
                            Column(Modifier.fillMaxWidth().clickable { onAbrirCompromiso(p.compromiso.id) }.padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.compromiso.descripcion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(Formato.dinero(p.compromiso.saldoPendiente), fontWeight = FontWeight.SemiBold)
                                }
                                Text("Vence ${Formato.fechaLarga(p.compromiso.fechaEsperada)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                p.costo?.let { Text("No pagarlo completo costaría al menos ${Formato.dinero(it.interesMes)} de interés al mes (${it.tasa} % anual).", style = MaterialTheme.typography.bodySmall, color = Semantica.aviso) }
                            }
                        }
                    }
                }
                item {
                    Seccion("Próximo corte · ${Formato.fechaCorta(d.periodoAbierto.corte)}") {
                        FilaValor("Lleva acumulado", Formato.dinero(d.porFacturar.total), negrita = true)
                        d.porFacturar.lineas.filter { it.cuotaNumero != null }.forEach {
                            FilaValor("${it.descripcion} (cuota ${it.cuotaNumero}/${it.cuotas})", Formato.dinero(it.monto))
                        }
                    }
                }
                item {
                    Seccion("Movimientos sin facturar") {
                        if (d.movimientos.isEmpty()) Text("Nada desde el ${Formato.fechaCorta(d.periodoAbierto.desde)}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        d.movimientos.forEach { m -> FilaMovTarjeta(m, miembros[m.userId]) { viewModel.abrir(m) } }
                    }
                }
                if (d.enCuotas.isNotEmpty()) item {
                    Seccion("Compras en cuotas") {
                        d.enCuotas.forEach { e ->
                            val m = e.movimiento
                            val cuota = (m.monto.toDoubleOrNull() ?: 0.0) / (m.cuotas ?: 1)
                            FilaValor("${m.descripcion} · ${e.cuotasCobradas}/${m.cuotas} cobradas", "${Formato.dinero(cuota)}/mes")
                        }
                    }
                }
                if (d.estadosCuenta.isNotEmpty()) item {
                    Seccion("Estados de cuenta") {
                        d.estadosCuenta.forEach { e ->
                            Row(Modifier.fillMaxWidth().clickable(enabled = e.compromisoId != null) { e.compromisoId?.let(onAbrirCompromiso) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Corte ${Formato.fechaCorta(e.fechaCorte)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Límite ${Formato.fechaCorta(e.fechaLimitePago)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                when (e.estado) {
                                    "pagado" -> Insignia("Pagado", Semantica.ingreso)
                                    "pendiente" -> Insignia("Pendiente", Semantica.info)
                                    "cancelado" -> Insignia("Cancelado", MaterialTheme.colorScheme.outline)
                                    else -> {}
                                }
                                Spacer(Modifier.size(8.dp))
                                Text(Formato.dinero(e.total), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
    form?.let { f -> estado.comunes?.let { c -> FormularioTarjeta(f, c, d?.periodoAbierto?.desde, viewModel) } }
    if (avance) TransferenciaSheet(
        onCerrar = { avance = false }, onHecho = { avance = false; viewModel.avisar("Avance registrado."); viewModel.cargar() },
        origen = "tarjeta:${viewModel.tarjetaId}", destino = "efectivo"
    )
}

@Composable
private fun FilaMovTarjeta(m: MovimientoTarjeta, quien: String?, onClick: () -> Unit) {
    val abono = m.tipo == "credito"
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(m.contacto?.fotoUrl, m.contacto?.nombre ?: m.categoria?.nombre ?: m.tipoTexto, Formato.color(m.categoria?.color, MaterialTheme.colorScheme.secondaryContainer), tamano = 34.dp)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(m.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(Formato.fechaCorta(m.fecha), m.tipoTexto.takeIf { m.tipo != "compra" }, m.cuotas?.takeIf { it > 1 }?.let { "$it cuotas" }, m.categoria?.nombre, quien)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
            )
        }
        if (!m.editable) Icon(Icons.Default.Lock, contentDescription = "No se modifica aquí", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
        Text((if (abono) "−" else "") + Formato.dinero(m.monto), color = if (abono) Semantica.ingreso else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioTarjeta(f: FormMovTarjeta, comunes: Comunes, desde: String?, vm: TarjetaDetalleViewModel) {
    var confirmar by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = vm::cerrarForm, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (f.id == null) "Nuevo movimiento" else "Editar movimiento", style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val tipos = listOf("compra" to "Compra", "cargo" to "Cargo", "credito" to "Crédito")
                tipos.forEachIndexed { i, (tipo, texto) ->
                    SegmentedButton(selected = f.tipo == tipo, onClick = { vm.cambiar(f.copy(tipo = tipo)) }, shape = SegmentedButtonDefaults.itemShape(i, tipos.size)) { Text(texto) }
                }
            }
            Text(
                when (f.tipo) {
                    "cargo" -> "Intereses, seguro, comisiones o mora que cobra el banco."
                    "credito" -> "Devolución o promoción: baja la deuda."
                    else -> "Con cuotas es una compra a plazos (tasa cero): cada corte cobra una parte."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = f.descripcion, onValueChange = { vm.cambiar(f.copy(descripcion = it)) }, label = { Text("Descripción") },
                isError = f.errores["descripcion"] != null, supportingText = f.errores["descripcion"]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = f.monto, onValueChange = { vm.cambiar(f.copy(monto = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' })) },
                    label = { Text("Monto") }, prefix = { Text("$ ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = f.errores["monto"] != null, supportingText = f.errores["monto"]?.let { { Text(it) } }, modifier = Modifier.weight(1f)
                )
                if (f.tipo == "compra") OutlinedTextField(
                    value = f.cuotas, onValueChange = { vm.cambiar(f.copy(cuotas = it.filter(Char::isDigit).take(2))) },
                    label = { Text("Cuotas") }, placeholder = { Text("Contado") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = f.errores["cuotas"] != null, supportingText = f.errores["cuotas"]?.let { { Text(it) } }, modifier = Modifier.weight(0.7f)
                )
            }
            CampoFecha("Fecha", f.fecha, { vm.cambiar(f.copy(fecha = it)) }, desde = desde, hasta = Formato.hoy(), error = f.errores["fecha"])
            if (f.tipo != "credito") Desplegable(
                "Categoría (opcional)", listOf<Pair<Int?, String>>(null to "Sin categoría") + comunes.categoriasDe("egreso").map { it.id to it.nombre },
                f.categoriaId, { vm.cambiar(f.copy(categoriaId = it)) }, error = f.errores["categoria_personal_id"]
            )
            if (comunes.miembros.size > 1) Desplegable("¿Quién la usó?", comunes.miembros.map { it.id to it.nombre }, f.userId, { vm.cambiar(f.copy(userId = it)) }, error = f.errores["user_id"])
            Desplegable(
                "¿Con quién? (opcional)",
                listOf<Pair<Int?, String>>(null to "Nadie") + comunes.contactos.filter { it.activo || it.id == f.contactoId }.map { it.id to it.nombreCompleto },
                f.contactoId, { vm.cambiar(f.copy(contactoId = it)) }, error = f.errores["contacto_id"]
            )
            OutlinedTextField(value = f.comentario, onValueChange = { vm.cambiar(f.copy(comentario = it)) }, label = { Text("Comentario (opcional)") }, modifier = Modifier.fillMaxWidth())
            f.error?.takeIf { f.errores.isEmpty() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (f.id != null) OutlinedButton(onClick = { confirmar = true }, enabled = !f.guardando) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::cerrarForm) { Text("Cancelar") }
                Button(onClick = vm::guardar, enabled = !f.guardando) {
                    if (f.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text("¿Eliminar este movimiento?") },
        text = { Text("${f.descripcion} · ${Formato.dinero(f.monto)}") },
        confirmButton = { TextButton(onClick = { confirmar = false; vm.eliminar() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
    )
}
