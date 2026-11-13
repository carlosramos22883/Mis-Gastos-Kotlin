package com.carlosramos.misgastos.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carlosramos.misgastos.data.remote.dto.ExcesoPresupuesto
import com.carlosramos.misgastos.data.remote.dto.TraspasoBody
import kotlin.math.roundToLong

/** Lo que se eligió en el aviso: "cubrir" (con lo que se quita a cada categoría) o "pasarme". */
data class DecisionPresupuesto(val modo: String, val traspasos: List<TraspasoBody>? = null)

/** Reparto en centavos de lo que se pasa entre las categorías elegidas. */
object RepartoPresupuesto {
    fun centavos(texto: String?): Long = texto?.replace(",", "")?.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L

    fun texto(centavos: Long): String = "%d.%02d".format(centavos / 100, centavos % 100)

    /**
     * Partes iguales; si a una no le alcanza su parte, lo que falta pasa a las otras. Devuelve lo sugerido para cada
     * una (mismo orden que [disponibles]); si no alcanza, la suma queda por debajo de [excedente].
     */
    fun sugerir(excedente: Long, disponibles: List<Long>): List<Long> {
        val resultado = MutableList(disponibles.size) { 0L }
        var resto = excedente
        val orden = disponibles.indices.sortedBy { disponibles[it] }
        orden.forEachIndexed { i, idx ->
            val quedan = orden.size - i
            val parte = if (quedan == 1) resto else resto / quedan
            val toma = minOf(parte, disponibles[idx])
            resultado[idx] = toma
            resto -= toma
        }
        return resultado
    }

    /**
     * Repartir: por qué aún no se puede pasar (null = ya se puede). Se puede pasar menos de lo que le queda, pero la
     * suma no puede pasarlo. Sin tope por categoría que recibe.
     */
    fun avisoReparto(disponible: Long, montos: List<Long>): String? {
        if (montos.isEmpty()) return null
        val suma = montos.sum()
        return when {
            suma > disponible -> "Solo le quedan ${Formato.dinero(texto(disponible))}: bájale ${Formato.dinero(texto(suma - disponible))} a los montos."
            montos.any { it <= 0 } -> "Quita las categorías sin monto."
            else -> null
        }
    }

    /** Aviso de por qué aún no se puede cubrir (null = ya cuadra). */
    fun aviso(excedente: Long, elegidas: List<Pair<Long, Long>>): String? {
        if (elegidas.isEmpty()) return null
        if (elegidas.any { (monto, disponible) -> monto > disponible }) return "Una categoría no tiene tanto disponible."
        val falta = excedente - elegidas.sumOf { it.first }
        return when {
            falta > 0 -> {
                val sinUsar = elegidas.sumOf { (monto, disponible) -> maxOf(0L, disponible - monto) }
                if (sinUsar >= falta) "Te faltan ${Formato.dinero(texto(falta))}: súbele a los montos."
                else "Te faltan ${Formato.dinero(texto(falta))}: elige otra categoría para que alcance."
            }
            falta < 0 -> "Te sobran ${Formato.dinero(texto(-falta))}: bájale a los montos."
            elegidas.any { it.first <= 0 } -> "Quita las categorías sin monto."
            else -> null
        }
    }
}

/**
 * Aviso de un gasto que pasa su presupuesto ([manual] = cubrir uno que ya se pasó): elegir de qué categorías cubrir
 * lo que se pasa. Al marcar, sugiere partes iguales; los montos se pueden cambiar mientras sumen justo lo que se pasa.
 * [repartir]: pasar lo que le queda a un presupuesto a otras (se elige cuánto; las que reciben no tienen tope).
 */
