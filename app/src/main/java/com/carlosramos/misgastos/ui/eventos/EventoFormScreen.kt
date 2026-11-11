package com.carlosramos.misgastos.ui.eventos

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.carlosramos.misgastos.data.remote.dto.ConsumoEventoBody
import com.carlosramos.misgastos.data.remote.dto.EventoBody
import com.carlosramos.misgastos.data.remote.dto.OpcionesEvento
import com.carlosramos.misgastos.data.remote.dto.ParticipanteEventoBody
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Imagen
import com.carlosramos.misgastos.ui.common.Seccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.roundToLong

private const val NUEVO = "__nuevo"

data class FilaParticipante(val uid: Int, val quien: String?, val nuevo: String = "", val festejado: Boolean = false)

/** Un producto (columna de la tabla) con su precio; cant = cuántos pidió cada participante (uid → texto). */
data class FilaProducto(val uid: Int, val descripcion: String = "", val precio: String = "", val cant: Map<Int, String> = emptyMap()) {
    fun cantidad(uid: Int) = cant[uid]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
}

/** Lo de cada participante en centavos, con el mismo reparto de EventoService::calcular. */
data class Parte(val consumo: Long, val propina: Long, val festejo: Long, val total: Long)
data class Reparto(val subtotal: Long, val propina: Long, val total: Long, val festejo: Long, val por: Map<Int, Parte>)

object CalculoEvento {
    private fun r6(v: Double) = Math.round(v * 1e6) / 1e6

    /** Reparte [total] centavos según [pesos] sin perder centavos; a igualdad, el que sobra va a quien tenga menos [exceso]. */
    fun repartir(total: Long, pesos: Map<Int, Double>, exceso: Map<Int, Double> = emptyMap()): Map<Int, Long> {
        if (pesos.isEmpty()) return emptyMap()
        val w = if (pesos.values.sum() <= 0) pesos.mapValues { 1.0 } else pesos
        val suma = w.values.sum()
        val partes = LinkedHashMap<Int, Long>(); val frac = HashMap<Int, Double>()
        w.forEach { (k, p) -> val exacto = total * p / suma; val base = floor(exacto + 1e-9).toLong(); partes[k] = base; frac[k] = r6(exacto - base) }
        val orden = w.keys.toList()
        val claves = orden.sortedWith(compareByDescending<Int> { frac[it] }.thenBy { r6(exceso[it] ?: 0.0) }.thenBy { orden.indexOf(it) })
        var sobran = total - partes.values.sum()
        var i = 0
        while (sobran > 0) { val k = claves[i % claves.size]; partes[k] = partes[k]!! + 1; sobran--; i++ }
        return partes
    }

    /** Propina: porcentaje del consumo de cada uno o un monto fijo dividido entre todos. Lo de los festejados se reparte entre los demás. */
    fun calcular(participantes: List<FilaParticipante>, productos: List<FilaProducto>, propinaTipo: String, propinaValor: String): Reparto {
        val uids = participantes.map { it.uid }
        val consumo = uids.associateWith { u -> productos.sumOf { p -> p.cantidad(u) * ((p.precio.toDoubleOrNull() ?: 0.0) * 100).roundToLong() } }
        val subtotal = consumo.values.sum()
        val valor = propinaValor.toDoubleOrNull() ?: 0.0
        val (propina, exactas) = when (propinaTipo) {
            "porcentaje" -> (subtotal * valor / 100).roundToLong().let { p -> p to uids.associateWith { if (subtotal > 0) p * consumo.getValue(it).toDouble() / subtotal else 0.0 } }
            "monto" -> (valor * 100).roundToLong().let { p -> p to uids.associateWith { p.toDouble() / uids.size.coerceAtLeast(1) } }
            else -> 0L to uids.associateWith { 0.0 }
        }
        val propinas = repartir(propina, exactas)
        val festejados = participantes.filter { it.festejado }.map { it.uid }
        val pagan = uids - festejados.toSet()
        val festejo = festejados.sumOf { consumo.getValue(it) + (propinas[it] ?: 0) }
        val exceso = uids.associateWith { (propinas[it] ?: 0) - exactas.getValue(it) }
        val aportes = if (pagan.isEmpty()) emptyMap() else repartir(festejo, pagan.associateWith { 1.0 }, exceso)
        val por = uids.associateWith {
            val c = consumo.getValue(it); val p = propinas[it] ?: 0; val f = aportes[it] ?: 0
            Parte(c, p, f, if (it in festejados) 0 else c + p + f)
        }
        return Reparto(subtotal, propina, subtotal + propina, festejo, por)
    }
}

