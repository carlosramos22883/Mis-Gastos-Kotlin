package com.carlosramos.misgastos.ui.altas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar

/** Pantalla de formulario de alta o edición: barra con volver y eliminar, contenido desplazable y botón Guardar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormulario(
    titulo: String,
    cargando: Boolean,
    guardando: Boolean,
    error: String?,
    onVolver: () -> Unit,
    onGuardar: () -> Unit,
    /** null = no se puede eliminar (registro nuevo) */
    onEliminar: (() -> Unit)? = null,
    queSeElimina: String = "este registro",
    contenido: @Composable ColumnScope.() -> Unit
) {
    var confirmar by remember { mutableStateOf(false) }
    // El error también se avisa abajo: puede venir de eliminar o quedar fuera de la vista al final del formulario
    val aviso = remember { SnackbarHostState() }
    LaunchedEffect(error) { error?.let { aviso.showSnackbar(it) } }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(aviso) },
        topBar = {
            TopAppBar(
                title = { Text(titulo) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (onEliminar != null) IconButton(onClick = { confirmar = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") } }
            )
        }
    ) { relleno ->
        if (cargando) { Cargando(Modifier.padding(relleno)); return@Scaffold }
        Column(
            Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            contenido()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onGuardar, enabled = !guardando, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar")
            }
        }
    }
    if (confirmar && onEliminar != null) Confirmar(
        "¿Eliminar $queSeElimina?", "Solo se puede si no tiene historial; si lo tiene, desactívalo.", "Eliminar",
        onConfirmar = { confirmar = false; onEliminar() }, onCancelar = { confirmar = false }
    )
}

@Composable
fun CampoTexto(etiqueta: String, valor: String, onCambio: (String) -> Unit, error: String? = null, modifier: Modifier = Modifier.fillMaxWidth(), numerico: Boolean = false, apoyo: String? = null) {
    OutlinedTextField(
        value = valor, onValueChange = onCambio, label = { Text(etiqueta) }, singleLine = true,
        keyboardOptions = if (numerico) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        isError = error != null, supportingText = (error ?: apoyo)?.let { { Text(it) } }, modifier = modifier
    )
}

@Composable
fun FilaInterruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit, apoyo: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(texto, style = MaterialTheme.typography.bodyLarge)
            apoyo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}

fun soloNumero(t: String, decimales: Boolean = true) = t.replace(',', '.').filter { it.isDigit() || (decimales && it == '.') }

fun dineroValido(t: String) = Regex("^\\d+(\\.\\d{1,2})?$").matches(t)
