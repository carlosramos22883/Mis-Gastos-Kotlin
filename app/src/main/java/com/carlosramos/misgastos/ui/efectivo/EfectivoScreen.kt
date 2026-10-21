package com.carlosramos.misgastos.ui.efectivo

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carlosramos.misgastos.data.remote.dto.MovimientoEfectivo
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio

@Composable
fun EfectivoScreen(viewModel: EfectivoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (estado.ciclo?.esActual == true) FloatingActionButton(onClick = viewModel::nuevo) { Icon(Icons.Default.Add, contentDescription = "Registrar movimiento") }
        }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Encabezado(estado, viewModel)
            when {
                estado.cargando -> Cargando()
                estado.error != null && estado.movimientos.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() })
                else -> Lista(estado, viewModel)
            }
        }
    }
    form?.let { f -> estado.comunes?.let { c -> FormularioEfectivo(f, c, estado.ciclo?.inicio, viewModel) } }
}

@Composable
private fun Encabezado(estado: EfectivoEstado, vm: EfectivoViewModel) {
    val ciclo = estado.ciclo
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.irACiclo(ciclo?.anterior) }, enabled = ciclo?.anterior != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ciclo anterior")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Efectivo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (ciclo != null) Text(
                    Formato.rango(ciclo.inicio, ciclo.fin) + if (ciclo.esActual) " · ciclo actual" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { vm.irACiclo(ciclo?.siguiente) }, enabled = ciclo?.siguiente != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ciclo siguiente")
            }
        }
        estado.resumen?.let { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Cifra("Ingresos", Formato.dinero(r.ingresos), Modifier.weight(1f))
                Cifra("Egresos", Formato.dinero(r.egresos), Modifier.weight(1f))
                Cifra("Saldo", Formato.dinero(r.saldo), Modifier.weight(1f), negrita = true)
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
private fun Cifra(etiqueta: String, valor: String, modifier: Modifier, negrita: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = if (negrita) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun Lista(estado: EfectivoEstado, vm: EfectivoViewModel) {
    val lista = rememberLazyListState()
    val alFinal by remember { derivedStateOf { lista.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lista.layoutInfo.totalItemsCount - 1 } }
    LaunchedEffect(alFinal) { if (alFinal) vm.cargarMas() }
    // La lista conserva su posición: si llega un movimiento nuevo arriba, se vuelve al inicio para verlo
    LaunchedEffect(estado.movimientos.firstOrNull()?.id) { lista.scrollToItem(0) }
    val miembros = estado.comunes?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }
    if (estado.movimientos.isEmpty()) { Vacio("No hay movimientos en este ciclo."); return }
    LazyColumn(state = lista, contentPadding = PaddingValues(bottom = 88.dp), modifier = Modifier.fillMaxSize()) {
        val porFecha = estado.movimientos.groupBy { it.fecha }
        porFecha.forEach { (fecha, movimientos) ->
            item(key = "f$fecha") {
                Text(
                    Formato.fechaLarga(fecha), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            items(movimientos, key = { it.id }) { m -> FilaMovimiento(m, miembros[m.userId]) { vm.editar(m) } }
        }
        if (estado.cargandoMas) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
    }
}

@Composable
private fun FilaMovimiento(m: MovimientoEfectivo, quien: String?, onClick: () -> Unit) {
    val ingreso = m.tipo == "ingreso"
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (m.contacto == null && m.categoria == null) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Movimiento entre cuentas", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        } else {
            Avatar(m.contacto?.fotoUrl, m.contacto?.nombre ?: m.categoria?.nombre ?: "?", Formato.color(m.categoria?.color), tamano = 36.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(m.descripcion, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(
                listOfNotNull(m.categoria?.nombre, m.contacto?.nombre, quien).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
            )
        }
        if (!m.editable) Icon(Icons.Default.Lock, contentDescription = "Se modifica desde su módulo", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp).padding(end = 2.dp))
        Text(
            (if (ingreso) "+" else "−") + Formato.dinero(m.monto),
            color = if (ingreso) Semantica.ingreso else Semantica.egreso,
            fontWeight = FontWeight.SemiBold
        )
    }
    HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioEfectivo(f: FormEfectivo, comunes: Comunes, cicloInicio: String?, vm: EfectivoViewModel) {
    var confirmarBorrar by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = vm::cerrarForm, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (f.id == null) "Nuevo movimiento" else "Editar movimiento", style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("egreso" to "Egreso", "ingreso" to "Ingreso").forEachIndexed { i, (tipo, texto) ->
                    SegmentedButton(
                        selected = f.tipo == tipo,
                        onClick = { vm.cambiar(f.copy(tipo = tipo, categoriaId = null)) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(texto) }
                }
            }
            OutlinedTextField(
                value = f.descripcion, onValueChange = { vm.cambiar(f.copy(descripcion = it)) }, label = { Text("Descripción") },
                isError = f.errores["descripcion"] != null, supportingText = f.errores["descripcion"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = f.monto, onValueChange = { vm.cambiar(f.copy(monto = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' })) },
                label = { Text("Monto") }, prefix = { Text("$ ") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = f.errores["monto"] != null, supportingText = f.errores["monto"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            CampoFecha("Fecha", f.fecha, { vm.cambiar(f.copy(fecha = it)) }, desde = cicloInicio, hasta = Formato.hoy(), error = f.errores["fecha"])
            Desplegable(
                "Categoría", comunes.categoriasDe(f.tipo).map { it.id to it.nombre }, f.categoriaId,
                { vm.cambiar(f.copy(categoriaId = it)) }, error = f.errores["categoria_personal_id"]
            )
            if (comunes.miembros.size > 1) Desplegable(
                "¿Quién lo hizo?", comunes.miembros.map { it.id to it.nombre }, f.userId, { vm.cambiar(f.copy(userId = it)) }, error = f.errores["user_id"]
            )
            Desplegable(
                "¿Con quién? (opcional)",
                listOf<Pair<Int?, String>>(null to "Nadie") + comunes.contactos.filter { it.activo || it.id == f.contactoId }.map { it.id to it.nombreCompleto },
                f.contactoId, { vm.cambiar(f.copy(contactoId = it)) }, error = f.errores["contacto_id"]
            )
            f.error?.takeIf { f.errores.isEmpty() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                if (f.id != null) OutlinedButton(onClick = { confirmarBorrar = true }, enabled = !f.guardando) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::cerrarForm) { Text("Cancelar") }
                Button(onClick = vm::guardar, enabled = !f.guardando) {
                    if (f.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirmarBorrar) AlertDialog(
        onDismissRequest = { confirmarBorrar = false },
        title = { Text("¿Eliminar este movimiento?") },
        text = { Text("${f.descripcion} · ${Formato.dinero(f.monto)}") },
        confirmButton = { TextButton(onClick = { confirmarBorrar = false; vm.eliminar() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } }
    )
}