data class EventoFormEstado(
    val cargando: Boolean = true,
    val opciones: OpcionesEvento? = null,
    val comunes: Comunes? = null,
    val nombre: String = "",
    val fecha: String = Formato.hoy(),
    val lugar: String = "",
    val participantes: List<FilaParticipante> = emptyList(),
    val pagador: Int? = null,
    val productos: List<FilaProducto> = emptyList(),
    val propinaTipo: String = "porcentaje",
    val propinaValor: String = "10",
    val origen: String? = null,
    val categoriaId: Int? = null,
    val fotoUrl: String? = null,
    val fotoNueva: ByteArray? = null,
    val quitarFoto: Boolean = false,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null,
    val confirmar: String? = null,
    val listo: Int? = null
) {
    /** Pagó un miembro del hogar: se registra el pago y los contactos quedan debiendo. */
    val pagaElHogar get() = participantes.firstOrNull { it.uid == pagador }?.quien?.startsWith("miembro:") == true
    val reparto get() = CalculoEvento.calcular(participantes, productos, propinaTipo, propinaValor)
}

@HiltViewModel
class EventoFormViewModel @Inject constructor(private val repo: FinanzasRepository, estadoGuardado: SavedStateHandle) : ViewModel() {
    private val id: Int? = (estadoGuardado.get<Int>("id") ?: 0).takeIf { it > 0 }
    private val _estado = MutableStateFlow(EventoFormEstado())
    val estado = _estado.asStateFlow()
    private var siguiente = 1000
    val editando get() = id != null

    init { cargar() }

    private fun cargar() = viewModelScope.launch {
        val comunes = (repo.comunes() as? Resultado.Ok)?.datos
        val opciones = (repo.opcionesEvento() as? Resultado.Ok)?.datos
        val base = EventoFormEstado(cargando = false, opciones = opciones, comunes = comunes, origen = opciones?.origenes?.firstOrNull()?.valor)
        val e = id?.let { (repo.evento(it) as? Resultado.Ok)?.datos }
        _estado.value = if (e == null) {
            base.copy(participantes = listOf(FilaParticipante(1, comunes?.let { "miembro:${it.usuarioId}" })), pagador = 1, productos = listOf(FilaProducto(2)))
        } else {
            val uids = e.participantes.map { it.id }
            base.copy(
                nombre = e.nombre, fecha = e.fecha, lugar = e.lugar.orEmpty(),
                participantes = e.participantes.map { FilaParticipante(it.id, it.quien, festejado = it.esFestejado) }, pagador = e.pagador?.id,
                productos = e.consumos.map { c -> FilaProducto(-c.id, c.descripcion, c.precio, uids.zip(c.cantidades).filter { it.second > 0 }.associate { (u, n) -> u to n.toString() }) },
                propinaTipo = e.propinaTipo, propinaValor = if (e.propinaTipo == "ninguna") "" else e.propinaValor,
                origen = e.origenValor ?: base.origen, categoriaId = e.categoriaId, fotoUrl = e.fotoUrl
            )
        }
    }

