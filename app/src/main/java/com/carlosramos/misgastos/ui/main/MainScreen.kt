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
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Savings
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
import com.carlosramos.misgastos.ui.altas.CuentaFormScreen
import com.carlosramos.misgastos.ui.altas.FuenteFormScreen
import com.carlosramos.misgastos.ui.altas.PrestamoFormScreen
import com.carlosramos.misgastos.ui.altas.TarjetaFormScreen
import com.carlosramos.misgastos.ui.compromisos.CompromisoDetalleScreen
import com.carlosramos.misgastos.ui.compromisos.CompromisoFormScreen
import com.carlosramos.misgastos.ui.compromisos.CompromisosScreen
import com.carlosramos.misgastos.ui.contactos.ContactoDetalleScreen
import com.carlosramos.misgastos.ui.contactos.ContactosScreen
import com.carlosramos.misgastos.ui.cuentas.CuentaDetalleScreen
import com.carlosramos.misgastos.ui.ingresos.IngresoDetalleScreen
import com.carlosramos.misgastos.ui.ingresos.IngresosScreen
import com.carlosramos.misgastos.ui.prestamos.PrestamoDetalleScreen
import com.carlosramos.misgastos.ui.prestamos.PrestamosScreen
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
                        selected = ruta == p.ruta || (p.ruta == "compromisos" && ruta.startsWith("compromiso")) ||
                            (p.ruta == "tarjetas" && ruta.startsWith("tarjeta")) ||
                            (p.ruta == "mas" && listOf("cuenta", "contacto", "prestamo", "ingreso", "fuente").any { ruta.startsWith(it) }),
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
                    onAbrirTarjeta = { nav.navigate("tarjeta/$it") },
                    onVerPrestamos = { nav.navigate("prestamos") },
                    onVerNosDeben = { nav.navigate("contactos?deben=true") }
                )
            }
            composable("efectivo") { EfectivoScreen() }
            composable("compromisos") { CompromisosScreen(onAbrir = { nav.navigate("compromiso/$it") }, onNuevo = { nav.navigate("compromiso-form/0") }) }
            composable("compromiso-form/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                CompromisoFormScreen(onListo = { nav.popBackStack() }, onVolver = { nav.popBackStack() })
            }
            composable("compromiso/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                CompromisoDetalleScreen(onVolver = { nav.popBackStack() }, onEditar = { nav.navigate("compromiso-form/$it") })
            }
            composable("tarjetas") { TarjetasScreen(onAbrir = { nav.navigate("tarjeta/$it") }, onNueva = { nav.navigate("tarjeta-form/0") }) }
            composable("tarjeta/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                TarjetaDetalleScreen(onVolver = { nav.popBackStack() }, onAbrirCompromiso = { nav.navigate("compromiso/$it") }, onEditar = { nav.navigate("tarjeta-form/$it") })
            }
            composable("cuentas") { CuentasScreen(onAbrir = { nav.navigate("cuenta/$it") }, onNueva = { nav.navigate("cuenta-form/0") }, onVolver = { nav.popBackStack() }) }
            composable("cuenta/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                CuentaDetalleScreen(onVolver = { nav.popBackStack() }, onEditar = { nav.navigate("cuenta-form/$it") })
            }
            composable("contactos?deben={deben}", arguments = listOf(navArgument("deben") { type = NavType.BoolType; defaultValue = false })) { e ->
                ContactosScreen(
                    soloDeben = e.arguments?.getBoolean("deben") == true, onAbrir = { nav.navigate("contacto/$it") },
                    onNuevo = { nav.navigate("contacto/0") }, onVolver = { nav.popBackStack() }
                )
            }
            composable("contacto/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                ContactoDetalleScreen(onVolver = { nav.popBackStack() })
            }
            composable("prestamos") { PrestamosScreen(onAbrir = { nav.navigate("prestamo/$it") }, onNuevo = { nav.navigate("prestamo-form/0") }, onVolver = { nav.popBackStack() }) }
            composable("prestamo/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { PrestamoDetalleScreen(onVolver = { nav.popBackStack() }, onEditar = { nav.navigate("prestamo-form/$it") }) }
            composable("ingresos") { IngresosScreen(onAbrir = { nav.navigate("ingreso/$it") }, onNueva = { nav.navigate("fuente-form/0") }, onVolver = { nav.popBackStack() }) }
            composable("ingreso/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { IngresoDetalleScreen(onVolver = { nav.popBackStack() }, onEditar = { nav.navigate("fuente-form/$it") }) }
            // Formularios de alta y edición (id 0 = nuevo). Al eliminar se vuelve a la lista, no al detalle que ya no existe.
            val alTerminar = { lista: String -> { eliminado: Boolean -> if (eliminado) { if (!nav.popBackStack(lista, false)) { nav.popBackStack(); nav.popBackStack() } } else nav.popBackStack(); Unit } }
            val conId = listOf(navArgument("id") { type = NavType.IntType })
            composable("cuenta-form/{id}", arguments = conId) { CuentaFormScreen(alTerminar("cuentas"), { nav.popBackStack() }) }
            composable("tarjeta-form/{id}", arguments = conId) { TarjetaFormScreen(alTerminar("tarjetas"), { nav.popBackStack() }) }
            composable("prestamo-form/{id}", arguments = conId) { PrestamoFormScreen(alTerminar("prestamos"), { nav.popBackStack() }) }
            composable("fuente-form/{id}", arguments = conId) { FuenteFormScreen(alTerminar("ingresos"), { nav.popBackStack() }) }
            composable("mas") { MasScreen(viewModel, salir, ir = { nav.navigate(it) }) }
        }
    }
}

@Composable
private fun MasScreen(viewModel: MainViewModel, onCerrarSesion: () -> Unit, ir: (String) -> Unit) {
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
            listOf(
                Triple("cuentas", "Cuentas y billeteras", Icons.Default.AccountBalance),
                Triple("contactos", "Contactos y \"nos deben\"", Icons.Default.People),
                Triple("prestamos", "Préstamos", Icons.Default.RequestQuote),
                Triple("ingresos", "Ingresos", Icons.Default.Savings)
            ).forEach { (ruta, texto, icono) ->
                Row(Modifier.fillMaxWidth().clickable { ir(ruta) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }
}
