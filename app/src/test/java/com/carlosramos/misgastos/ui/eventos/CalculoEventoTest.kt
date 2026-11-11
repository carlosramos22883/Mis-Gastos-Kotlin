package com.carlosramos.misgastos.ui.eventos

import org.junit.Assert.assertEquals
import org.junit.Test

/** El reparto de la app debe dar lo mismo que EventoService::calcular del servidor. */
class CalculoEventoTest {
    private val personas = listOf("Rocio", "Juan Carlos", "Sergio", "Ever", "Eduardo", "Samuel", "Charlie", "Joel", "Alvarenga", "Mayra")
    private fun producto(uid: Int, precio: String, vararg quienes: String) =
        FilaProducto(uid, "p$uid", precio, quienes.associate { personas.indexOf(it) to "1" })

    /** La cuenta de la hoja de cálculo del autor: cuadra en $102.91 y Rocío (cumpleañera) no paga. */
    @Test
    fun laCuentaDeLaHoja() {
        val filas = personas.mapIndexed { i, n -> FilaParticipante(i, n, festejado = n == "Rocio") }
        val productos = listOf(
            producto(100, "6.95", "Juan Carlos", "Samuel", "Charlie", "Mayra"), producto(101, "6.95", "Alvarenga"), producto(102, "7.95", "Sergio", "Ever"),
            producto(103, "6.95", "Eduardo"), producto(104, "7.50", "Rocio"), producto(105, "6.95", "Joel"), producto(106, "2.50", "Rocio", "Mayra"),
            producto(107, "2.00", "Juan Carlos", "Sergio", "Samuel", "Charlie"), producto(108, "2.00", "Ever", "Joel", "Alvarenga"), producto(109, "2.50", "Eduardo")
        )
        val r = CalculoEvento.calcular(filas, productos, "porcentaje", "10")
        assertEquals(listOf(9355L, 936L, 10291L), listOf(r.subtotal, r.propina, r.total))
        assertEquals(10291L, r.por.values.sumOf { it.total })
        assertEquals(0L, r.por.getValue(0).total)
        assertEquals(1100L, r.festejo)
    }

    @Test
    fun propinaFijaDivididaEntreTodos() {
        val filas = listOf(FilaParticipante(1, "a"), FilaParticipante(2, "b"), FilaParticipante(3, "c"))
        val productos = listOf(FilaProducto(5, "Refresco", "1.00", mapOf(1 to "1", 2 to "1", 3 to "1")), FilaProducto(6, "Nachos", "6.00", mapOf(1 to "1")))
        val r = CalculoEvento.calcular(filas, productos, "monto", "1")
        assertEquals(listOf(734L, 133L, 133L), listOf(1, 2, 3).map { r.por.getValue(it).total })
    }
}
