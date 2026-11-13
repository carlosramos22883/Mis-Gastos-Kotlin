package com.carlosramos.misgastos.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reparto sugerido de lo que se pasa un presupuesto entre las categorías elegidas (en centavos). */
class RepartoPresupuestoTest {
    @Test
    fun mitadYMitad() {
        assertEquals(listOf(2000L, 2000L), RepartoPresupuesto.sugerir(4000, listOf(10000, 4000)))
    }

    @Test
    fun siAUnaNoLeAlcanzaLoQueFaltaPasaALaOtra() {
        // $40 entre una que tiene $5 y otra con $100: $5 y $35
        assertEquals(listOf(500L, 3500L), RepartoPresupuesto.sugerir(4000, listOf(500, 10000)))
    }

    @Test
    fun losCentavosNoSePierden() {
        val r = RepartoPresupuesto.sugerir(1000, listOf(5000, 5000, 5000))
        assertEquals(1000L, r.sum())
    }

    @Test
    fun avisos() {
        // El usuario cambió a $30 y $10: cuadra
        assertNull(RepartoPresupuesto.aviso(4000, listOf(3000L to 10000L, 1000L to 4000L)))
        assertTrue(RepartoPresupuesto.aviso(4000, listOf(500L to 500L))!!.contains("elige otra categoría"))
        assertTrue(RepartoPresupuesto.aviso(4000, listOf(2000L to 10000L))!!.contains("súbele"))
        assertTrue(RepartoPresupuesto.aviso(4000, listOf(5000L to 10000L))!!.contains("Te sobran"))
        assertEquals("Una categoría no tiene tanto disponible.", RepartoPresupuesto.aviso(4000, listOf(4000L to 1000L)))
    }

    @Test
    fun centavosYTexto() {
        assertEquals(2437L, RepartoPresupuesto.centavos("24.37"))
        assertEquals("24.37", RepartoPresupuesto.texto(2437))
        assertEquals("0.05", RepartoPresupuesto.texto(5))
    }
}

/** Repartir lo que le queda a un presupuesto: sin tope por categoría que recibe. */
class RepartoDeLoQueQuedaTest {
    @Test
    fun avisos() {
        // A Salud le quedan $90: pasar los $90 entre Gasolina y Súper ($60 y $30) o solo una parte ($40) se puede
        assertNull(RepartoPresupuesto.avisoReparto(9000, listOf(6000L, 3000L)))
        assertNull(RepartoPresupuesto.avisoReparto(9000, listOf(4000L)))
        assertEquals("Solo le quedan $ 90.00: bájale $ 10.00 a los montos.", RepartoPresupuesto.avisoReparto(9000, listOf(6000L, 4000L)))
        assertEquals("Quita las categorías sin monto.", RepartoPresupuesto.avisoReparto(9000, listOf(4000L, 0L)))
        assertEquals(listOf(4500L, 4500L), RepartoPresupuesto.sugerir(9000, listOf(Long.MAX_VALUE, Long.MAX_VALUE)))
    }
}