@Composable
fun DialogoCubrirPresupuesto(
    exceso: ExcesoPresupuesto,
    manual: Boolean = false,
    repartir: Boolean = false,
    onDecidir: (DecisionPresupuesto) -> Unit,
    onCancelar: () -> Unit
) {
    val montos = remember(exceso) { mutableStateMapOf<Int, String>() } // presupuesto_id -> monto (solo las elegidas)
    // Repartir: se sugiere pasar todo lo que le queda, pero se puede pasar menos
    val excedente = RepartoPresupuesto.centavos(if (repartir) exceso.disponible else exceso.excedente)
    fun sugerir() {
        val elegidas = exceso.opciones.filter { it.presupuestoId in montos.keys }
        RepartoPresupuesto.sugerir(excedente, elegidas.map { if (repartir) Long.MAX_VALUE else RepartoPresupuesto.centavos(it.disponible) })
            .forEachIndexed { i, c -> montos[elegidas[i].presupuestoId] = RepartoPresupuesto.texto(c) }
    }
    val elegidas = exceso.opciones.filter { it.presupuestoId in montos.keys }
        .map { RepartoPresupuesto.centavos(montos[it.presupuestoId]) to RepartoPresupuesto.centavos(it.disponible) }
    val suma = elegidas.sumOf { it.first }
    val aviso = if (repartir) RepartoPresupuesto.avisoReparto(excedente, elegidas.map { it.first })
        else RepartoPresupuesto.aviso(excedente, elegidas)
    val valido = elegidas.isNotEmpty() && aviso == null

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (repartir) "Pasar a otras categorías" else if (manual) "Este presupuesto se pasó" else "Este gasto pasa tu presupuesto") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (repartir) "A «${exceso.categoria}» le quedan ${Formato.dinero(exceso.disponible)}. Pásalo a las categorías que sí lo van a usar."
                    else if (manual) "«${exceso.categoria}» se pasó por ${Formato.dinero(exceso.excedente)}."
                    else "A «${exceso.categoria}» le quedan ${Formato.dinero(exceso.queda)} y el gasto es de ${Formato.dinero(exceso.gasto)}: te pasarías por ${Formato.dinero(exceso.excedente)}."
                )
                if (exceso.opciones.isEmpty()) {
                    Text("Ninguna otra categoría tiene presupuesto disponible este ciclo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(if (repartir) "¿A cuáles? Elige una o varias. Se sugiere repartir todo lo que le queda, pero puedes pasar menos: cambia cuánto recibe cada una." else "¿Lo cubres con lo que les queda a otras categorías? Elige una o varias y, si quieres, cambia cuánto le quitas a cada una.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    exceso.opciones.forEach { o ->
                        val elegida = o.presupuestoId in montos.keys
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = elegida, onCheckedChange = { marcar ->
                                if (marcar) montos[o.presupuestoId] = "" else montos.remove(o.presupuestoId)
                                sugerir()
                            })
                            Box(Modifier.size(10.dp).background(Formato.color(o.color), CircleShape))
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(o.categoria, style = MaterialTheme.typography.bodyMedium)
                                Text((if (repartir) "Ahora le quedan " else "Le quedan ") + Formato.dinero(o.disponible), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (elegida) OutlinedTextField(
                                value = montos[o.presupuestoId].orEmpty(),
                                onValueChange = { montos[o.presupuestoId] = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' } },
                                prefix = { Text("$") }, singleLine = true, modifier = Modifier.width(110.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                                isError = !repartir && RepartoPresupuesto.centavos(montos[o.presupuestoId]) > RepartoPresupuesto.centavos(o.disponible),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(if (repartir) "Repartes" else "Cubres", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${Formato.dinero(RepartoPresupuesto.texto(suma))} de ${Formato.dinero(RepartoPresupuesto.texto(excedente))}", fontWeight = FontWeight.SemiBold,
                            color = if (valido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    aviso?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            if (exceso.opciones.isNotEmpty()) Button(enabled = valido, onClick = {
                onDecidir(DecisionPresupuesto("cubrir", exceso.opciones.filter { it.presupuestoId in montos.keys }
                    .map { TraspasoBody(it.presupuestoId, RepartoPresupuesto.texto(RepartoPresupuesto.centavos(montos[it.presupuestoId]))) }))
            }) { Text(if (repartir) "Pasar" else if (manual) "Cubrir" else "Cubrir y guardar") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCancelar) { Text("Cancelar") }
                if (!manual && !repartir) TextButton(onClick = { onDecidir(DecisionPresupuesto("pasarme")) }) { Text("Guardar y pasarme") }
            }
        }
    )
}
