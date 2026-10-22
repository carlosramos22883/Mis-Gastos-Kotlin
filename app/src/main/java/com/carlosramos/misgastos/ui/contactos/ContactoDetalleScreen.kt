package com.carlosramos.misgastos.ui.contactos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import com.carlosramos.misgastos.ui.common.Imagen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Contacto
import com.carlosramos.misgastos.data.remote.dto.ContactoBody
import com.carlosramos.misgastos.data.remote.dto.Deuda
import com.carlosramos.misgastos.data.remote.dto.DeudaBody
import com.carlosramos.misgastos.data.remote.dto.DeudaPago
import com.carlosramos.misgastos.data.remote.dto.DeudaPagoBody
import com.carlosramos.misgastos.data.remote.dto.TipoSimple
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.CampoFecha
import com.carlosramos.misgastos.ui.common.CampoMonto
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.Confirmar
import com.carlosramos.misgastos.ui.common.Desplegable
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.HojaFormulario
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.nombreOrigen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Formulario de "le prestamos / nos debe" (pago == false) o de "nos pagó" (pago == true). */
data class FormDeuda(
    val pago: Boolean,
    val descripcion: String = "",
    val monto: String = "",
    val fecha: String = Formato.hoy(),
    val origen: String = "efectivo",
    val deudaId: Int? = null,
    val comentario: String = "",
    val userId: Int? = null,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class FormContacto(
    val id: Int? = null,
    val nombre: String = "",
    val apellido: String = "",
    val tipoId: Int? = null,
    val telefono: String = "",
    val email: String = "",
    val notas: String = "",
    val activo: Boolean = true,
    val guardando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class ContactoEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val contacto: Contacto? = null,
    val comunes: Comunes? = null,
    /** "efectivo", "cuenta:42", "tarjeta:7" → nombre con saldo */
    val origenes: List<Pair<String, String>> = emptyList(),
    val tipos: List<TipoSimple> = emptyList(),
    val subiendoFoto: Boolean = false,
    val mensaje: String? = null
)

@HiltViewModel
class ContactoViewModel @Inject constructor(
    private val repo: FinanzasRepository,
    estadoGuardado: SavedStateHandle
) : ViewModel() {
    /** 0 = contacto nuevo */
    private var id: Int = estadoGuardado["id"] ?: 0
    private val _estado = MutableStateFlow(ContactoEstado(cargando = id != 0))
    val estado = _estado.asStateFlow()
    private val _deuda = MutableStateFlow<FormDeuda?>(null)
    val deuda = _deuda.asStateFlow()
    private val _contacto = MutableStateFlow<FormContacto?>(null)
    val formContacto = _contacto.asStateFlow()

    init {
        viewModelScope.launch {
            val tipos = (repo.catalogos() as? Resultado.Ok)?.datos?.tiposContacto.orEmpty()
            _estado.value = _estado.value.copy(comunes = (repo.comunes() as? Resultado.Ok)?.datos, tipos = tipos)
            if (id == 0) _contacto.value = FormContacto(tipoId = tipos.firstOrNull()?.id) else cargarOrigenes()
        }
        if (id != 0) cargar()
    }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        _estado.value = when (val r = repo.contacto(id)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, contacto = r.datos)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun avisar(t: String) { _estado.value = _estado.value.copy(mensaje = t) }
    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }

    // ----- Deudas y pagos -----

    /** Efectivo, cuentas y tarjetas con su saldo (también dan nombre a los orígenes del historial). */
    private suspend fun cargarOrigenes() {
        val cajas = (repo.cajas() as? Resultado.Ok)?.datos.orEmpty().map { it.valor to "${it.nombre} (${Formato.dinero(it.saldo)})" }
        val tarjetas = (repo.tarjetas() as? Resultado.Ok)?.datos.orEmpty()
            .filter { it.activo }.map { "tarjeta:${it.id}" to "Tarjeta ${it.nombre} (disponible ${Formato.dinero(it.disponible)})" }
        _estado.value = _estado.value.copy(origenes = cajas + tarjetas)
    }

    fun abrirDeuda(pago: Boolean) {
        _deuda.value = FormDeuda(pago = pago, userId = _estado.value.comunes?.usuarioId, guardando = true)
        viewModelScope.launch { cargarOrigenes(); _deuda.value = _deuda.value?.copy(guardando = false) }
    }

    fun cambiarDeuda(f: FormDeuda) { _deuda.value = f }
    fun cerrarDeuda() { _deuda.value = null }

    fun guardarDeuda() {
        val f = _deuda.value ?: return
        val errores = buildMap {
            if (!f.pago && f.descripcion.isBlank()) put("descripcion", "¿Qué le prestaron o qué debe?")
            if (!Formato.montoValido(f.monto)) put("monto", "Monto positivo con máximo dos decimales.")
        }
        if (errores.isNotEmpty()) { _deuda.value = f.copy(errores = errores); return }
        _deuda.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val comentario = f.comentario.trim().ifBlank { null }
            val r = if (f.pago) repo.crearPagoDeuda(id, DeudaPagoBody(f.monto, f.fecha, f.origen, f.deudaId, comentario, f.userId))
            else repo.crearDeuda(id, DeudaBody(f.descripcion.trim(), f.monto, f.fecha, f.origen, comentario, f.userId))
            when (r) {
                is Resultado.Ok -> {
                    _deuda.value = null
                    _estado.value = _estado.value.copy(contacto = r.datos, mensaje = if (f.pago) "Pago registrado." else "Deuda registrada.")
                }
                is Resultado.Falla -> _deuda.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminarDeuda(d: Deuda) = viewModelScope.launch {
        when (val r = repo.eliminarDeuda(d.id)) {
            is Resultado.Ok -> { avisar("Deuda eliminada."); cargar() }
            is Resultado.Falla -> avisar(r.mensaje)
        }
    }

    fun eliminarPago(p: DeudaPago) = viewModelScope.launch {
        when (val r = repo.eliminarPagoDeuda(p.id)) {
            is Resultado.Ok -> { avisar("Pago eliminado."); cargar() }
            is Resultado.Falla -> avisar(r.mensaje)
        }
    }

    // ----- Foto -----

    fun foto(jpeg: ByteArray?) {
        val c = _estado.value.contacto ?: return
        if (jpeg == null) { avisar("No se pudo leer la foto."); return }
        _estado.value = _estado.value.copy(subiendoFoto = true)
        viewModelScope.launch {
            when (val r = repo.fotoContacto(c, jpeg)) {
                is Resultado.Ok -> { _estado.value = _estado.value.copy(subiendoFoto = false); avisar("Foto actualizada."); cargar() }
                is Resultado.Falla -> { _estado.value = _estado.value.copy(subiendoFoto = false); avisar(r.mensaje) }
            }
        }
    }

    // ----- Datos del contacto -----

    fun editarContacto() {
        val c = _estado.value.contacto ?: return
        _contacto.value = FormContacto(
            id = c.id, nombre = c.nombre.orEmpty(), apellido = c.apellido.orEmpty(), tipoId = c.tipoContactoId,
            telefono = c.telefono.orEmpty(), email = c.email.orEmpty(), notas = c.notas.orEmpty(), activo = c.activo
        )
    }

    fun cambiarContacto(f: FormContacto) { _contacto.value = f }
    fun cerrarContacto() { _contacto.value = null }

    /** Al crear, esta misma pantalla pasa a mostrar el contacto nuevo. */
    fun guardarContacto() {
        val f = _contacto.value ?: return
        val errores = buildMap {
            if (f.nombre.isBlank()) put("nombre", "Escribe el nombre.")
            if (f.tipoId == null) put("tipo_contacto_id", "Elige el tipo.")
        }
        if (errores.isNotEmpty()) { _contacto.value = f.copy(errores = errores); return }
        _contacto.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = ContactoBody(
                f.nombre.trim(), f.apellido.trim().ifBlank { null }, f.tipoId!!, f.telefono.trim().ifBlank { null },
                f.email.trim().ifBlank { null }, f.notas.trim().ifBlank { null }, f.activo
            )
            when (val r = repo.guardarContacto(f.id, body)) {
                is Resultado.Ok -> {
                    _contacto.value = null
                    if (f.id == null) { id = r.datos.id; _estado.value = _estado.value.copy(cargando = true); avisar("Contacto creado.") } else avisar("Contacto actualizado.")
                    cargar()
                    cargarOrigenes()
                }
                is Resultado.Falla -> _contacto.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactoDetalleScreen(onVolver: () -> Unit, viewModel: ContactoViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val deuda by viewModel.deuda.collectAsStateWithLifecycle()
    val formContacto by viewModel.formContacto.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var borrarDeuda by remember { mutableStateOf<Deuda?>(null) }
    var borrarPago by remember { mutableStateOf<DeudaPago?>(null) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeVisto() } }
    val c = estado.contacto
    val miembros = estado.comunes?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }
    // Para el historial, el nombre sin el saldo actual
    val cajas = estado.origenes.associate { (k, v) -> k to v.substringBefore(" (") }
    val contexto = LocalContext.current
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.foto(Imagen.paraSubir(contexto, it)) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(c?.nombreCompleto ?: "Nuevo contacto", maxLines = 1, overflow = TextOverflow.Ellipsis) }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { if (c != null) IconButton(onClick = viewModel::editarContacto) { Icon(Icons.Default.Edit, contentDescription = "Editar contacto") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            c == null && formContacto == null && estado.error != null -> ErrorConReintento(estado.error!!, { viewModel.cargar() }, Modifier.padding(relleno))
            c == null -> Cargando(Modifier.padding(relleno))
            else -> Column(Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tocar la foto para cambiarla (se recorta en cuadrado de 512 px, como en la web)
                    Box(Modifier.clickable(enabled = !estado.subiendoFoto) { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Avatar(c.fotoUrl, c.nombreCompleto, tamano = 104.dp)
                        if (estado.subiendoFoto) CircularProgressIndicator(Modifier.size(104.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(c.nombreCompleto, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(c.tipo, c.telefono, c.email).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!c.activo) Insignia("Inactivo", MaterialTheme.colorScheme.outline)
                    }
                }
                c.notas?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Seccion("Lo que nos debe") {
                    FilaValor("Prestado", Formato.dinero(c.prestado))
                    FilaValor("Pagado", Formato.dinero(c.pagado))
                    FilaValor("Pendiente", Formato.dinero(c.pendiente), Semantica.ingreso, negrita = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.abrirDeuda(false) }, modifier = Modifier.weight(1f)) { Text("Prestar / anotar") }
                    Button(onClick = { viewModel.abrirDeuda(true) }, enabled = (c.pendiente?.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.weight(1f)) { Text("Nos pagó") }
                }
                c.deudas.orEmpty().forEach { d ->
                    Seccion(d.descripcion, accion = {
                        Insignia(if (d.estado == "pagada") "Pagada" else "Pendiente ${Formato.dinero(d.pendiente)}", if (d.estado == "pagada") Semantica.ingreso else Semantica.aviso)
                    }) {
                        FilaValor("${Formato.fechaCorta(d.fecha)} · ${nombreOrigen(d.origen, cajas)}${miembros[d.userId]?.let { " · $it" } ?: ""}", Formato.dinero(d.monto))
                        d.comentario?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        d.pagos.forEach { p ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Row(Modifier.fillMaxWidth().clickable { borrarPago = p }.padding(vertical = 6.dp)) {
                                Text("Pagó el ${Formato.fechaCorta(p.fecha)} · ${nombreOrigen(p.destino, cajas)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(Formato.dinero(p.monto), color = Semantica.ingreso, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (d.pagos.isEmpty()) Text(
                            "Eliminar esta deuda", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { borrarDeuda = d }.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    deuda?.let { f ->
        HojaFormulario(
            titulo = if (f.pago) "Nos pagó" else "Prestar o anotar deuda", accion = "Guardar", guardando = f.guardando,
            onCerrar = viewModel::cerrarDeuda, onAccion = viewModel::guardarDeuda, error = f.error?.takeIf { f.errores.isEmpty() }
        ) {
            if (!f.pago) OutlinedTextField(
                value = f.descripcion, onValueChange = { viewModel.cambiarDeuda(f.copy(descripcion = it)) }, label = { Text("¿Qué fue?") },
                placeholder = { Text("Ej: Camiseta, préstamo para el bus…") },
                isError = f.errores["descripcion"] != null, supportingText = f.errores["descripcion"]?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth()
            )
            CampoMonto("Monto", f.monto, { viewModel.cambiarDeuda(f.copy(monto = it)) }, error = f.errores["monto"],
                apoyo = if (f.pago) "Debe ${Formato.dinero(c?.pendiente)}: se aplica de la deuda más antigua a la más reciente" else null)
            CampoFecha("Fecha", f.fecha, { viewModel.cambiarDeuda(f.copy(fecha = it)) }, desde = null, hasta = Formato.hoy(), error = f.errores["fecha"])
            Desplegable(
                if (f.pago) "¿A dónde entró el dinero?" else "¿De dónde salió el dinero?",
                estado.origenes.filter { !f.pago || !it.first.startsWith("tarjeta:") } + ("ninguno" to if (f.pago) "Solo anotarlo (no entró dinero)" else "Solo anotarlo (no salió dinero)"),
                f.origen, { viewModel.cambiarDeuda(f.copy(origen = it)) }, error = f.errores[if (f.pago) "destino" else "origen"]
            )
            if (f.pago) {
                val abiertas = c?.deudas.orEmpty().filter { it.estado != "pagada" }
                if (abiertas.size > 1) Desplegable(
                    "¿A cuál deuda?", listOf<Pair<Int?, String>>(null to "A la más antigua primero") + abiertas.map { it.id to "${it.descripcion} (${Formato.dinero(it.pendiente)})" },
                    f.deudaId, { viewModel.cambiarDeuda(f.copy(deudaId = it)) }
                )
            }
            OutlinedTextField(value = f.comentario, onValueChange = { viewModel.cambiarDeuda(f.copy(comentario = it)) }, label = { Text("Comentario (opcional)") }, modifier = Modifier.fillMaxWidth())
            estado.comunes?.miembros?.takeIf { it.size > 1 }?.let { m ->
                Desplegable("¿Quién lo registró?", m.map { it.id to it.nombre }, f.userId, { viewModel.cambiarDeuda(f.copy(userId = it)) })
            }
        }
    }

    formContacto?.let { f ->
        HojaFormulario(
            titulo = if (f.id == null) "Nuevo contacto" else "Editar contacto", accion = "Guardar", guardando = f.guardando,
            onCerrar = { viewModel.cerrarContacto(); if (f.id == null) onVolver() },
            onAccion = viewModel::guardarContacto, error = f.error?.takeIf { f.errores.isEmpty() }
        ) {
            OutlinedTextField(value = f.nombre, onValueChange = { viewModel.cambiarContacto(f.copy(nombre = it)) }, label = { Text("Nombre") },
                isError = f.errores["nombre"] != null, supportingText = f.errores["nombre"]?.let { { Text(it) } }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = f.apellido, onValueChange = { viewModel.cambiarContacto(f.copy(apellido = it)) }, label = { Text("Apellido (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Desplegable("Tipo", estado.tipos.map { it.id to it.nombre }, f.tipoId, { viewModel.cambiarContacto(f.copy(tipoId = it)) }, error = f.errores["tipo_contacto_id"])
            OutlinedTextField(value = f.telefono, onValueChange = { viewModel.cambiarContacto(f.copy(telefono = it)) }, label = { Text("Teléfono (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = f.email, onValueChange = { viewModel.cambiarContacto(f.copy(email = it)) }, label = { Text("Correo (opcional)") },
                isError = f.errores["email"] != null, supportingText = f.errores["email"]?.let { { Text(it) } }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = f.notas, onValueChange = { viewModel.cambiarContacto(f.copy(notas = it)) }, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth())
            if (f.id != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activo", modifier = Modifier.weight(1f))
                Switch(checked = f.activo, onCheckedChange = { viewModel.cambiarContacto(f.copy(activo = it)) })
            }
            if (f.id != null) Text("Para cambiar la foto, tócala en la ficha del contacto.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    borrarDeuda?.let { d ->
        Confirmar("¿Eliminar esta deuda?", "${d.descripcion} · ${Formato.dinero(d.monto)}. Si salió dinero, se revierte ese gasto.", "Eliminar",
            onConfirmar = { borrarDeuda = null; viewModel.eliminarDeuda(d) }, onCancelar = { borrarDeuda = null })
    }
    borrarPago?.let { p ->
        Confirmar("¿Eliminar este pago?", "${Formato.dinero(p.monto)} del ${Formato.fechaCorta(p.fecha)}. Si entró dinero, se revierte ese ingreso.", "Eliminar",
            onConfirmar = { borrarPago = null; viewModel.eliminarPago(p) }, onCancelar = { borrarPago = null })
    }
}
