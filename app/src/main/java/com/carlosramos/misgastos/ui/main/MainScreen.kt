package com.carlosramos.misgastos.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.data.repository.Sesion
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.compromisos.CompromisoDetalleScreen
import com.carlosramos.misgastos.ui.compromisos.CompromisosScreen
import com.carlosramos.misgastos.ui.cuentas.CuentaDetalleScreen
import com.carlosramos.misgastos.ui.cuentas.CuentasScreen
import com.carlosramos.misgastos.ui.efectivo.EfectivoScreen
import com.carlosramos.misgastos.ui.tarjetas.TarjetaDetalleScreen
import com.carlosramos.misgastos.ui.tarjetas.TarjetasScreen
import com.carlosramos.misgastos.ui.inicio.InicioScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repo: FinanzasRepository, val sesion: Sesion) : ViewModel() {
    private val _comunes = MutableStateFlow<Comunes?>(null)
    val comunes = _comunes.asStateFlow()

    init { viewModelScope.launch { (repo.comunes() as? Resultado.Ok)?.let { _comunes.value = it.datos } } }

    fun olvidar() = repo.olvidar()
}

private data class Pestana(val ruta: String, val texto: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana("inicio", "Inicio", Icons.Default.Home),
    Pestana("efectivo", "Efectivo", Icons.Default.Payments),
    Pestana("tarjetas", "Tarjetas", Icons.Default.CreditCard),
    Pestana("compromisos", "Compromisos", Icons.AutoMirrored.Filled.EventNote),
    Pestana("mas", "Más", Icons.Default.Menu)
)

/** Pantalla principal después del login: barra inferior con los módulos. */
@Composable
fun MainScreen(onCerrarSesion: () -> Unit, viewModel: MainViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val actual by nav.currentBackStackEntryAsState()
    val salir = { viewModel.olvidar(); onCerrarSesion() }
    LaunchedEffect(Unit) { viewModel.sesion.expirada.collect { salir() } }

    fun ir(ruta: String) = nav.navigate(ruta) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        bottomBar = {
            val ruta = actual?.destination?.route.orEmpty()
            NavigationBar {
                pestanas.forEach { p ->
                    NavigationBarItem(
                        selected = ruta == p.ruta || (p.ruta == "compromisos" && ruta.startsWith("compromiso/")) ||
                            (p.ruta == "tarjetas" && ruta.startsWith("tarjeta/")) || (p.ruta == "mas" && ruta.startsWith("cuenta")),
                        onClick = { ir(p.ruta) },
                        icon = { Icon(p.icono, contentDescription = null) },
                        label = { Text(p.texto, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    ) { relleno ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(relleno)) {
            composable("inicio") {
                InicioScreen(
                    onAbrirCompromiso = { nav.navigate("compromiso/$it") },
                    onVerEfectivo = { ir("efectivo") },
                    onVerCompromisos = { ir("compromisos") },
                    onAbrirCuenta = { nav.navigate("cuenta/$it") },
                    onAbrirTarjeta = { nav.navigate("tarjeta/$it") }
                )
            }
            composable("efectivo") { EfectivoScreen() }
            composable("compromisos") { CompromisosScreen(onAbrir = { nav.navigate("compromiso/$it") }) }
            composable("compromiso/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                CompromisoDetalleScreen(onVolver = { nav.popBackStack() })
            }
            composable("tarjetas") { TarjetasScreen(onAbrir = { nav.navigate("tarjeta/$it") }) }
            composable("tarjeta/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                TarjetaDetalleScreen(onVolver = { nav.popBackStack() }, onAbrirCompromiso = { nav.navigate("compromiso/$it") })
            }
            composable("cuentas") { CuentasScreen(onAbrir = { nav.navigate("cuenta/$it") }, onVolver = { nav.popBackStack() }) }
            composable("cuenta/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                CuentaDetalleScreen(onVolver = { nav.popBackStack() })
            }
            composable("mas") { MasScreen(viewModel, salir, onCuentas = { nav.navigate("cuentas") }) }
        }
    }
}

@Composable
private fun MasScreen(viewModel: MainViewModel, onCerrarSesion: () -> Unit, onCuentas: () -> Unit) {
    val comunes by viewModel.comunes.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        comunes?.let { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(null, c.nombre, tamano = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(c.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(c.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            c.hogar?.let { h ->
                Seccion(h.nombre) {
                    FilaValor("Cierre de ciclo", "Día ${h.fechaCorteDia}")
                    h.miembros.forEach { m -> FilaValor(m.nombre, if (m.esAdministrador) "Administra" else "Miembro") }
                }
            }
        }
        Seccion("Módulos") {
            Row(Modifier.fillMaxWidth().clickable(onClick = onCuentas).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Cuentas y billeteras", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        Seccion("Próximamente en la app") {
            Text(
                "Contactos y \"nos deben\", préstamos e ingresos. Mientras tanto están en la web.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }
}