    fun cambiar(f: (EventoFormEstado) -> EventoFormEstado) { _estado.value = f(_estado.value) }
    fun agregarParticipante() = cambiar { it.copy(participantes = it.participantes + FilaParticipante(siguiente++, null)) }
    fun quitarParticipante(uid: Int) = cambiar { e ->
        val resto = e.participantes.filter { it.uid != uid }
        e.copy(participantes = resto, productos = e.productos.map { it.copy(cant = it.cant - uid) }, pagador = if (e.pagador == uid) resto.firstOrNull()?.uid else e.pagador)
    }
    fun participante(p: FilaParticipante) = cambiar { e -> e.copy(participantes = e.participantes.map { if (it.uid == p.uid) p else it }) }
    fun agregarProducto() = cambiar { it.copy(productos = it.productos + FilaProducto(siguiente++)) }
    fun quitarProducto(uid: Int) = cambiar { e -> e.copy(productos = e.productos.filter { it.uid != uid }) }
    fun producto(c: FilaProducto) = cambiar { e -> e.copy(productos = e.productos.map { if (it.uid == c.uid) c else it }) }
    fun cantidad(producto: Int, persona: Int, texto: String) = cambiar { e ->
        e.copy(productos = e.productos.map { if (it.uid == producto) it.copy(cant = it.cant + (persona to texto.filter(Char::isDigit).take(3))) else it })
    }

    fun guardar(confirmado: Boolean = false) {
        val e = _estado.value
        val errores = buildMap {
            if (e.nombre.isBlank()) put("nombre", "Escribe el nombre del evento.")
            if (e.participantes.any { quien(it).isBlank() }) put("participantes", "Elige a cada persona (o escribe el nombre del nuevo contacto).")
            if (e.participantes.isNotEmpty() && e.participantes.all { it.festejado }) put("participantes", "Al menos una persona debe pagar: no pueden ser todos festejados.")
            if (e.productos.isEmpty()) put("consumos", "Agrega al menos un producto.")
            e.productos.firstOrNull { it.descripcion.isBlank() || !Formato.montoValido(it.precio) }?.let { put("consumos", "Cada producto necesita nombre y precio.") }
            e.productos.firstOrNull { p -> e.participantes.sumOf { p.cantidad(it.uid) } == 0 }?.let { put("consumos", "Nadie pidió «${it.descripcion}»: anota cuántos pidió cada uno o quítalo.") }
            if (e.propinaTipo != "ninguna" && e.propinaValor.toDoubleOrNull() == null) put("propina_valor", "Escribe la propina.")
            if (e.pagaElHogar && e.origen != "ninguno" && e.categoriaId == null) put("categoria_personal_id", "Elige la categoría del gasto.")
        }
        if (errores.isNotEmpty()) { cambiar { it.copy(errores = errores, error = errores.values.first()) }; return }
        val posicion = e.participantes.map { it.uid }
        val body = EventoBody(
            nombre = e.nombre.trim(), fecha = e.fecha, lugar = e.lugar.trim().ifBlank { null },
            propinaTipo = e.propinaTipo, propinaValor = if (e.propinaTipo == "ninguna") null else e.propinaValor,
            participantes = e.participantes.map { ParticipanteEventoBody(quien(it), it.festejado) },
            pagador = posicion.indexOf(e.pagador).coerceAtLeast(0),
            consumos = e.productos.map { c -> ConsumoEventoBody(c.descripcion.trim(), c.precio, posicion.map { c.cantidad(it) }) },
            origen = if (e.pagaElHogar) e.origen else null, categoriaId = if (e.pagaElHogar && e.origen != "ninguno") e.categoriaId else null,
            confirmar = if (confirmado) true else null
        )
        cambiar { it.copy(guardando = true, errores = emptyMap(), error = null, confirmar = null) }
        viewModelScope.launch {
            when (val r = repo.guardarEvento(id, body)) {
                is Resultado.Ok -> {
                    // La foto de la cuenta va aparte (multipart); si falla, el evento ya quedó guardado
                    val foto = when {
                        e.fotoNueva != null -> repo.subirFotoEvento(r.datos.id, e.fotoNueva)
                        e.quitarFoto && e.fotoUrl != null -> repo.quitarFotoEvento(r.datos.id)
                        else -> null
                    }
                    if (foto is Resultado.Falla) cambiar { it.copy(guardando = false, error = "El evento se guardó, pero la foto no: ${foto.mensaje}", listo = r.datos.id) }
                    else cambiar { it.copy(guardando = false, listo = r.datos.id) }
                }
                is Resultado.Falla -> {
                    val pregunta = r.campos["confirmar"]
                    cambiar { it.copy(guardando = false, errores = r.campos - "confirmar", error = if (pregunta != null) null else (r.campos.values.firstOrNull() ?: r.mensaje), confirmar = pregunta) }
                }
            }
        }
    }

