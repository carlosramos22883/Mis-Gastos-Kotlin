package com.carlosramos.misgastos.ui.altas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Catalogos
import com.carlosramos.misgastos.data.remote.dto.CuentaBody
import com.carlosramos.misgastos.data.remote.dto.TarjetaBody
import com.carlosramos.misgastos.data.repository.Comunes
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

// ======================= Cuenta =======================

data class FormCuenta(
    val id: Int? = null,
    val nombre: String = "",
    val tipoCuentaId: Int? = null,
    val bancoId: Int? = null,
    val color: String = "#0369A1",
    val saldoInicial: String = "",
    val fechaInicial: String = Formato.hoy(),
    val activo: Boolean = true,
    val userId: Int? = null
)

data class EstadoForm<T>(
    val cargando: Boolean = true,
    val catalogos: Catalogos? = null,
    val comunes: Comunes? = null,
    val form: T,
    val guardando: Boolean = false,
    val listo: Boolean = false,
    val eliminado: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class CuentaFormViewModel @Inject constructor(private val repo: FinanzasRepository, guardado: SavedStateHandle) : ViewModel() {
    private val id: Int = guardado["id"] ?: 0
    private val _e = MutableStateFlow(EstadoForm(form = FormCuenta()))
    val estado = _e.asStateFlow()

    init {
        viewModelScope.launch {
            val cat = (repo.catalogos() as? Resultado.Ok)?.datos
            val comunes = (repo.comunes() as? Resultado.Ok)?.datos
            var f = FormCuenta(tipoCuentaId = cat?.tiposCuenta?.firstOrNull()?.id, userId = comunes?.usuarioId)
            if (id != 0) (repo.cuentas() as? Resultado.Ok)?.datos?.firstOrNull { it.id == id }?.let { c ->
                f = FormCuenta(c.id, c.nombre, c.tipoCuenta?.id, c.banco?.id, c.color ?: f.color, activo = c.activo, userId = c.userId)
            }
            _e.value = EstadoForm(cargando = false, catalogos = cat, comunes = comunes, form = f)
        }
    }

    fun cambiar(f: FormCuenta) { _e.value = _e.value.copy(form = f) }

    fun guardar() {
        val f = _e.value.form
        val errores = buildMap {
            if (f.nombre.isBlank()) put("nombre", "Escribe el nombre.")
            if (f.tipoCuentaId == null) put("tipo_cuenta_id", "Elige el tipo.")
            if (f.id == null && !dineroValido(f.saldoInicial)) put("saldo_inicial", "Saldo con máximo dos decimales (0 si está vacía).")
        }
        if (errores.isNotEmpty()) { _e.value = _e.value.copy(errores = errores); return }
        _e.value = _e.value.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = CuentaBody(
                f.nombre.trim(), f.tipoCuentaId!!, f.bancoId, f.color,
                f.saldoInicial.takeIf { f.id == null }, f.fechaInicial.takeIf { f.id == null }, f.activo, f.userId
            )
            _e.value = when (val r = repo.guardarCuenta(f.id, body)) {
                is Resultado.Ok -> _e.value.copy(guardando = false, listo = true)
                is Resultado.Falla -> _e.value.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() = viewModelScope.launch {
        _e.value = when (val r = repo.eliminarCuenta(id)) {
            is Resultado.Ok -> _e.value.copy(listo = true, eliminado = true)
            is Resultado.Falla -> _e.value.copy(error = r.mensaje)
        }
    }
}

@Composable
fun CuentaFormScreen(onListo: (eliminado: Boolean) -> Unit, onVolver: () -> Unit, viewModel: CuentaFormViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val f = e.form
    LaunchedEffect(e.listo) { if (e.listo) onListo(e.eliminado) }
    PantallaFormulario(
        if (f.id == null) "Nueva cuenta" else "Editar cuenta", e.cargando, e.guardando, e.error?.takeIf { e.errores.isEmpty() },
        onVolver, viewModel::guardar, onEliminar = f.id?.let { { viewModel.eliminar() } }, queSeElimina = "esta cuenta"
    ) {
        CampoTexto("Nombre", f.nombre, { viewModel.cambiar(f.copy(nombre = it)) }, e.errores["nombre"], apoyo = "Ej: Agrícola Stefany, Chivo, Niu")
        Desplegable("Tipo", e.catalogos?.tiposCuenta.orEmpty().map { it.id to it.nombre }, f.tipoCuentaId, { viewModel.cambiar(f.copy(tipoCuentaId = it)) }, error = e.errores["tipo_cuenta_id"])
        Desplegable(
            "Banco o emisor", listOf<Pair<Int?, String>>(null to "Ninguno") + e.catalogos?.bancos.orEmpty().map { it.id to it.nombre },
            f.bancoId, { viewModel.cambiar(f.copy(bancoId = it)) }, error = e.errores["banco_id"]
        )
        if (f.id == null) {
            CampoMonto("Saldo inicial", f.saldoInicial, { viewModel.cambiar(f.copy(saldoInicial = it)) }, error = e.errores["saldo_inicial"])
            CampoFecha("¿Desde cuándo?", f.fechaInicial, { viewModel.cambiar(f.copy(fechaInicial = it)) }, desde = null, hasta = Formato.hoy(), error = e.errores["fecha_inicial"])
            Text("El saldo y la fecha inicial solo se fijan al crearla.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        e.comunes?.miembros?.takeIf { it.size > 1 }?.let { m ->
            Desplegable("Titular", m.map { it.id to it.nombre }, f.userId, { viewModel.cambiar(f.copy(userId = it)) }, error = e.errores["user_id"])
        }
        SelectorColor(f.color) { viewModel.cambiar(f.copy(color = it)) }
        if (f.id != null) FilaInterruptor("Activa", f.activo, { viewModel.cambiar(f.copy(activo = it)) }, "Una cuenta inactiva no recibe movimientos nuevos")
    }
}

// ======================= Tarjeta =======================

data class FormTarjeta(
    val id: Int? = null,
    val nombre: String = "",
    val bancoId: Int? = null,
    val marcaId: Int? = null,
    val digitos: String = "",
    val color: String = "#1155CC",
    val limite: String = "",
    val tasa: String = "",
    val diaCorte: String = "",
    val diaPago: String = "",
    val activo: Boolean = true,
    val avances: Boolean = true,
    val disponible: String = "",
    val tienePendiente: Boolean = false,
    val pendienteMonto: String = "",
    val pendienteFecha: String = Formato.hoy(),
    val userId: Int? = null
)

@HiltViewModel
class TarjetaFormViewModel @Inject constructor(private val repo: FinanzasRepository, guardado: SavedStateHandle) : ViewModel() {
    private val id: Int = guardado["id"] ?: 0
    private val _e = MutableStateFlow(EstadoForm(form = FormTarjeta()))
    val estado = _e.asStateFlow()

    init {
        viewModelScope.launch {
            val cat = (repo.catalogos() as? Resultado.Ok)?.datos
            val comunes = (repo.comunes() as? Resultado.Ok)?.datos
            var f = FormTarjeta(userId = comunes?.usuarioId)
            if (id != 0) (repo.tarjeta(id) as? Resultado.Ok)?.datos?.tarjeta?.let { t ->
                f = FormTarjeta(
                    t.id, t.nombre, t.banco?.id, t.marca?.id, t.ultimosDigitos.orEmpty(), t.color ?: f.color, t.limite,
                    t.tasaInteres.orEmpty(), t.diaCorte.toString(), t.diaPago.toString(), t.activo, t.permiteAvances, userId = t.userId
                )
            }
            _e.value = EstadoForm(cargando = false, catalogos = cat, comunes = comunes, form = f)
        }
    }

    fun cambiar(f: FormTarjeta) { _e.value = _e.value.copy(form = f) }

    fun guardar() {
        val f = _e.value.form
        val dia = { t: String -> t.toIntOrNull()?.takeIf { it in 1..31 } }
        val errores = buildMap {
            if (f.nombre.isBlank()) put("nombre", "Escribe el nombre.")
            if (!Formato.montoValido(f.limite)) put("limite_credito", "Límite positivo con máximo dos decimales.")
            if (dia(f.diaCorte) == null) put("dia_corte", "Día del 1 al 31.")
            if (dia(f.diaPago) == null) put("dia_pago", "Día del 1 al 31.")
            if (f.digitos.isNotEmpty() && f.digitos.length != 4) put("ultimos_digitos", "Cuatro dígitos.")
            if (f.id == null) {
                if (!dineroValido(f.disponible)) put("disponible", "¿Cuánto tiene disponible hoy?")
                else if ((f.disponible.toDoubleOrNull() ?: 0.0) > (f.limite.toDoubleOrNull() ?: 0.0)) put("disponible", "No puede ser mayor que el límite.")
                if (f.tienePendiente && !Formato.montoValido(f.pendienteMonto)) put("pendiente_monto", "Monto del estado de cuenta.")
            }
        }
        if (errores.isNotEmpty()) { _e.value = _e.value.copy(errores = errores); return }
        _e.value = _e.value.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val nueva = f.id == null
            val body = TarjetaBody(
                f.nombre.trim(), f.bancoId, f.marcaId, f.digitos.ifBlank { null }, f.color, f.limite, f.tasa.ifBlank { null },
                f.diaCorte.toInt(), f.diaPago.toInt(), f.activo, f.avances,
                disponible = f.disponible.takeIf { nueva }, tienePendiente = f.tienePendiente.takeIf { nueva },
                pendienteMonto = f.pendienteMonto.takeIf { nueva && f.tienePendiente }, pendienteFecha = f.pendienteFecha.takeIf { nueva && f.tienePendiente },
                userId = f.userId
            )
            _e.value = when (val r = repo.guardarTarjeta(f.id, body)) {
                is Resultado.Ok -> _e.value.copy(guardando = false, listo = true)
                is Resultado.Falla -> _e.value.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() = viewModelScope.launch {
        _e.value = when (val r = repo.eliminarTarjeta(id)) {
            is Resultado.Ok -> _e.value.copy(listo = true, eliminado = true)
            is Resultado.Falla -> _e.value.copy(error = r.mensaje)
        }
    }
}

@Composable
fun TarjetaFormScreen(onListo: (eliminado: Boolean) -> Unit, onVolver: () -> Unit, viewModel: TarjetaFormViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val f = e.form
    LaunchedEffect(e.listo) { if (e.listo) onListo(e.eliminado) }
    PantallaFormulario(
        if (f.id == null) "Nueva tarjeta" else "Editar tarjeta", e.cargando, e.guardando, e.error?.takeIf { e.errores.isEmpty() },
        onVolver, viewModel::guardar, onEliminar = f.id?.let { { viewModel.eliminar() } }, queSeElimina = "esta tarjeta"
    ) {
        CampoTexto("Nombre", f.nombre, { viewModel.cambiar(f.copy(nombre = it)) }, e.errores["nombre"], apoyo = "Ej: Bac Walmart Stef")
        Desplegable(
            "Banco o tienda", listOf<Pair<Int?, String>>(null to "Ninguno") + e.catalogos?.bancos.orEmpty().map { it.id to it.nombre },
            f.bancoId, { viewModel.cambiar(f.copy(bancoId = it)) }, error = e.errores["banco_id"]
        )
        Desplegable(
            "Red", listOf<Pair<Int?, String>>(null to "Ninguna") + e.catalogos?.marcasRed.orEmpty().map { it.id to it.nombre },
            f.marcaId, { viewModel.cambiar(f.copy(marcaId = it)) }, error = e.errores["marca_red_id"]
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMonto("Límite", f.limite, { viewModel.cambiar(f.copy(limite = it)) }, Modifier.weight(1f), error = e.errores["limite_credito"])
            CampoTexto("Últimos 4", f.digitos, { viewModel.cambiar(f.copy(digitos = it.filter(Char::isDigit).take(4))) }, e.errores["ultimos_digitos"], Modifier.weight(0.7f), numerico = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto("Día de corte", f.diaCorte, { viewModel.cambiar(f.copy(diaCorte = soloNumero(it, false).take(2))) }, e.errores["dia_corte"], Modifier.weight(1f), numerico = true)
            CampoTexto("Día de pago", f.diaPago, { viewModel.cambiar(f.copy(diaPago = soloNumero(it, false).take(2))) }, e.errores["dia_pago"], Modifier.weight(1f), numerico = true)
        }
        CampoTexto("Tasa de interés anual % (opcional)", f.tasa, { viewModel.cambiar(f.copy(tasa = soloNumero(it))) }, e.errores["tasa_interes"], numerico = true,
            apoyo = "Sirve para calcular lo que cuesta no pagar completo")
        if (f.id == null) {
            CampoMonto("Disponible hoy", f.disponible, { viewModel.cambiar(f.copy(disponible = it)) }, error = e.errores["disponible"], apoyo = "La deuda inicial = límite − disponible")
            FilaInterruptor("Tiene un estado de cuenta por pagar", f.tienePendiente, { viewModel.cambiar(f.copy(tienePendiente = it)) })
            if (f.tienePendiente) {
                CampoMonto("Monto a pagar", f.pendienteMonto, { viewModel.cambiar(f.copy(pendienteMonto = it)) }, error = e.errores["pendiente_monto"])
                CampoFecha("Fecha límite", f.pendienteFecha, { viewModel.cambiar(f.copy(pendienteFecha = it)) }, desde = null, hasta = null, error = e.errores["pendiente_fecha"])
            }
        }
        e.comunes?.miembros?.takeIf { it.size > 1 }?.let { m ->
            Desplegable("Titular", m.map { it.id to it.nombre }, f.userId, { viewModel.cambiar(f.copy(userId = it)) }, error = e.errores["user_id"])
        }
        SelectorColor(f.color) { viewModel.cambiar(f.copy(color = it)) }
        FilaInterruptor("Permite avances de efectivo", f.avances, { viewModel.cambiar(f.copy(avances = it)) }, "Para recargar Chivo, Niu o retirar")
        if (f.id != null) FilaInterruptor("Activa", f.activo, { viewModel.cambiar(f.copy(activo = it)) })
    }
}

