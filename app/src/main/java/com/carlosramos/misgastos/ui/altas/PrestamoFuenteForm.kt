package com.carlosramos.misgastos.ui.altas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.CargoBody
import com.carlosramos.misgastos.data.remote.dto.FuenteBody
import com.carlosramos.misgastos.data.remote.dto.PrestamoBody
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.SelectorColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ======================= Préstamo =======================

data class FormPrestamo(
    val id: Int? = null,
    val nombre: String = "",
    val tipoId: Int? = null,
    val bancoId: Int? = null,
    val acreedor: String = "",
    val contactoId: Int? = null,
    val montoOriginal: String = "",
    val tasa: String = "",
    val plazo: String = "",
    val fechaOtorgamiento: String = Formato.hoy(),
    val diaPago: String = "",
    val cuota: String = "",
    val saldoInicial: String = "",
    val fechaSaldo: String = Formato.hoy(),
    val medioPago: String = "transferencia",
    val montoPlanilla: String = "",
    val cargos: List<Pair<String, String>> = emptyList(),
    val color: String = "#7C3AED",
    val notas: String = "",
    val activo: Boolean = true,
    val crearCompromiso: Boolean = true,
    val tienePagos: Boolean = false,
    val userId: Int? = null
)

@HiltViewModel
class PrestamoFormViewModel @Inject constructor(private val repo: FinanzasRepository, guardado: SavedStateHandle) : ViewModel() {
    private val id: Int = guardado["id"] ?: 0
    private val _e = MutableStateFlow(EstadoForm(form = FormPrestamo()))
    val estado = _e.asStateFlow()

    init {
        viewModelScope.launch {
            val cat = (repo.catalogos() as? Resultado.Ok)?.datos
            val comunes = (repo.comunes() as? Resultado.Ok)?.datos
            var f = FormPrestamo(tipoId = cat?.tiposPrestamo?.firstOrNull()?.id, userId = comunes?.usuarioId)
            if (id != 0) (repo.prestamo(id) as? Resultado.Ok)?.datos?.let { p ->
                val d = p.datos
                f = FormPrestamo(
                    id = p.id, nombre = p.nombre, tipoId = p.tipoPrestamoId, bancoId = p.bancoId, acreedor = d?.acreedor.orEmpty(), contactoId = d?.contactoId,
                    montoOriginal = p.montoOriginal, tasa = p.tasaAnual?.toDoubleOrNull()?.toString()?.removeSuffix(".0").orEmpty(), plazo = p.plazoMeses?.toString().orEmpty(),
                    fechaOtorgamiento = p.fechaOtorgamiento ?: Formato.hoy(), diaPago = p.diaPago?.toString().orEmpty(), cuota = d?.cuota.orEmpty(),
                    saldoInicial = d?.saldoInicial.orEmpty(), fechaSaldo = d?.fechaSaldo ?: Formato.hoy(), medioPago = p.medioPago ?: "por_definir",
                    montoPlanilla = p.montoPlanilla?.takeIf { (it.toDoubleOrNull() ?: 0.0) > 0 }.orEmpty(),
                    cargos = p.cargos.map { it.nombre to it.monto.orEmpty() }, color = p.color ?: "#7C3AED", notas = d?.notas.orEmpty(),
                    activo = p.activo, crearCompromiso = false, tienePagos = !p.pagos.isNullOrEmpty(), userId = p.userId
                )
            }
            _e.value = EstadoForm(cargando = false, catalogos = cat, comunes = comunes, form = f)
        }
    }

    fun cambiar(f: FormPrestamo) { _e.value = _e.value.copy(form = f) }

