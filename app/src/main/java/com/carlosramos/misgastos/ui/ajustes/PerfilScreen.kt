package com.carlosramos.misgastos.ui.ajustes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.altas.CampoTexto
import com.carlosramos.misgastos.ui.altas.PantallaFormulario
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Imagen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerfilEstado(
    val cargando: Boolean = true,
    val nombre: String = "",
    val email: String = "",
    val emailOriginal: String = "",
    val foto: String? = null,
    val subiendo: Boolean = false,
    val guardando: Boolean = false,
    val listo: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class PerfilViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _e = MutableStateFlow(PerfilEstado())
    val estado = _e.asStateFlow()

    init {
        viewModelScope.launch {
            (repo.comunes(refrescar = true) as? Resultado.Ok)?.datos?.let {
                _e.value = PerfilEstado(cargando = false, nombre = it.nombre, email = it.email, emailOriginal = it.email, foto = Formato.urlAvatar(it.avatar))
            }
        }
    }

    fun cambiar(f: PerfilEstado) { _e.value = f }

    fun foto(jpeg: ByteArray?) {
        if (jpeg == null) { _e.value = _e.value.copy(error = "No se pudo leer la foto."); return }
        _e.value = _e.value.copy(subiendo = true, error = null)
        viewModelScope.launch {
            _e.value = when (val r = repo.subirAvatar(jpeg)) {
                // ?v= evita que se muestre la foto anterior guardada en caché
                is Resultado.Ok -> _e.value.copy(subiendo = false, foto = r.datos.avatarUrl?.let { "$it?v=${System.currentTimeMillis()}" })
                is Resultado.Falla -> _e.value.copy(subiendo = false, error = r.mensaje)
            }
        }
    }

    fun guardar() {
        val f = _e.value
        val errores = buildMap {
            if (f.nombre.isBlank()) put("name", "Escribe tu nombre.")
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(f.email.trim()).matches()) put("email", "Correo no válido.")
        }
        if (errores.isNotEmpty()) { _e.value = f.copy(errores = errores); return }
        _e.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            _e.value = when (val r = repo.actualizarPerfil(f.nombre.trim(), f.email.trim().lowercase())) {
                is Resultado.Ok -> _e.value.copy(guardando = false, listo = true)
                is Resultado.Falla -> _e.value.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }
}

@Composable
fun PerfilScreen(onListo: () -> Unit, onVolver: () -> Unit, viewModel: PerfilViewModel = hiltViewModel()) {
    val e by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val elegir = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { viewModel.foto(Imagen.paraSubir(context, it)) }
    }
    LaunchedEffect(e.listo) { if (e.listo) onListo() }
    PantallaFormulario("Mi perfil", e.cargando, e.guardando, e.error?.takeIf { e.errores.isEmpty() }, onVolver, viewModel::guardar) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.clickable { elegir.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Avatar(e.foto, e.nombre.ifBlank { "?" }, tamano = 120.dp)
            }
        }
        TextButton(onClick = { elegir.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !e.subiendo, modifier = Modifier.fillMaxWidth()) {
            Text(if (e.subiendo) "Subiendo foto…" else "Cambiar foto")
        }
        CampoTexto("Nombre", e.nombre, { viewModel.cambiar(e.copy(nombre = it)) }, e.errores["name"])
        CampoTexto("Correo", e.email, { viewModel.cambiar(e.copy(email = it)) }, e.errores["email"])
        if (e.email.trim().lowercase() != e.emailOriginal.lowercase()) Text(
            "Al cambiar el correo te llegará un enlace para verificarlo de nuevo.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
