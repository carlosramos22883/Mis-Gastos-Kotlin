package com.carlosramos.misgastos.ui.cuentas

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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.carlosramos.misgastos.data.remote.dto.Cuenta
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

data class CuentasEstado(
    val cargando: Boolean = true,
    val error: String? = null,
    val cuentas: List<Cuenta> = emptyList(),
    val miembros: Map<Int, String> = emptyMap()
)

@HiltViewModel
class CuentasViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(CuentasEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        _estado.value = _estado.value.copy(error = null)
        val miembros = (repo.comunes() as? Resultado.Ok)?.datos?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }
        _estado.value = when (val r = repo.cuentas()) {
            is Resultado.Ok -> CuentasEstado(cargando = false, cuentas = r.datos, miembros = miembros)
            is Resultado.Falla -> _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentasScreen(onAbrir: (Int) -> Unit, onVolver: () -> Unit, viewModel: CuentasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var transferir by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (estado.cuentas.isNotEmpty()) viewModel.cargar() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Cuentas y billeteras") }, windowInsets = WindowInsets(0),
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { transferir = true }, icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }, text = { Text("Transferir") })
        }
    ) { relleno ->
        when {
            estado.cargando -> Cargando(Modifier.padding(relleno))
            estado.error != null && estado.cuentas.isEmpty() -> ErrorConReintento(estado.error!!, { viewModel.cargar() }, Modifier.padding(relleno))
            estado.cuentas.isEmpty() -> Vacio("Aún no hay cuentas. Créalas desde la web.", Modifier.padding(relleno))
            else -> LazyColumn(Modifier.padding(relleno).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    val total = estado.cuentas.filter { it.activo }.sumOf { it.saldo.toDoubleOrNull() ?: 0.0 }
                    Text("Total en cuentas: ${Formato.dinero(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(estado.cuentas, key = { it.id }) { c -> FilaCuenta(c, estado.miembros[c.userId]) { onAbrir(c.id) } }
                item { Spacer(Modifier.padding(36.dp)) }
            }
        }
    }
    if (transferir) TransferenciaSheet(onCerrar = { transferir = false }, onHecho = { transferir = false; viewModel.cargar() })
}

@Composable
private fun FilaCuenta(c: Cuenta, titular: String?, onClick: () -> Unit) {
    val saldo = c.saldo.toDoubleOrNull() ?: 0.0
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.banco?.logoUrl, c.banco?.nombre ?: c.nombre, Formato.color(c.color), tamano = 44.dp, cuadrado = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(c.tipoCuenta?.nombre, titular).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!c.activo) Insignia("Inactiva", MaterialTheme.colorScheme.outline)
            }
            Text(Formato.dinero(saldo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (saldo < 0) Semantica.egreso else MaterialTheme.colorScheme.onSurface)
        }
    }
}