    private fun quien(p: FilaParticipante) = if (p.quien == NUEVO) p.nuevo.trim().let { if (it.isBlank()) "" else "nuevo:$it" } else p.quien.orEmpty()
}

private val ANCHO_NOMBRE = 120.dp
private val ANCHO_PRODUCTO = 76.dp
private val ANCHO_RESUMEN = 92.dp
private val ALTO_FILA = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoFormScreen(onListo: (Int) -> Unit, onVolver: () -> Unit, viewModel: EventoFormViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    LaunchedEffect(e.listo) { e.listo?.let(onListo) }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { Imagen.reducida(contexto, it) }?.let { jpeg -> viewModel.cambiar { s -> s.copy(fotoNueva = jpeg, quitarFoto = false) } }
    }
    val opciones = e.opciones?.participantes.orEmpty().map { it.valor to it.etiqueta } + (NUEVO to "+ Nuevo contacto…")
    fun nombre(p: FilaParticipante) = if (p.quien == NUEVO) p.nuevo.ifBlank { "Nuevo" } else opciones.firstOrNull { it.first == p.quien }?.second?.substringBefore(" (") ?: "¿Quién?"
    val reparto = e.reparto
    val hayPropina = e.propinaTipo != "ninguna"
    val hayFestejo = e.participantes.any { it.festejado }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.editando) "Editar evento" else "Nuevo evento") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp).imePadding(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total ${Formato.dinero(reparto.total / 100.0)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Consumo ${Formato.dinero(reparto.subtotal / 100.0)} · Propina ${Formato.dinero(reparto.propina / 100.0)}", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { viewModel.guardar() }, enabled = !e.guardando && !e.cargando) {
                    if (e.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar")
                }
            }
        }
    ) { relleno ->
        if (e.cargando) { Cargando(Modifier.padding(relleno)); return@Scaffold }
        Column(Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            e.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Seccion("¿Qué celebraron?") {
                OutlinedTextField(e.nombre, { v -> viewModel.cambiar { it.copy(nombre = v) } }, label = { Text("Nombre (ej. Cumpleaños de Rocío)") }, singleLine = true,
                    isError = e.errores["nombre"] != null, modifier = Modifier.fillMaxWidth())
                CampoFecha("Fecha", e.fecha, { v -> viewModel.cambiar { it.copy(fecha = v) } }, desde = null, hasta = Formato.hoy(), error = e.errores["fecha"])
                OutlinedTextField(e.lugar, { v -> viewModel.cambiar { it.copy(lugar = v) } }, label = { Text("Lugar (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            Seccion("¿Quiénes estuvieron?", accion = { TextButton(onClick = viewModel::agregarParticipante) { Icon(Icons.Default.Add, null); Text("Persona") } }) {
                e.participantes.forEach { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Desplegable("Persona", opciones, p.quien, { viewModel.participante(p.copy(quien = it)) }, Modifier.weight(1f))
                                if (e.participantes.size > 1) IconButton(onClick = { viewModel.quitarParticipante(p.uid) }) { Icon(Icons.Default.Close, contentDescription = "Quitar") }
                            }
                            if (p.quien == NUEVO) OutlinedTextField(p.nuevo, { viewModel.participante(p.copy(nuevo = it)) }, label = { Text("Nombre del nuevo contacto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(p.festejado, { viewModel.participante(p.copy(festejado = it)) }); Text("Festejado")
                                RadioButton(e.pagador == p.uid, { viewModel.cambiar { it.copy(pagador = p.uid) } }); Text("Pagó la cuenta")
                            }
                        }
                    }
                }
                Text("Un festejado no paga: lo suyo se reparte entre los demás.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Seccion("Productos", accion = { TextButton(onClick = viewModel::agregarProducto) { Icon(Icons.Default.Add, null); Text("Producto") } }) {
                e.productos.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(c.descripcion, { viewModel.producto(c.copy(descripcion = it)) }, label = { Text("Producto") }, singleLine = true, modifier = Modifier.weight(1.4f))
                        CampoMonto("Precio", c.precio, { viewModel.producto(c.copy(precio = it)) }, Modifier.weight(1f))
                        if (e.productos.size > 1) IconButton(onClick = { viewModel.quitarProducto(c.uid) }) { Icon(Icons.Default.Close, contentDescription = "Quitar") }
                    }
                }
            }

            // Como la hoja: una fila por persona, una columna por producto; en cada celda, cuántos pidió
            Seccion("¿Cuántos pidió cada uno?") {
                e.errores["consumos"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Row {
                    Column(Modifier.width(ANCHO_NOMBRE)) {
                        Box(Modifier.height(ALTO_FILA + 8.dp))
                        e.participantes.forEach { p -> CeldaTexto(nombre(p) + if (p.festejado) " 🎂" else "", ANCHO_NOMBRE, alineado = TextAlign.Start) }
                        CeldaTexto("Total", ANCHO_NOMBRE, alineado = TextAlign.Start, negrita = true)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        e.productos.forEach { c ->
                            Column(Modifier.width(ANCHO_PRODUCTO)) {
                                Column(Modifier.height(ALTO_FILA + 8.dp).padding(horizontal = 4.dp), verticalArrangement = Arrangement.Bottom) {
                                    Text(c.descripcion.ifBlank { "Producto" }, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(Formato.dinero(c.precio), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                e.participantes.forEach { p -> CeldaCantidad(c.cant[p.uid].orEmpty()) { viewModel.cantidad(c.uid, p.uid, it) } }
                                CeldaTexto(e.participantes.sumOf { c.cantidad(it.uid) }.takeIf { it > 0 }?.toString() ?: "", ANCHO_PRODUCTO, negrita = true)
                            }
                        }
                        Resumen("Total", e.participantes.map { reparto.por[it.uid]?.consumo }, reparto.subtotal)
                        if (hayPropina) {
                            Resumen("Propina", e.participantes.map { reparto.por[it.uid]?.propina }, reparto.propina)
                            Resumen("Con propina", e.participantes.map { reparto.por[it.uid]?.let { r -> r.consumo + r.propina } }, reparto.total)
                        }
                        if (hayFestejo) Resumen("Cumpleañero", e.participantes.map { p -> if (p.festejado) null else reparto.por[p.uid]?.festejo }, reparto.festejo)
                        Resumen("A pagar", e.participantes.map { p -> if (p.festejado) null else reparto.por[p.uid]?.total }, reparto.total, negrita = true)
                    }
                }
            }

            Seccion("Propina") {
                Desplegable("Propina", listOf("ninguna" to "Sin propina", "porcentaje" to "Porcentaje del total de cada uno", "monto" to "Monto fijo dividido entre todos"), e.propinaTipo,
                    { v -> viewModel.cambiar { it.copy(propinaTipo = v) } })
                if (hayPropina) OutlinedTextField(e.propinaValor, { v -> viewModel.cambiar { it.copy(propinaValor = v.replace(',', '.').filter { c -> c.isDigit() || c == '.' }) } },
                    label = { Text(if (e.propinaTipo == "porcentaje") "Porcentaje (%)" else "Monto ($)") }, singleLine = true, isError = e.errores["propina_valor"] != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }

            Seccion("Foto de la cuenta (opcional)") {
                Text("Se comparte junto con la tabla, para que todos vean el total del restaurante.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val nueva = remember(e.fotoNueva) { e.fotoNueva?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
                when {
                    nueva != null -> Image(nueva, "Foto de la cuenta", Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit)
                    e.fotoUrl != null && !e.quitarFoto -> AsyncImage(Formato.urlImagen(e.fotoUrl), "Foto de la cuenta", Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(if (e.fotoNueva != null || (e.fotoUrl != null && !e.quitarFoto)) "Cambiar foto" else "Elegir foto")
                    }
                    if (e.fotoNueva != null || (e.fotoUrl != null && !e.quitarFoto)) TextButton(onClick = { viewModel.cambiar { it.copy(fotoNueva = null, quitarFoto = true) } }) { Text("Quitar") }
                }
            }

            if (e.pagaElHogar) Seccion("¿Con qué se pagó la cuenta?") {
                Desplegable("Pagado desde", e.opciones?.origenes.orEmpty().map { it.valor to it.etiqueta }, e.origen, { v -> viewModel.cambiar { it.copy(origen = v) } }, error = e.errores["origen"])
                if (e.origen != "ninguno") Desplegable("Categoría del gasto", e.comunes?.categoriasDe("egreso").orEmpty().map { it.id to it.nombre }, e.categoriaId,
                    { v -> viewModel.cambiar { it.copy(categoriaId = v) } }, error = e.errores["categoria_personal_id"])
                Text("Un solo movimiento por el total. Lo de cada contacto queda en \"Nos deben\" y, al cobrarle, el dinero entra solo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else Text("Pagó otra persona: en el evento podrás marcar quién ya le pagó (y registrar tu parte).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    e.confirmar?.let { texto -> Confirmar("¿Registrar de todas formas?", texto, "Sí, registrar", { viewModel.guardar(confirmado = true) }, { viewModel.cambiar { it.copy(confirmar = null) } }) }
}

@Composable
private fun CeldaTexto(texto: String, ancho: Dp, alineado: TextAlign = TextAlign.End, negrita: Boolean = false) {
    Box(Modifier.width(ancho).height(ALTO_FILA).padding(horizontal = 4.dp), contentAlignment = if (alineado == TextAlign.Start) Alignment.CenterStart else Alignment.CenterEnd) {
        Text(texto, style = MaterialTheme.typography.bodySmall, fontWeight = if (negrita) FontWeight.Bold else null, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = alineado)
    }
}

/** Celda de cantidad: un número pequeño, centrado. */
@Composable
private fun CeldaCantidad(valor: String, onCambio: (String) -> Unit) {
    Box(Modifier.width(ANCHO_PRODUCTO).height(ALTO_FILA).padding(4.dp), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = valor, onValueChange = onCambio, singleLine = true,
            textStyle = TextStyle(textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyMedium.fontSize),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxSize().border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(6.dp)).padding(top = 10.dp)
        )
    }
}

/** Columna calculada (total, propina, lo del cumpleañero, a pagar) en centavos; null = no aplica (festejado). */
@Composable
private fun Resumen(titulo: String, valores: List<Long?>, total: Long, negrita: Boolean = false) {
    Column(Modifier.width(ANCHO_RESUMEN)) {
        Box(Modifier.height(ALTO_FILA + 8.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.BottomEnd) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
        valores.forEach { v -> CeldaTexto(if (v == null) "—" else Formato.dinero(v / 100.0), ANCHO_RESUMEN, negrita = negrita) }
        CeldaTexto(Formato.dinero(total / 100.0), ANCHO_RESUMEN, negrita = true)
    }
}
