package com.carlosramos.misgastos.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlosramos.misgastos.data.repository.Comunes

/** Formulario de un movimiento (nuevo si id == null). */
data class FormMovimiento(
    val id: Int? = null,
    val tipo: String = "egreso",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: String = Formato.hoy(),
    val categoriaId: Int? = null,
    val userId: Int? = null,
    val contactoId: Int? = null,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

/** Formulario de ingreso/egreso de efectivo o de una cuenta. [desde]: primera fecha permitida (inicio del ciclo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioMovimiento(
    f: FormMovimiento,
    comunes: Comunes,
    desde: String?,
    onCambiar: (FormMovimiento) -> Unit,
    onGuardar: () -> Unit,
    onEliminar: () -> Unit,
    onCerrar: () -> Unit
) {
    var confirmarBorrar by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (f.id == null) "Nuevo movimiento" else "Editar movimiento", style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("egreso" to "Egreso", "ingreso" to "Ingreso").forEachIndexed { i, (tipo, texto) ->
                    SegmentedButton(
                        selected = f.tipo == tipo,
                        onClick = { onCambiar(f.copy(tipo = tipo, categoriaId = null)) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(texto) }
                }
            }
            OutlinedTextField(
                value = f.descripcion, onValueChange = { onCambiar(f.copy(descripcion = it)) }, label = { Text("Descripción") },
                isError = f.errores["descripcion"] != null, supportingText = f.errores["descripcion"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = f.monto, onValueChange = { onCambiar(f.copy(monto = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' })) },
                label = { Text("Monto") }, prefix = { Text("$ ") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = f.errores["monto"] != null, supportingText = f.errores["monto"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            CampoFecha("Fecha", f.fecha, { onCambiar(f.copy(fecha = it)) }, desde = desde, hasta = Formato.hoy(), error = f.errores["fecha"])
            Desplegable(
                "Categoría", comunes.categoriasDe(f.tipo).map { it.id to it.nombre }, f.categoriaId,
                { onCambiar(f.copy(categoriaId = it)) }, error = f.errores["categoria_personal_id"]
            )
            if (comunes.miembros.size > 1) Desplegable(
                "¿Quién lo hizo?", comunes.miembros.map { it.id to it.nombre }, f.userId, { onCambiar(f.copy(userId = it)) }, error = f.errores["user_id"]
            )
            Desplegable(
                "¿Con quién? (opcional)",
                listOf<Pair<Int?, String>>(null to "Nadie") + comunes.contactos.filter { it.activo || it.id == f.contactoId }.map { it.id to it.nombreCompleto },
                f.contactoId, { onCambiar(f.copy(contactoId = it)) }, error = f.errores["contacto_id"]
            )
            f.error?.takeIf { f.errores.isEmpty() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                if (f.id != null) OutlinedButton(onClick = { confirmarBorrar = true }, enabled = !f.guardando) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCerrar) { Text("Cancelar") }
                Button(onClick = onGuardar, enabled = !f.guardando) {
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
        confirmButton = { TextButton(onClick = { confirmarBorrar = false; onEliminar() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } }
    )
}
