package com.carlosramos.misgastos.ui.compromisos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.CompromisoBody
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.Formato
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class FormCompromiso(
    val id: Int? = null,
    val descripcion: String = "",
    val monto: String = "",
    val medioPago: String = "por_definir",
    val categoriaId: Int? = null,
    val contactoId: Int? = null,
    val userId: Int? = null,
    val comentario: String = "",
    val recurrente: Boolean = false,
    val fechaEsperada: String = Formato.hoy(),
    val frecuencia: String = "mensual",
    val diaSemana: Int = 1,
    val diaPago: String = LocalDate.now().dayOfMonth.toString(),
    val diaPago2: String = "",
    val fechaInicio: String = Formato.hoy(),
    val finalizacion: String = "indefinido",
    val fechaFin: String = LocalDate.now().plusYears(1).toString(),
    val totalCuotas: String = "",
    val guardando: Boolean = false,
    val listo: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class FormCompromisoEstado(val cargando: Boolean = true, val comunes: Comunes? = null, val form: FormCompromiso = FormCompromiso())

@HiltViewModel
class CompromisoFormViewModel @Inject constructor(
    private val repo: FinanzasRepository,
    estadoGuardado: SavedStateHandle
) : ViewModel() {
    /** 0 = nuevo; si no, se edita ese pago (no la regla de la serie). */
    private val id: Int = estadoGuardado["id"] ?: 0
    private val _estado = MutableStateFlow(FormCompromisoEstado())
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val comunes = (repo.comunes() as? Resultado.Ok)?.datos
            var form = FormCompromiso(userId = comunes?.usuarioId)
            if (id != 0) (repo.compromiso(id) as? Resultado.Ok)?.datos?.let { c ->
                form = FormCompromiso(
                    id = c.id, descripcion = c.descripcion, monto = c.monto, medioPago = c.medioPago ?: "por_definir", categoriaId = c.categoria?.id,
                    contactoId = c.contacto?.id, userId = c.userId, comentario = c.comentario.orEmpty(), fechaEsperada = c.fechaEsperada ?: Formato.hoy()
                )
            }
            _estado.value = FormCompromisoEstado(cargando = false, comunes = comunes, form = form)
        }
    }

    fun cambiar(f: FormCompromiso) { _estado.value = _estado.value.copy(form = f) }

    fun guardar() {
        val f = _estado.value.form
        val dia = { t: String -> t.toIntOrNull()?.takeIf { it in 1..31 } }
        val errores = buildMap {
            if (f.descripcion.isBlank()) put("descripcion", "Escribe qué hay que pagar.")
            if (!Formato.montoValido(f.monto)) put("monto", "Monto positivo con máximo dos decimales.")
            if (f.recurrente) {
                if (f.frecuencia != "semanal" && dia(f.diaPago) == null) put("dia_pago", "Día del 1 al 31 (31 = último día del mes).")
                if (f.frecuencia == "quincenal" && (dia(f.diaPago2) == null || f.diaPago2 == f.diaPago)) put("dia_pago_2", "Otro día del 1 al 31.")
                if (f.finalizacion == "cuotas" && (f.totalCuotas.toIntOrNull() ?: 0) !in 1..600) put("total_cuotas", "Entre 1 y 600.")
                if (f.finalizacion == "fecha" && f.fechaFin < f.fechaInicio) put("fecha_fin", "Debe ser después del inicio.")
            }
        }
        if (errores.isNotEmpty()) { cambiar(f.copy(errores = errores)); return }
        cambiar(f.copy(guardando = true, errores = emptyMap(), error = null))
        viewModelScope.launch {
            val body = CompromisoBody(
                descripcion = f.descripcion.trim(), monto = f.monto, medioPago = f.medioPago, categoriaId = f.categoriaId,
                comentario = f.comentario.trim().ifBlank { null }, contactoId = f.contactoId?.toString(), userId = f.userId,
                recurrenciaTipo = if (f.id != null) null else if (f.recurrente) "recurrente" else "unica",
                fechaEsperada = if (f.recurrente && f.id == null) null else f.fechaEsperada,
                frecuencia = f.frecuencia.takeIf { f.recurrente },
                diaSemana = f.diaSemana.takeIf { f.recurrente && f.frecuencia == "semanal" },
                diaPago = f.diaPago.toIntOrNull().takeIf { f.recurrente && f.frecuencia != "semanal" },
                diaPago2 = f.diaPago2.toIntOrNull().takeIf { f.recurrente && f.frecuencia == "quincenal" },
                fechaInicio = f.fechaInicio.takeIf { f.recurrente },
                finalizacion = f.finalizacion.takeIf { f.recurrente },
                fechaFin = f.fechaFin.takeIf { f.recurrente && f.finalizacion == "fecha" },
                totalCuotas = f.totalCuotas.toIntOrNull().takeIf { f.recurrente && f.finalizacion == "cuotas" }
            )
            val r = if (f.id == null) repo.crearCompromiso(body) else repo.editarCompromiso(f.id, body)
            when (r) {
                is Resultado.Ok -> cambiar(f.copy(guardando = false, listo = true))
                is Resultado.Falla -> cambiar(f.copy(guardando = false, errores = r.campos, error = r.mensaje))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompromisoFormScreen(onListo: () -> Unit, onVolver: () -> Unit, viewModel: CompromisoFormViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val f = estado.form
    LaunchedEffect(f.listo) { if (f.listo) onListo() }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (f.id == null) "Nuevo compromiso" else "Editar pago") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }
            )
        }
    ) { relleno ->
        val c = estado.comunes
        if (estado.cargando || c == null) { Cargando(Modifier.padding(relleno)); return@Scaffold }
        Column(
            Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (f.id == null) SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false to "Una sola vez", true to "Se repite").forEachIndexed { i, (rec, texto) ->
                    SegmentedButton(selected = f.recurrente == rec, onClick = { viewModel.cambiar(f.copy(recurrente = rec)) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(texto) }
                }
            }
            OutlinedTextField(
                value = f.descripcion, onValueChange = { viewModel.cambiar(f.copy(descripcion = it)) }, label = { Text("¿Qué hay que pagar?") },
                placeholder = { Text("Ej: Internet, colegio de Gaby, cuota del carro…") },
                isError = f.errores["descripcion"] != null, supportingText = f.errores["descripcion"]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth()
            )
            CampoMonto(if (f.recurrente) "Monto de cada pago" else "Monto", f.monto, { viewModel.cambiar(f.copy(monto = it)) }, error = f.errores["monto"])

            if (!f.recurrente || f.id != null) {
                CampoFecha("Fecha esperada", f.fechaEsperada, { viewModel.cambiar(f.copy(fechaEsperada = it)) }, desde = null, hasta = null, error = f.errores["fecha_esperada"])
            } else {
                Desplegable("Frecuencia", listOf("mensual" to "Mensual", "quincenal" to "Quincenal", "semanal" to "Semanal"), f.frecuencia, { viewModel.cambiar(f.copy(frecuencia = it)) }, error = f.errores["frecuencia"])
                when (f.frecuencia) {
                    "semanal" -> Desplegable(
                        "Día de la semana",
                        listOf(1 to "Lunes", 2 to "Martes", 3 to "Miércoles", 4 to "Jueves", 5 to "Viernes", 6 to "Sábado", 0 to "Domingo"),
                        f.diaSemana, { viewModel.cambiar(f.copy(diaSemana = it)) }, error = f.errores["dia_semana"]
                    )
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoDia(if (f.frecuencia == "quincenal") "Primer día" else "Día de pago", f.diaPago, { viewModel.cambiar(f.copy(diaPago = it)) }, f.errores["dia_pago"], Modifier.weight(1f))
                        if (f.frecuencia == "quincenal") CampoDia("Segundo día", f.diaPago2, { viewModel.cambiar(f.copy(diaPago2 = it)) }, f.errores["dia_pago_2"], Modifier.weight(1f))
                    }
                }
                Text("31 = último día de cada mes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CampoFecha("Desde", f.fechaInicio, { viewModel.cambiar(f.copy(fechaInicio = it)) }, desde = null, hasta = null, error = f.errores["fecha_inicio"])
                Desplegable(
                    "¿Hasta cuándo?", listOf("indefinido" to "Sin fecha de fin", "fecha" to "Hasta una fecha", "cuotas" to "Un número de pagos"),
                    f.finalizacion, { viewModel.cambiar(f.copy(finalizacion = it)) }, error = f.errores["finalizacion"]
                )
                when (f.finalizacion) {
                    "fecha" -> CampoFecha("Hasta", f.fechaFin, { viewModel.cambiar(f.copy(fechaFin = it)) }, desde = f.fechaInicio, hasta = null, error = f.errores["fecha_fin"])
                    "cuotas" -> OutlinedTextField(
                        value = f.totalCuotas, onValueChange = { viewModel.cambiar(f.copy(totalCuotas = it.filter(Char::isDigit).take(3))) },
                        label = { Text("Número de pagos") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = f.errores["total_cuotas"] != null, supportingText = f.errores["total_cuotas"]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Desplegable(
                "¿Cómo se paga?", listOf("por_definir" to "Por definir", "efectivo" to "Efectivo", "transferencia" to "Transferencia", "tarjeta" to "Tarjeta"),
                f.medioPago, { viewModel.cambiar(f.copy(medioPago = it)) }, error = f.errores["medio_pago"]
            )
            Desplegable(
                "Categoría", listOf<Pair<Int?, String>>(null to "Se elige al pagar") + c.categoriasDe("egreso").map { it.id to it.nombre },
                f.categoriaId, { viewModel.cambiar(f.copy(categoriaId = it)) }, error = f.errores["categoria_personal_id"]
            )
            Desplegable(
                "¿A quién? (opcional)", listOf<Pair<Int?, String>>(null to "Nadie") + c.contactos.filter { it.activo || it.id == f.contactoId }.map { it.id to it.nombreCompleto },
                f.contactoId, { viewModel.cambiar(f.copy(contactoId = it)) }, error = f.errores["contacto_id"]
            )
            if (c.miembros.size > 1) Desplegable("¿Quién lo registra?", c.miembros.map { it.id to it.nombre }, f.userId, { viewModel.cambiar(f.copy(userId = it)) }, error = f.errores["user_id"])
            OutlinedTextField(value = f.comentario, onValueChange = { viewModel.cambiar(f.copy(comentario = it)) }, label = { Text("Comentario (opcional)") }, modifier = Modifier.fillMaxWidth())
            if (f.id != null) Text("Solo cambia este pago; los demás de la serie siguen igual.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            f.error?.takeIf { f.errores.isEmpty() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::guardar, enabled = !f.guardando, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (f.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar")
            }
        }
    }
}

@Composable
private fun CampoDia(etiqueta: String, valor: String, onCambio: (String) -> Unit, error: String?, modifier: Modifier) {
    OutlinedTextField(
        value = valor, onValueChange = { onCambio(it.filter(Char::isDigit).take(2)) }, label = { Text(etiqueta) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = error != null, supportingText = error?.let { { Text(it) } }, modifier = modifier
    )
}