    fun guardar() {
        val f = _e.value.form
        val opcional = { t: String -> t.isBlank() || dineroValido(t) }
        val errores = buildMap {
            if (f.nombre.isBlank()) put("nombre", "Escribe un nombre.")
            if (f.tipoId == null) put("tipo_prestamo_id", "Elige el tipo.")
            if (!Formato.montoValido(f.montoOriginal)) put("monto_original", "Monto con máximo dos decimales.")
            if ((f.tasa.toDoubleOrNull() ?: -1.0) !in 0.0..100.0) put("tasa_anual", "Tasa anual entre 0 y 100 (0 si no cobra interés).")
            if (!dineroValido(f.saldoInicial)) put("saldo_inicial", "¿Cuánto se debe a la fecha indicada?")
            if (f.plazo.isNotBlank() && (f.plazo.toIntOrNull() ?: 0) !in 1..600) put("plazo_meses", "Entre 1 y 600 meses.")
            if (f.diaPago.isNotBlank() && (f.diaPago.toIntOrNull() ?: 0) !in 1..31) put("dia_pago", "Día del 1 al 31.")
            if (!opcional(f.cuota)) put("cuota", "Máximo dos decimales.")
            if (!opcional(f.montoPlanilla)) put("monto_planilla", "Máximo dos decimales.")
        }
        if (errores.isNotEmpty()) { _e.value = _e.value.copy(errores = errores); return }
        _e.value = _e.value.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = PrestamoBody(
                f.nombre.trim(), f.tipoId!!, f.bancoId, f.acreedor.trim().ifBlank { null }, f.montoOriginal, f.tasa,
                f.plazo.toIntOrNull(), f.fechaOtorgamiento, f.diaPago.toIntOrNull(), f.cuota.ifBlank { null }, f.saldoInicial, f.fechaSaldo,
                f.medioPago, f.montoPlanilla.ifBlank { null }, f.color, f.notas.trim().ifBlank { null }, f.activo,
                f.cargos.filter { it.first.isNotBlank() && dineroValido(it.second) }.map { CargoBody(it.first.trim(), it.second) },
                crearCompromiso = f.crearCompromiso.takeIf { f.id == null }, contactoId = f.contactoId?.toString(), userId = f.userId
            )
            _e.value = when (val r = repo.guardarPrestamo(f.id, body)) {
                is Resultado.Ok -> _e.value.copy(guardando = false, listo = true)
                is Resultado.Falla -> _e.value.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() = viewModelScope.launch {
        _e.value = when (val r = repo.eliminarPrestamo(id)) {
            is Resultado.Ok -> _e.value.copy(listo = true, eliminado = true)
            is Resultado.Falla -> _e.value.copy(error = r.mensaje)
        }
    }
}

@Composable
fun PrestamoFormScreen(onListo: (eliminado: Boolean) -> Unit, onVolver: () -> Unit, viewModel: PrestamoFormViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val f = e.form
    LaunchedEffect(e.listo) { if (e.listo) onListo(e.eliminado) }
    PantallaFormulario(
        if (f.id == null) "Nuevo préstamo" else "Editar préstamo", e.cargando, e.guardando, e.error?.takeIf { e.errores.isEmpty() },
        onVolver, viewModel::guardar, onEliminar = f.id?.let { { viewModel.eliminar() } }, queSeElimina = "este préstamo"
    ) {
        CampoTexto("Nombre", f.nombre, { viewModel.cambiar(f.copy(nombre = it)) }, e.errores["nombre"], apoyo = "Ej: Préstamo de consumo, Carro, Evelyn")
        Desplegable("Tipo", e.catalogos?.tiposPrestamo.orEmpty().map { it.id to it.nombre }, f.tipoId, { viewModel.cambiar(f.copy(tipoId = it)) }, error = e.errores["tipo_prestamo_id"])
        Desplegable(
            "Banco", listOf<Pair<Int?, String>>(null to "No es de un banco") + e.catalogos?.bancos.orEmpty().map { it.id to it.nombre },
            f.bancoId, { viewModel.cambiar(f.copy(bancoId = it)) }, error = e.errores["banco_id"]
        )
        if (f.bancoId == null) Desplegable(
            "¿De quién? (contacto)", listOf<Pair<Int?, String>>(null to "Nadie") + e.comunes?.contactos.orEmpty().filter { it.activo || it.id == f.contactoId }.map { it.id to it.nombreCompleto },
            f.contactoId, { viewModel.cambiar(f.copy(contactoId = it)) }, error = e.errores["contacto_id"]
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMonto("Monto original", f.montoOriginal, { viewModel.cambiar(f.copy(montoOriginal = it)) }, Modifier.weight(1f), error = e.errores["monto_original"])
            CampoTexto("Tasa anual %", f.tasa, { viewModel.cambiar(f.copy(tasa = soloNumero(it))) }, e.errores["tasa_anual"], Modifier.weight(0.8f), numerico = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto("Plazo (meses)", f.plazo, { viewModel.cambiar(f.copy(plazo = soloNumero(it, false).take(3))) }, e.errores["plazo_meses"], Modifier.weight(1f), numerico = true)
            CampoTexto("Día de pago", f.diaPago, { viewModel.cambiar(f.copy(diaPago = soloNumero(it, false).take(2))) }, e.errores["dia_pago"], Modifier.weight(1f), numerico = true)
        }
        CampoFecha("Otorgado el", f.fechaOtorgamiento, { viewModel.cambiar(f.copy(fechaOtorgamiento = it)) }, desde = null, hasta = Formato.hoy(), error = e.errores["fecha_otorgamiento"])
        CampoMonto("Cuota fija (capital + interés)", f.cuota, { viewModel.cambiar(f.copy(cuota = it)) }, error = e.errores["cuota"], apoyo = "Vacío = se calcula con la tasa y el plazo")
        Text("Punto de partida", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMonto("Saldo", f.saldoInicial, { viewModel.cambiar(f.copy(saldoInicial = it)) }, Modifier.weight(1f), error = e.errores["saldo_inicial"])
        }
        CampoFecha("A la fecha", f.fechaSaldo, { viewModel.cambiar(f.copy(fechaSaldo = it)) }, desde = null, hasta = Formato.hoy(), error = e.errores["fecha_saldo"])
        if (f.tienePagos) Text("Ya tiene pagos: el saldo y su fecha ya no cambian.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Desplegable(
            "¿Cómo se paga?", listOf("transferencia" to "Desde una cuenta", "efectivo" to "En efectivo", "planilla" to "Descuento de planilla", "por_definir" to "Por definir"),
            f.medioPago, { viewModel.cambiar(f.copy(medioPago = it)) }, error = e.errores["medio_pago"]
        )
        CampoMonto("Parte que descuentan de planilla (opcional)", f.montoPlanilla, { viewModel.cambiar(f.copy(montoPlanilla = it)) }, error = e.errores["monto_planilla"],
            apoyo = "Si se paga mitad por planilla y mitad en ventanilla")
        Text("Cargos mensuales (seguros, IVA…)", style = MaterialTheme.typography.titleSmall)
        f.cargos.forEachIndexed { i, (nombre, monto) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { v -> viewModel.cambiar(f.copy(cargos = f.cargos.toMutableList().also { it[i] = v to monto })) },
                    label = { Text("Nombre") }, singleLine = true, modifier = Modifier.weight(1f))
                CampoMonto("Monto", monto, { v -> viewModel.cambiar(f.copy(cargos = f.cargos.toMutableList().also { it[i] = nombre to v })) }, Modifier.weight(0.8f))
                IconButton(onClick = { viewModel.cambiar(f.copy(cargos = f.cargos.filterIndexed { j, _ -> j != i })) }) { Icon(Icons.Default.Close, contentDescription = "Quitar cargo") }
            }
        }
        TextButton(onClick = { viewModel.cambiar(f.copy(cargos = f.cargos + ("" to ""))) }) {
            Icon(Icons.Default.Add, contentDescription = null); Text("Agregar cargo")
        }
        e.comunes?.miembros?.takeIf { it.size > 1 }?.let { m ->
            Desplegable("Titular", m.map { it.id to it.nombre }, f.userId, { viewModel.cambiar(f.copy(userId = it)) }, error = e.errores["user_id"])
        }
        OutlinedTextField(f.notas, { viewModel.cambiar(f.copy(notas = it)) }, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth())
        SelectorColor(f.color) { viewModel.cambiar(f.copy(color = it)) }
        if (f.id == null) FilaInterruptor("Crear el compromiso mensual", f.crearCompromiso, { viewModel.cambiar(f.copy(crearCompromiso = it)) }, "Aparece cada mes en Compromisos para no olvidar el pago")
        else FilaInterruptor("Activo", f.activo, { viewModel.cambiar(f.copy(activo = it)) })
    }
}

// ======================= Fuente de ingreso =======================

data class FormFuente(
    val id: Int? = null,
    val nombre: String = "",
    val tipoId: Int? = null,
    val pagador: String = "",
    val frecuencia: String = "mensual",
    val diaPago: String = "",
    val diaPago2: String = "",
    val montoBruto: String = "",
    val destino: String = "efectivo",
    val color: String = "#16A34A",
    val activo: Boolean = true,
    val userId: Int? = null
)

@HiltViewModel
class FuenteFormViewModel @Inject constructor(private val repo: FinanzasRepository, guardado: SavedStateHandle) : ViewModel() {
    private val id: Int = guardado["id"] ?: 0
    private val _e = MutableStateFlow(EstadoForm(form = FormFuente()))
    val estado = _e.asStateFlow()
    private val _destinos = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val destinos = _destinos.asStateFlow()

