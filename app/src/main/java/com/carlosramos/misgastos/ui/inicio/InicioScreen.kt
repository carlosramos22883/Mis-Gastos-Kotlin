package com.carlosramos.misgastos.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.Alerta
import com.carlosramos.misgastos.data.remote.dto.AvancesCiclo
import com.carlosramos.misgastos.data.remote.dto.PrestamosCiclo
import com.carlosramos.misgastos.data.remote.dto.Dashboard
import com.carlosramos.misgastos.data.remote.dto.TarjetaResumen
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.Avatar
import com.carlosramos.misgastos.ui.common.Cargando
import com.carlosramos.misgastos.ui.common.ErrorConReintento
import com.carlosramos.misgastos.ui.common.FilaValor
import com.carlosramos.misgastos.ui.common.Formato
import com.carlosramos.misgastos.ui.common.Insignia
import com.carlosramos.misgastos.ui.common.Seccion
import com.carlosramos.misgastos.ui.common.Semantica
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InicioEstado(
    val cargando: Boolean = true,
    val refrescando: Boolean = false,
    val datos: Dashboard? = null,
    val miembros: Map<Int, String> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class InicioViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(InicioEstado())
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar(refrescar: Boolean = false) = viewModelScope.launch {
        _estado.value = _estado.value.copy(cargando = _estado.value.datos == null, refrescando = refrescar, error = null)
        val miembros = (repo.comunes(refrescar) as? Resultado.Ok)?.datos?.miembros.orEmpty().associate { it.id to it.nombre.substringBefore(" ") }
        _estado.value = when (val r = repo.dashboard()) {
            is Resultado.Ok -> InicioEstado(cargando = false, datos = r.datos, miembros = miembros)
            is Resultado.Falla -> _estado.value.copy(cargando = false, refrescando = false, error = r.mensaje)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onAbrirCompromiso: (Int) -> Unit,
    onVerEfectivo: () -> Unit,
    onVerCompromisos: () -> Unit,
    onAbrirCuenta: (Int) -> Unit,
    onAbrirTarjeta: (Int) -> Unit,
    onVerPrestamos: () -> Unit,
    onAbrirPrestamo: (Int) -> Unit,
    onVerNosDeben: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Al volver a la pestaña, los números se actualizan (pudo registrarse algo en otra pantalla)
    LaunchedEffect(Unit) { if (estado.datos != null) viewModel.cargar() }
    val d = estado.datos
    when {
        estado.cargando -> Cargando()
        d == null -> ErrorConReintento(estado.error ?: "No se pudo cargar.", { viewModel.cargar() })
        else -> PullToRefreshBox(isRefreshing = estado.refrescando, onRefresh = { viewModel.cargar(refrescar = true) }) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        "Ciclo ${Formato.rango(d.ciclo.inicio, d.ciclo.fin)} · " +
                            when (d.ciclo.diasRestantes) { 0 -> "cierra hoy"; 1 -> "queda 1 día"; else -> "quedan ${d.ciclo.diasRestantes} días" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item { Liquidez(d, onVerEfectivo) }
                if (d.tope != null || !d.presupuestos.isNullOrEmpty()) item { GastoDelCiclo(d, onAbrirCompromiso, onVerEfectivo, onAbrirCuenta, onAbrirTarjeta) }
                d.prestamosCiclo?.takeIf { it.lista.isNotEmpty() }?.let { pc -> item { PrestamosDelCiclo(pc, onAbrirPrestamo) } }
                // Solo si en el ciclo se sacó algo de una tarjeta (avance o pago de otra tarjeta)
                d.avancesCiclo?.takeIf { it.total > 0 }?.let { av -> item { AvancesEntreTarjetas(av, onAbrirTarjeta) } }
                if (d.alertas.isNotEmpty()) item {
                    Seccion("Avisos") { d.alertas.forEach { FilaAlerta(it) } }
                }
                if (d.proximos.isNotEmpty()) item {
                    Seccion("Próximos pagos", accion = { Text("Ver todos", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onVerCompromisos)) }) {
                        d.proximos.forEach { p ->
                            Row(Modifier.fillMaxWidth().clickable { onAbrirCompromiso(p.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(width = 4.dp, height = 32.dp).clip(RoundedCornerShape(2.dp)).background(Formato.color(p.color, MaterialTheme.colorScheme.outlineVariant)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.descripcion, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(Formato.fechaCorta(p.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (p.vencido) { Spacer(Modifier.width(6.dp)); Insignia("Vencido", Semantica.egreso) }
                                    }
                                }
                                Text(Formato.dinero(p.monto), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                item {
                    Seccion("Cuentas · ${Formato.dinero(d.totalCuentas)}") {
                        d.cuentas.forEach { c ->
                            Row(Modifier.fillMaxWidth().clickable { onAbrirCuenta(c.id) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(c.banco?.logo, c.banco?.nombre ?: c.nombre, Formato.color(c.color), tamano = 32.dp, cuadrado = true)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.nombre, style = MaterialTheme.typography.bodyMedium)
                                    estado.miembros[c.userId]?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                Text(Formato.dinero(c.saldo), color = if (c.saldo < 0) Semantica.egreso else Color.Unspecified)
                            }
                        }
                    }
                }
                item {
                    Seccion("Deudas") {
                        FilaValor("Préstamos (saldo) ›", Formato.dinero(d.deudaPrestamos), onClick = onVerPrestamos)
                        FilaValor("Cuotas de préstamos por mes", Formato.dinero(d.cuotasPrestamos), onClick = onVerPrestamos)
                        FilaValor("Deuda en tarjetas", Formato.dinero(d.tarjetasTotales.deuda))
                        FilaValor("Nos deben ›", Formato.dinero(d.nosDeben), Semantica.ingreso, onClick = onVerNosDeben)
                    }
                }
                if (d.gastosPorCategoria.isNotEmpty()) item { GastosPorCategoria(d) }
                if (d.historial.isNotEmpty()) item {
                    Seccion("Ingresos y gastos por ciclo") {
                        d.historial.reversed().forEach { h ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(Formato.rango(h.inicio, h.fin), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row {
                                    Text("Ingresos ${Formato.dinero(h.ingresos)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text("Gastos ${Formato.dinero(h.gastos)}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                item { Tarjetas(d, estado.miembros, onAbrirTarjeta) }
            }
        }
    }
}

@Composable
private fun Liquidez(d: Dashboard, onVerEfectivo: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Disponible real", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
            Text(
                Formato.dinero(d.liquidez),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = if (d.liquidez < 0) Semantica.egreso else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text("Efectivo + cuentas − lo que falta pagar este ciclo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Dato("Efectivo", Formato.dinero(d.efectivo), Modifier.weight(1f).clickable(onClick = onVerEfectivo))
                Dato("Cuentas", Formato.dinero(d.totalCuentas), Modifier.weight(1f))
                Dato("Por pagar", Formato.dinero(d.porPagarCiclo), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun FilaAlerta(a: Alerta) {
    val (icono, color) = when (a.nivel) {
        "critico" -> Icons.Default.Error to Semantica.egreso
        "serio", "aviso" -> Icons.Default.Warning to Semantica.aviso
        else -> Icons.Default.Info to Semantica.info
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Insignia(a.etiqueta, color)
            Text(a.texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** El "cuadrito" de la hoja: cada tarjeta con su color, lo disponible y lo que hay que pagar. */
@Composable
private fun Tarjetas(d: Dashboard, miembros: Map<Int, String>, onAbrir: (Int) -> Unit) {
    val t = d.tarjetasTotales
    Seccion("Tarjetas de crédito") {
        FilaValor("Disponible", Formato.dinero(t.disponible), negrita = true)
        FilaValor("Por pagar (estados de cuenta)", Formato.dinero(t.porPagar))
        FilaValor("Disponible para avances", Formato.dinero(t.disponibleAvances))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(d.tarjetas, key = { it.id }) { TarjetaChica(it, miembros[it.userId]) { onAbrir(it.id) } }
        }
    }
}

@Composable
private fun TarjetaChica(t: TarjetaResumen, titular: String?, onClick: () -> Unit) {
    val fondo = Formato.color(t.color, MaterialTheme.colorScheme.secondary)
    val texto = Formato.textoSobre(fondo)
    Column(
        Modifier.width(180.dp).clip(RoundedCornerShape(12.dp)).background(fondo).clickable(onClick = onClick).padding(12.dp)
    ) {
        Text(t.nombre, color = texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(titular ?: "", color = texto.copy(alpha = 0.85f), fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Text("Disponible ${Formato.dinero(t.disponible)}", color = texto, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            if (t.porPagar > 0) "Pagar ${Formato.dinero(t.porPagar)} · ${Formato.fechaCorta(t.vence)}" else "Sin pagos pendientes",
            color = texto.copy(alpha = 0.9f), fontSize = 11.sp, maxLines = 1
        )
    }
}

/** Barras horizontales con el color de cada categoría; el monto va en texto normal. */
@Composable
private fun GastosPorCategoria(d: Dashboard) {
    val total = d.gastosPorCategoria.sumOf { it.monto }
    val principales = d.gastosPorCategoria.take(6)
    val otros = d.gastosPorCategoria.drop(6).sumOf { it.monto }
    val maximo = maxOf(principales.maxOf { it.monto }, otros).coerceAtLeast(1.0)
    Seccion("Gastos del ciclo · ${Formato.dinero(total)}") {
        (principales.map { Triple(it.nombre, it.color, it.monto) } + if (otros > 0) listOf(Triple("Otras categorías", null, otros)) else emptyList())
            .forEach { (nombre, color, monto) ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row {
                        Text(nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(Formato.dinero(monto), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth((monto / maximo).toFloat().coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(3.dp)).background(Formato.color(color, MaterialTheme.colorScheme.outline))
                        )
                    }
                }
            }
    }
}

/** Termómetro del ciclo: el tope de gasto y cada presupuesto (súper, gasolina…). Ámbar al 80 %, rojo al pasarse. */
@Composable
private fun GastoDelCiclo(d: Dashboard, onAbrirCompromiso: (Int) -> Unit, onVerEfectivo: () -> Unit, onAbrirCuenta: (Int) -> Unit, onAbrirTarjeta: (Int) -> Unit) {
    val colorBarra = @Composable { usado: Double, monto: Double ->
        when { usado > monto -> MaterialTheme.colorScheme.error; usado >= monto * 0.8 -> Semantica.aviso; else -> Semantica.ingreso }
    }
    val barra = @Composable { usado: Double, monto: Double, alto: Int ->
        Box(Modifier.fillMaxWidth().height(alto.dp).clip(RoundedCornerShape(alto.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(if (monto > 0) (usado / monto).toFloat().coerceIn(0.02f, 1f) else 0f).clip(RoundedCornerShape(alto.dp)).background(colorBarra(usado, monto)))
        }
    }
    Seccion("Gasto del ciclo") {
        d.tope?.let { t ->
            Row {
                Text("Llevas ${Formato.dinero(t.gastado)}" + (if (t.comprometido > 0) " + ${Formato.dinero(t.comprometido)} por pagar" else "") + " de ${Formato.dinero(t.tope)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${t.porcentaje} %", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            barra(t.gastado + t.comprometido, t.tope, 10)
            Text(
                if (t.queda < 0) "Te pasaste por ${Formato.dinero(-t.queda)}" else "Te quedan ${Formato.dinero(t.queda)} · ${Formato.dinero(t.porDia)} por día (${t.diasRestantes + 1} días)",
                style = MaterialTheme.typography.bodySmall, color = if (t.queda < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Cuenta lo gastado y lo que falta pagar en el ciclo, menos las cuotas de préstamos.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!t.fuera.isNullOrEmpty()) Text(
                "Fuera de presupuesto: ${Formato.dinero(t.fueraTotal)} (" + t.fuera.joinToString { "${it.nombre} ${Formato.dinero(it.gastado + it.comprometido)}" } +
                    "). Sale del tope sin descontarse de ningún presupuesto.",
                style = MaterialTheme.typography.labelSmall, color = Semantica.aviso
            )
            if (kotlin.math.abs(t.sinAsignar) >= 0.01) Text(
                if (t.sinAsignar > 0) "Sin asignar a un presupuesto: ${Formato.dinero(t.sinAsignar)}." else "Los presupuestos suman ${Formato.dinero(-t.sinAsignar)} más que el tope.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        d.presupuestos.orEmpty().forEach { p ->
            Column(Modifier.fillMaxWidth().clickable { onAbrirCompromiso(p.id) }.padding(vertical = 5.dp)) {
                Row {
                    Text(p.descripcion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (p.queda < 0) "te pasaste ${Formato.dinero(-p.queda)}" else "quedan ${Formato.dinero(p.queda)}",
                        style = MaterialTheme.typography.bodySmall, color = if (p.queda < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(3.dp))
                barra(p.gastado + p.comprometido, p.monto, 6)
                Text(
                    "${Formato.dinero(p.gastado)} de ${Formato.dinero(p.monto)}" + if (p.comprometido > 0) " + ${Formato.dinero(p.comprometido)} por pagar" else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Lo gastado en categorías sin presupuesto también sale del tope: en rojo, junto a los demás
        d.tope?.fuera.orEmpty().forEach { f ->
            var abierto by rememberSaveable(f.nombre) { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().clickable { abierto = !abierto }.padding(vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${f.nombre} (sin presupuesto)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("te pasaste ${Formato.dinero(f.gastado + f.comprometido)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Icon(if (abierto) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (abierto) "Ocultar gastos" else "Ver gastos",
                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(3.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.error))
                Text(
                    Formato.dinero(f.gastado) + if (f.comprometido > 0) " + ${Formato.dinero(f.comprometido)} por pagar" else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Los gastos y lo por pagar de la categoría: cada uno abre donde está
            if (abierto) Column(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 6.dp)) {
                f.movimientos.orEmpty().forEach { m ->
                    val abrir: () -> Unit = when (m.clase) {
                        "cuenta" -> { { m.lugarId?.let(onAbrirCuenta) } }
                        "tarjeta" -> { { m.lugarId?.let(onAbrirTarjeta) } }
                        else -> onVerEfectivo
                    }
                    FilaValor("${Formato.fechaCorta(m.fecha)} · ${m.descripcion} (${m.origen}) ›", Formato.dinero(m.monto), onClick = abrir)
                }
                f.pendientes.orEmpty().forEach { c ->
                    FilaValor("${c.fecha?.let { Formato.fechaCorta(it) } ?: "Sin fecha"} · ${c.descripcion} (por pagar) ›", Formato.dinero(c.monto), onClick = { onAbrirCompromiso(c.id) })
                }
            }
        }
    }
}

/** La cuota de cada préstamo, si ya se pagó en el ciclo y cuánto se pagó de más. */
@Composable
private fun PrestamosDelCiclo(pc: PrestamosCiclo, onAbrirPrestamo: (Int) -> Unit) {
    Seccion("Préstamos del ciclo") {
        Text(
            "${pc.pagadas} de ${pc.cuotas} " + (if (pc.cuotas == 1) "cuota pagada" else "cuotas pagadas") +
                (if (pc.falta > 0) " · faltan ${Formato.dinero(pc.falta)}" else "") + (if (pc.extra > 0) " · pagaste ${Formato.dinero(pc.extra)} de más" else ""),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        pc.lista.forEach { p ->
            Row(Modifier.fillMaxWidth().clickable { onAbrirPrestamo(p.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (p.cuota > 0) "Cuota ${Formato.dinero(p.cuota)}" else "Sin cuota", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (p.mora > 0) Text("Incluye ${Formato.dinero(p.mora)} de mora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        p.pagada -> Insignia("Pagada" + (p.fecha?.let { " el ${Formato.fechaCorta(it)}" } ?: ""), Semantica.ingreso)
                        p.pagado > 0 && p.cuota > 0 -> Insignia("Faltan ${Formato.dinero(p.falta)}", Semantica.aviso)
                        p.cuota > 0 -> Insignia("Sin pagar", MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Text("Pagaste ${Formato.dinero(p.pagado)}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (p.extra > 0) Text("${Formato.dinero(p.extra)} de más", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Semantica.ingreso)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

/** Lo que se sacó de cada tarjeta en el ciclo (avances y pagos a otras) frente a lo que se le pagó, y a dónde fue. */
@Composable
private fun AvancesEntreTarjetas(av: AvancesCiclo, onAbrirTarjeta: (Int) -> Unit) {
    val maximo = av.lista.maxOf { maxOf(it.sacado, it.recibido) }.coerceAtLeast(1.0)
    val colorSacado = MaterialTheme.colorScheme.tertiary
    val colorPagado = MaterialTheme.colorScheme.primary
    val linea = @Composable { valor: Double, color: Color ->
        if (valor > 0) Box(Modifier.fillMaxWidth((valor / maximo).toFloat().coerceIn(0.01f, 1f)).height(5.dp).clip(RoundedCornerShape(3.dp)).background(color))
        else Spacer(Modifier.height(5.dp))
    }
    Seccion("Avances y pagos entre tarjetas") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colorSacado)); Spacer(Modifier.width(4.dp))
            Text("Sacado", style = MaterialTheme.typography.labelSmall); Spacer(Modifier.width(12.dp))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colorPagado)); Spacer(Modifier.width(4.dp))
            Text("Pagado a la tarjeta", style = MaterialTheme.typography.labelSmall)
        }
        av.lista.forEach { t ->
            Column(Modifier.fillMaxWidth().clickable { onAbrirTarjeta(t.id) }.padding(vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Formato.color(t.color, MaterialTheme.colorScheme.outline)))
                    Spacer(Modifier.width(6.dp))
                    Text(t.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("sacado ${Formato.dinero(t.sacado)} · pagado ${Formato.dinero(t.recibido)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(3.dp))
                linea(t.sacado, colorSacado)
                Spacer(Modifier.height(2.dp))
                linea(t.recibido, colorPagado)
            }
        }
        if (av.flujos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("A dónde fue lo sacado", style = MaterialTheme.typography.titleSmall)
            av.flujos.forEach { f ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${f.de} → ${f.a} (" + (if (f.tipo == "avance") "avance" else "pago de tarjeta") + ")", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(Formato.dinero(f.monto), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Total sacado de las tarjetas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(Formato.dinero(av.total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
