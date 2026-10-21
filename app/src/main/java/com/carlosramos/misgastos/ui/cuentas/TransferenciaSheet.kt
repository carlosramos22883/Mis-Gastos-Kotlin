package com.carlosramos.misgastos.ui.cuentas

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.OpcionesTransferencia
import com.carlosramos.misgastos.data.remote.dto.TransferenciaBody
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.Formato
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormTransferencia(
    val opciones: OpcionesTransferencia? = null,
    val comunes: Comunes? = null,
    val origen: String? = null,
    val destino: String? = null,
    val monto: String = "",
    val fecha: String = Formato.hoy(),
    val descripcion: String = "",
    val userId: Int? = null,
    val guardando: Boolean = false,
    val listo: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class TransferenciaViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _form = MutableStateFlow(FormTransferencia())
    val form = _form.asStateFlow()

    /** origen/destino: "efectivo" | "cuenta:{id}" | "tarjeta:{id}" (avance, solo origen). */
    fun iniciar(origen: String?, destino: String?) = viewModelScope.launch {
        val comunes = (repo.comunes() as? Resultado.Ok)?.datos
        when (val r = repo.opcionesTransferencia()) {
            is Resultado.Ok -> {
                val o = origen ?: "efectivo"
                val d = destino ?: r.datos.destinos.firstOrNull { it.valor != o }?.valor
                _form.value = FormTransferencia(opciones = r.datos, comunes = comunes, origen = o, destino = d, userId = comunes?.usuarioId)
            }
            is Resultado.Falla -> _form.value = FormTransferencia(error = r.mensaje)
        }
    }

    fun cambiar(f: FormTransferencia) { _form.value = f }

    fun consumido() { _form.value = FormTransferencia() }

    fun guardar() {
        val f = _form.value
        val errores = buildMap {
            if (f.origen == null) put("origen", "Elige de dónde sale.")
            if (f.destino == null) put("destino", "Elige a dónde va.")
            if (f.origen != null && f.origen == f.destino) put("destino", "El destino debe ser distinto del origen.")
            if (!Formato.montoValido(f.monto)) put("monto", "Monto positivo con máximo dos decimales.")
        }
        if (errores.isNotEmpty()) { _form.value = f.copy(errores = errores); return }
        _form.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = TransferenciaBody(f.origen!!, f.destino!!, f.monto, f.fecha, f.descripcion.trim().ifBlank { null }, f.userId)
            _form.value = when (val r = repo.transferir(body)) {
                is Resultado.Ok -> f.copy(guardando = false, listo = true)
                is Resultado.Falla -> f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }
}

/** Mover dinero entre efectivo y cuentas; desde una tarjeta es un avance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferenciaSheet(
    onCerrar: () -> Unit,
    onHecho: () -> Unit,
    origen: String? = null,
    destino: String? = null,
    viewModel: TransferenciaViewModel = hiltViewModel()
) {
    val f by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.iniciar(origen, destino) }
    LaunchedEffect(f.listo) { if (f.listo) { viewModel.consumido(); onHecho() } }
    val etiqueta = { o: com.carlosramos.misgastos.data.remote.dto.OpcionTransferencia ->
        o.valor to "${o.nombre} (${if (o.valor.startsWith("tarjeta:")) "disponible " else ""}${Formato.dinero(o.saldo)})"
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Transferir", style = MaterialTheme.typography.titleLarge)
            val opciones = f.opciones
            if (opciones == null) {
                if (f.error != null) Text(f.error!!, color = MaterialTheme.colorScheme.error) else CircularProgressIndicator()
            } else {
                Desplegable("Desde", opciones.origenes.map(etiqueta), f.origen, { viewModel.cambiar(f.copy(origen = it)) }, error = f.errores["origen"])
                if (f.origen?.startsWith("tarjeta:") == true) Text(
                    "Es un avance de efectivo: queda como deuda en la tarjeta y suele cobrar comisión e intereses desde el primer día.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Desplegable("Hacia", opciones.destinos.filter { it.valor != f.origen }.map(etiqueta), f.destino, { viewModel.cambiar(f.copy(destino = it)) }, error = f.errores["destino"])
                OutlinedTextField(
                    value = f.monto, onValueChange = { viewModel.cambiar(f.copy(monto = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' })) },
                    label = { Text("Monto") }, prefix = { Text("$ ") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = f.errores["monto"] != null, supportingText = f.errores["monto"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                CampoFecha("Fecha", f.fecha, { viewModel.cambiar(f.copy(fecha = it)) }, desde = null, hasta = Formato.hoy(), error = f.errores["fecha"])
                OutlinedTextField(
                    value = f.descripcion, onValueChange = { viewModel.cambiar(f.copy(descripcion = it)) },
                    label = { Text("Nota (opcional)") }, placeholder = { Text("Ej: Chivo 17/10") }, modifier = Modifier.fillMaxWidth()
                )
                f.comunes?.miembros?.takeIf { it.size > 1 }?.let { miembros ->
                    Desplegable("¿Quién lo hizo?", miembros.map { it.id to it.nombre }, f.userId, { viewModel.cambiar(f.copy(userId = it)) }, error = f.errores["user_id"])
                }
                f.error?.takeIf { f.errores.isEmpty() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCerrar) { Text("Cancelar") }
                Button(onClick = viewModel::guardar, enabled = !f.guardando && opciones != null) {
                    if (f.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Transferir")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