    init {
        viewModelScope.launch {
            val cat = (repo.catalogos() as? Resultado.Ok)?.datos
            val comunes = (repo.comunes() as? Resultado.Ok)?.datos
            _destinos.value = (repo.cajas() as? Resultado.Ok)?.datos.orEmpty().map { it.valor to it.nombre }
            var f = FormFuente(tipoId = cat?.tiposIngreso?.firstOrNull()?.id, userId = comunes?.usuarioId)
            if (id != 0) (repo.ingreso(id) as? Resultado.Ok)?.datos?.let { s ->
                f = FormFuente(
                    s.id, s.nombre, s.tipoIngresoId, s.pagador.orEmpty(), s.frecuencia, s.diaPago?.toString().orEmpty(), s.diaPago2?.toString().orEmpty(),
                    s.montoBruto?.takeIf { (it.toDoubleOrNull() ?: 0.0) > 0 }.orEmpty(), s.destino ?: "efectivo", s.color ?: "#16A34A", s.activo, s.userId
                )
            }
            _e.value = EstadoForm(cargando = false, catalogos = cat, comunes = comunes, form = f)
        }
    }

    fun cambiar(f: FormFuente) { _e.value = _e.value.copy(form = f) }

    fun guardar() {
        val f = _e.value.form
        val errores = buildMap {
            if (f.nombre.isBlank()) put("nombre", "Escribe un nombre.")
            if (f.tipoId == null) put("tipo_ingreso_id", "Elige el tipo.")
            if (f.diaPago.isNotBlank() && (f.diaPago.toIntOrNull() ?: 0) !in 1..31) put("dia_pago", "Día del 1 al 31.")
            if (f.frecuencia == "quincenal" && (f.diaPago2.toIntOrNull() ?: 0) !in 1..31) put("dia_pago_2", "Día del 1 al 31.")
            if (f.montoBruto.isNotBlank() && !dineroValido(f.montoBruto)) put("monto_bruto", "Máximo dos decimales.")
        }
        if (errores.isNotEmpty()) { _e.value = _e.value.copy(errores = errores); return }
        _e.value = _e.value.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = FuenteBody(
                f.nombre.trim(), f.tipoId!!, f.pagador.trim().ifBlank { null }, f.frecuencia, f.diaPago.toIntOrNull(),
                f.diaPago2.toIntOrNull().takeIf { f.frecuencia == "quincenal" }, f.montoBruto.ifBlank { null }, f.destino, f.color, f.activo, f.userId
            )
            _e.value = when (val r = repo.guardarFuente(f.id, body)) {
                is Resultado.Ok -> _e.value.copy(guardando = false, listo = true)
                is Resultado.Falla -> _e.value.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() = viewModelScope.launch {
        _e.value = when (val r = repo.eliminarFuente(id)) {
            is Resultado.Ok -> _e.value.copy(listo = true, eliminado = true)
            is Resultado.Falla -> _e.value.copy(error = r.mensaje)
        }
    }
}

@Composable
fun FuenteFormScreen(onListo: (eliminado: Boolean) -> Unit, onVolver: () -> Unit, viewModel: FuenteFormViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val destinos by viewModel.destinos.collectAsStateWithLifecycle()
    val f = e.form
    LaunchedEffect(e.listo) { if (e.listo) onListo(e.eliminado) }
    PantallaFormulario(
        if (f.id == null) "Nueva fuente de ingreso" else "Editar fuente", e.cargando, e.guardando, e.error?.takeIf { e.errores.isEmpty() },
        onVolver, viewModel::guardar, onEliminar = f.id?.let { { viewModel.eliminar() } }, queSeElimina = "esta fuente"
    ) {
        CampoTexto("Nombre", f.nombre, { viewModel.cambiar(f.copy(nombre = it)) }, e.errores["nombre"], apoyo = "Ej: Sueldo Stefany, Servicios profesionales")
        Desplegable("Tipo", e.catalogos?.tiposIngreso.orEmpty().map { it.id to it.nombre }, f.tipoId, { viewModel.cambiar(f.copy(tipoId = it)) }, error = e.errores["tipo_ingreso_id"])
        CampoTexto("¿Quién paga? (opcional)", f.pagador, { viewModel.cambiar(f.copy(pagador = it)) }, e.errores["pagador"])
        Desplegable("Frecuencia", listOf("mensual" to "Mensual", "quincenal" to "Quincenal"), f.frecuencia, { viewModel.cambiar(f.copy(frecuencia = it)) }, error = e.errores["frecuencia"])
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto(if (f.frecuencia == "quincenal") "Primer día" else "Día de pago", f.diaPago, { viewModel.cambiar(f.copy(diaPago = soloNumero(it, false).take(2))) }, e.errores["dia_pago"], Modifier.weight(1f), numerico = true)
            if (f.frecuencia == "quincenal") CampoTexto("Segundo día", f.diaPago2, { viewModel.cambiar(f.copy(diaPago2 = soloNumero(it, false).take(2))) }, e.errores["dia_pago_2"], Modifier.weight(1f), numerico = true)
        }
        CampoMonto("Bruto habitual (opcional)", f.montoBruto, { viewModel.cambiar(f.copy(montoBruto = it)) }, error = e.errores["monto_bruto"])
        Desplegable("¿A dónde entra?", destinos, f.destino, { viewModel.cambiar(f.copy(destino = it)) }, error = e.errores["destino"])
        e.comunes?.miembros?.takeIf { it.size > 1 }?.let { m ->
            Desplegable("¿De quién es?", m.map { it.id to it.nombre }, f.userId, { viewModel.cambiar(f.copy(userId = it)) }, error = e.errores["user_id"])
        }
        SelectorColor(f.color) { viewModel.cambiar(f.copy(color = it)) }
        if (f.id != null) FilaInterruptor("Activa", f.activo, { viewModel.cambiar(f.copy(activo = it)) })
        Text("Los descuentos de planilla (ISSS, AFP, renta, préstamos) se configuran en la web.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

