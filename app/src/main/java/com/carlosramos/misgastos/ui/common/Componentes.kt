package com.carlosramos.misgastos.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll
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
fun FilaValor(etiqueta: String, valor: String, colorValor: Color = Color.Unspecified, negrita: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

/** Campo de dinero: solo dígitos y punto, teclado decimal y prefijo "$". */
@Composable
fun CampoMonto(etiqueta: String, valor: String, onCambio: (String) -> Unit, modifier: Modifier = Modifier, error: String? = null, apoyo: String? = null) {
    OutlinedTextField(
        value = valor, onValueChange = { onCambio(it.replace(',', '.').filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(etiqueta) }, prefix = { Text("$ ") }, singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        isError = error != null, supportingText = (error ?: apoyo)?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth()
    )
}

/** Hoja inferior con título, contenido desplazable y botones Cancelar / acción principal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaFormulario(
    titulo: String,
    accion: String,
    guardando: Boolean,
    onCerrar: () -> Unit,
    onAccion: () -> Unit,
    error: String? = null,
    extra: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onCerrar, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp)
                .then(Modifier.imePadding().navigationBarsPadding()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            contenido()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                extra?.invoke()
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCerrar) { Text("Cancelar") }
                Button(onClick = onAccion, enabled = !guardando) {
                    if (guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(accion)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Confirmación simple para acciones que borran algo. */
@Composable
fun Confirmar(titulo: String, texto: String, accion: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(accion, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Nombre de un origen/destino ("efectivo", "cuenta:42", "tarjeta:7", "planilla", "ninguno"). */
fun nombreOrigen(valor: String?, cajas: Map<String, String> = emptyMap()): String = when {
    valor == null -> ""
    valor == "efectivo" -> "Efectivo"
    valor == "planilla" -> "Descuento de planilla"
    valor == "ninguno" -> "Solo anotado"
    valor == "transferencia" -> "Transferencia"
    else -> cajas[valor] ?: valor.substringBefore(":").replaceFirstChar { it.uppercase() }
}

/** Colores para cuentas, tarjetas y préstamos (los de la web). */
val PALETA = listOf(
    "#0369A1", "#0284C7", "#1155CC", "#4A86E8", "#7C3AED", "#C026D3", "#FF00FF", "#DB2777", "#DC2626", "#EA580C",
    "#F1C232", "#FFD966", "#16A34A", "#00A651", "#B6D7A8", "#0891B2", "#434343", "#999999", "#1E293B", "#92400E"
)

/** Fila de muestras de color; la elegida lleva un borde y un ✓ (no solo el color). */
@Composable
fun SelectorColor(seleccion: String, onSeleccion: (String) -> Unit) {
    Column {
        Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PALETA.forEach { hex ->
                val c = Formato.color(hex)
                val elegido = hex.equals(seleccion, ignoreCase = true)
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c)
                        .then(if (elegido) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable { onSeleccion(hex) },
                    contentAlignment = Alignment.Center
                ) { if (elegido) Text("✓", color = Formato.textoSobre(c), fontWeight = FontWeight.Bold) }
            }
        }
    }
}
