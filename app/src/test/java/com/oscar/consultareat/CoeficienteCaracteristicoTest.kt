package com.oscar.consultareat

import com.oscar.consultareat.ui.coeficiente.calcularResultado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoeficienteCaracteristicoTest {

    @Test
    fun `circunferencias iguales no calculan valores y no hay fraude`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1100.0, lp = 3000.0)

        assertTrue(resultado.circunferenciasIguales)
        assertFalse(resultado.hayFraude)
        assertNull(resultado.gamma)
        assertNull(resultado.beta)
        assertNull(resultado.alfa)
        assertNull(resultado.dseta)
    }

    @Test
    fun `calcula gamma beta alfa y dseta`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1000.0, lp = 3120.0)

        assertEquals(3_000_000.0, resultado.gamma!!, 0.0001)
        assertEquals(3_120_000.0, resultado.beta!!, 0.0001)
        assertEquals(120_000.0, resultado.alfa!!, 0.0001)
        assertEquals(4.0, resultado.dseta!!, 0.0001)
    }

    @Test
    fun `dseta exactamente cuatro no es fraude`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1000.0, lp = 3120.0)

        assertEquals(4.0, resultado.dseta!!, 0.0001)
        assertFalse(resultado.hayFraude)
    }

    @Test
    fun `dseta superior a cuatro es fraude`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1000.0, lp = 3130.0)

        assertEquals(4.3333, resultado.dseta!!, 0.0001)
        assertTrue(resultado.hayFraude)
    }

    @Test
    fun `dseta negativa superior a cuatro en valor absoluto es fraude`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1000.0, lp = 2870.0)

        assertEquals(-4.3333, resultado.dseta!!, 0.0001)
        assertTrue(resultado.hayFraude)
    }

    @Test
    fun `dseta negativa dentro del margen no es fraude`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1000.0, lp = 2950.0)

        assertEquals(-1.6667, resultado.dseta!!, 0.0001)
        assertFalse(resultado.hayFraude)
    }
}
