package com.oscar.consultareat

import com.oscar.consultareat.data.baremo.BaremoInfraccion
import com.oscar.consultareat.data.baremo.matchesBaremoSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaremoRepositoryTest {
    private val infraccion = BaremoInfraccion(
        codigo = "AA01.01",
        concepto = "Obstrucción a la labor inspectora",
        indiceAcronimo = "A",
        indiceDefinicion = "Obligaciones con la administración pública",
        subindiceAcronimo = "A",
        subindiceConcepto = "Obstrucción a la labor inspectora",
        gravedad = "MUY GRAVE",
        normasInfringidas = "Ley de Ordenación de los Transportes Terrestres",
        normaSancionadora = "LOTT",
        preceptoSancionador = "Artículo 140",
        cuantia = 4001,
        observaciones = "Consultar texto vigente"
    )

    @Test
    fun buscaSinDistinguirMayusculasNiAcentos() {
        assertTrue(matchesBaremoSearch(infraccion, "obstruccion"))
        assertTrue(matchesBaremoSearch(infraccion, "ADMINISTRACION PUBLICA"))
        assertTrue(matchesBaremoSearch(infraccion, "aa01.01"))
    }

    @Test
    fun buscaEnNormasYPreceptos() {
        assertTrue(matchesBaremoSearch(infraccion, "ordenación"))
        assertTrue(matchesBaremoSearch(infraccion, "articulo 140"))
        assertFalse(matchesBaremoSearch(infraccion, "vehículos"))
    }

    @Test
    fun busquedaVaciaConservaElResultado() {
        assertTrue(matchesBaremoSearch(infraccion, ""))
        assertTrue(matchesBaremoSearch(infraccion, null))
    }
}
