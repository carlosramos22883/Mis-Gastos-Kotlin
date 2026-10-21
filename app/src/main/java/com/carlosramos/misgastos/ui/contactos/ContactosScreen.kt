package com.carlosramos.misgastos.ui.contactos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Contacto
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Semantica
import com.carlosramos.misgastos.ui.common.Vacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactosEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val contactos: List<Contacto> = emptyList(),
    val soloDeben: Boolean = false
)

@HiltViewModel
class ContactosViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ContactosEstado())
    val estado = _estado.asStateFlow()

    fun iniciar(soloDeben: Boolean) {
        if (_estado.value.contactos.isEmpty()) _estado.value = _estado.value.copy(soloDeben = soloDeben)
        cargar()
    }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        _estado.value = when (val r = repo.contactos(if (_estado.value.soloDeben) "deben" else null)) {
            is Resultado.Ok -> _estado.value.copy(cargando = false, contactos = r.datos)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun soloDeben(valor: Boolean) { _estado.value = _estado.value.copy(soloDeben = valor, cargando = true, contactos = emptyList()); cargar() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactosScreen(
    soloDeben: Boolean,
    onAbrir: (Int) -> Unit,
    onNuevo: () -> Unit,
    onVolver: () -> Unit,
    viewModel: ContactosViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var busqueda by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.iniciar(soloDeben) }
    val total = estado.contactos.sumOf { it.pendiente?.toDoubleOrNull() ?: 0.0 }
    val lista = estado.contactos.filter { busqueda.isBlank() || it.nombreCompleto.contains(busqueda.trim(), ignoreCase = true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Contactos") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onNuevo) { Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo contacto") } }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = busqueda, onValueChange = { busqueda = it }, placeholder = { Text("Buscar por nombre") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = !estado.soloDeben, onClick = { viewModel.soloDeben(false) }, label = { Text("Todos") })
                    FilterChip(selected = estado.soloDeben, onClick = { viewModel.soloDeben(true) }, label = { Text("Nos deben") })
                    Spacer(Modifier.weight(1f))
                    if (total > 0) Text("Nos deben ${Formato.dinero(total)}", color = Semantica.ingreso, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            }
            when {
                estado.cargando -> Cargando()
                estado.error != null && estado.contactos.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() })
                lista.isEmpty() -> Vacio(if (estado.soloDeben) "Nadie les debe. 🎉" else "No hay contactos con ese nombre.")
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(lista, key = { it.id }) { c -> FilaContacto(c) { onAbrir(c.id) } }
                }
            }
        }
    }
}

@Composable
private fun FilaContacto(c: Contacto, onClick: () -> Unit) {
    val pendiente = c.pendiente?.toDoubleOrNull() ?: 0.0
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(c.fotoUrl, c.nombreCompleto, tamano = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.nombreCompleto, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                c.tipo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (!c.activo) Insignia("Inactivo", MaterialTheme.colorScheme.outline)
            }
        }
        if (pendiente > 0) Column(horizontalAlignment = Alignment.End) {
            Text("Debe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Formato.dinero(pendiente), color = Semantica.ingreso, fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
