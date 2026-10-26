package com.oscar.consultareat

import com.oscar.consultareat.ui.coeficiente.EstadoCoeficiente
import com.oscar.consultareat.ui.coeficiente.MetodoCalculo
import com.oscar.consultareat.ui.coeficiente.calcularResultado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoeficienteCaracteristicoTest {

    @Test
    fun `misma L usa el metodo A de W constante`() {
        val resultado = calcularResultado(wa = 7660.0, la = 2929.0, wp = 7950.0, lp = 2929.0)

        assertEquals(MetodoCalculo.W_CONSTANTE, resultado.metodo)
        assertEquals(290.0, resultado.diferenciaW!!, 0.0001)
        assertEquals(3.7859, resultado.z, 0.0001)
        assertEquals(EstadoCoeficiente.DENTRO_DE_PARAMETROS, resultado.estado)
        assertNull(resultado.gamma)
        assertNull(resultado.beta)
        assertNull(resultado.alfa)
    }

    @Test
    fun `metodo A por encima del cuatro por ciento es indicio`() {
        val resultado = calcularResultado(wa = 7630.0, la = 2929.0, wp = 7950.0, lp = 2929.0)

        assertEquals(320.0, resultado.diferenciaW!!, 0.0001)
        assertEquals(4.194, resultado.z, 0.0001)
        assertEquals(EstadoCoeficiente.INDICIO_MANIPULACION, resultado.estado)
    }

    @Test
    fun `L distinta usa el metodo B de neumaticos`() {
        val resultado = calcularResultado(wa = 7630.0, la = 2929.0, wp = 7650.0, lp = 3000.0)

        assertEquals(MetodoCalculo.NEUMATICOS, resultado.metodo)
        assertEquals(22_348_270.0, resultado.gamma!!, 0.0001)
        assertEquals(22_950_000.0, resultado.beta!!, 0.0001)
        assertEquals(601_730.0, resultado.alfa!!, 0.0001)
        assertEquals(2.6925, resultado.z, 0.0001)
        assertEquals(EstadoCoeficiente.DENTRO_DE_PARAMETROS, resultado.estado)
        assertNull(resultado.diferenciaW)
    }

    @Test
    fun `metodo B por encima del cuatro por ciento es indicio`() {
        val resultado = calcularResultado(wa = 7630.0, la = 2929.0, wp = 7650.0, lp = 3050.0)

        assertEquals(23_332_500.0, resultado.beta!!, 0.0001)
        assertEquals(984_230.0, resultado.alfa!!, 0.0001)
        assertEquals(4.4040, resultado.z, 0.0001)
        assertEquals(EstadoCoeficiente.INDICIO_MANIPULACION, resultado.estado)
    }

    @Test
    fun `Z exactamente cuatro queda dentro de parametros`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1000.0, lp = 3120.0)

        assertEquals(4.0, resultado.z, 0.0001)
        assertEquals(EstadoCoeficiente.DENTRO_DE_PARAMETROS, resultado.estado)
    }

    @Test
    fun `Z negativa superior a cuatro en valor absoluto es indicio`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 1000.0, lp = 2870.0)

        assertEquals(-4.3333, resultado.z, 0.0001)
        assertEquals(EstadoCoeficiente.INDICIO_MANIPULACION, resultado.estado)
    }

    @Test
    fun `metodo A con W inferior tambien es indicio si supera el cuatro por ciento`() {
        val resultado = calcularResultado(wa = 1000.0, la = 3000.0, wp = 950.0, lp = 3000.0)

        assertEquals(-50.0, resultado.diferenciaW!!, 0.0001)
        assertEquals(-5.0, resultado.z, 0.0001)
        assertEquals(EstadoCoeficiente.INDICIO_MANIPULACION, resultado.estado)
    }
}
