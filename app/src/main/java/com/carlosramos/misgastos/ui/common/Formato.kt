package com.carlosramos.misgastos.ui.common

import androidx.compose.ui.graphics.Color
import com.carlosramos.misgastos.util.Constants
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Formatos en español: dinero "$ 1,234.56" y fechas "20 oct" / "lunes 20 de octubre". */
object Formato {
    private val espanol = Locale("es", "SV")
    private val decimales = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val corta = DateTimeFormatter.ofPattern("d MMM", espanol)
    private val conAnio = DateTimeFormatter.ofPattern("d MMM yyyy", espanol)
    private val larga = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", espanol)

    fun dinero(valor: Double): String = (if (valor < 0) "-$ " else "$ ") + decimales.format(abs(valor))

    fun dinero(valor: String?): String = dinero(valor?.toDoubleOrNull() ?: 0.0)

    fun fecha(iso: String?): LocalDate? = iso?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

    /** "20 oct" (o "20 oct 2025" si no es de este año). */
    fun fechaCorta(iso: String?): String {
        val f = fecha(iso) ?: return ""
        return (if (f.year == LocalDate.now().year) corta else conAnio).format(f).replace(".", "").replace("sept", "sep")
    }

    /** "Lunes 20 de octubre" */
    fun fechaLarga(iso: String?): String =
        fecha(iso)?.let { larga.format(it).replaceFirstChar { c -> c.uppercase() } } ?: ""

    /** "23 sep – 22 oct" */
    fun rango(inicio: String, fin: String) = "${fechaCorta(inicio)} – ${fechaCorta(fin)}"

    fun hoy(): String = LocalDate.now().toString()

    /** Milisegundos UTC de una fecha (para el DatePicker de Material) y de vuelta. */
    fun aMilis(iso: String): Long = LocalDate.parse(iso).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    fun deMilis(milis: Long): String = java.time.Instant.ofEpochMilli(milis).atZone(ZoneId.of("UTC")).toLocalDate().toString()

    /** "#16A34A" → Color; si no es válido, el color por defecto. */
    fun color(hex: String?, defecto: Color = Color.Gray): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(defecto)

    /** Texto que se lee sobre un fondo de color (contraste WCAG simple). */
    fun textoSobre(fondo: Color): Color {
        fun canal(c: Float) = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val luminancia = 0.2126f * canal(fondo.red) + 0.7152f * canal(fondo.green) + 0.0722f * canal(fondo.blue)
        return if (luminancia > 0.4f) Color(0xFF111827) else Color.White
    }

    /**
     * El servidor arma las URLs con su propio host (localhost), que el emulador o el celular no ven.
     * Se reemplaza el origen por el de la API configurada.
     */
    fun urlImagen(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val origen = Constants.BASE_URL.removeSuffix("/").removeSuffix("/api")
        return url.replace(Regex("^https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?"), origen)
    }

    /** Foto de un miembro del hogar: la API manda la ruta dentro de storage o una URL completa. */
    fun urlAvatar(ruta: String?): String? = when {
        ruta.isNullOrBlank() -> null
        ruta.startsWith("http") -> urlImagen(ruta)
        else -> Constants.BASE_URL.removeSuffix("/").removeSuffix("/api") + "/storage/" + ruta
    }

    /** Valida un monto como lo hace el servidor: positivo, máximo dos decimales. */
    fun montoValido(texto: String): Boolean = Regex("^\\d+(\\.\\d{1,2})?$").matches(texto) && (texto.toDoubleOrNull() ?: 0.0) > 0
}
