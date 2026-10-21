package com.carlosramos.misgastos.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

/** Colores con significado (iguales a la web): ingreso verde, egreso rojo y los estados. */
object Semantica {
    val ingreso @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4ADE80) else Color(0xFF15803D)
    val egreso @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFB91C1C)
    val aviso @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFB45309)
    val info @Composable get() = if (isSystemInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
}

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorConReintento(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(mensaje, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

@Composable
fun Vacio(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier.fillMaxWidth().padding(32.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Tarjeta con título para agrupar una sección. */
@Composable
fun Seccion(titulo: String, modifier: Modifier = Modifier, accion: (@Composable () -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                accion?.invoke()
            }
            Spacer(Modifier.height(8.dp))
            contenido()
        }
    }
}

/** Fila "etiqueta ...... valor". */
@Composable
fun FilaValor(etiqueta: String, valor: String, colorValor: Color = Color.Unspecified, negrita: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = colorValor, fontWeight = if (negrita) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/** Etiqueta pequeña de estado (siempre con texto, nunca solo color). */
@Composable
fun Insignia(texto: String, color: Color) {
    Text(
        texto,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Logo o foto desde la API; si no hay, las iniciales sobre un círculo de color. */
@Composable
fun Avatar(url: String?, nombre: String, color: Color = MaterialTheme.colorScheme.secondaryContainer, tamano: Dp = 40.dp, cuadrado: Boolean = false) {
    val forma = if (cuadrado) RoundedCornerShape(8.dp) else CircleShape
    val iniciales: @Composable () -> Unit = {
        Box(Modifier.size(tamano).clip(forma).background(color), contentAlignment = Alignment.Center) {
            Text(
                nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                color = Formato.textoSobre(color),
                fontSize = (tamano.value * 0.36f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    val enlace = Formato.urlImagen(url)
    if (enlace == null) {
        iniciales()
    } else {
        SubcomposeAsyncImage(
            model = enlace,
            contentDescription = nombre,
            contentScale = if (cuadrado) ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier.size(tamano).clip(forma).background(Color.White),
            error = { iniciales() },
            loading = { iniciales() }
        )
    }
}

/** Lista desplegable de solo lectura (Material 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <K> Desplegable(
    etiqueta: String,
    opciones: List<Pair<K, String>>,
    seleccion: K?,
    onSeleccion: (K) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = opciones.firstOrNull { it.first == seleccion }?.second.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (clave, texto) ->
                DropdownMenuItem(text = { Text(texto) }, onClick = { onSeleccion(clave); abierto = false })
            }
        }
    }
}

/** Campo de fecha que abre el calendario; solo deja elegir fechas entre [desde] y [hasta] (Y-m-d). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(etiqueta: String, valor: String, onCambio: (String) -> Unit, desde: String?, hasta: String?, error: String? = null) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = Formato.fechaLarga(valor),
        onValueChange = {},
        readOnly = true,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = { IconButton(onClick = { abierto = true }) { Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir fecha") } },
        modifier = Modifier.fillMaxWidth().clickable { abierto = true }
    )
    if (abierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = Formato.aMilis(valor),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dia = Formato.deMilis(utcTimeMillis)
                    return (desde == null || dia >= desde) && (hasta == null || dia <= hasta)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = { estado.selectedDateMillis?.let { onCambio(Formato.deMilis(it)) }; abierto = false }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
}
