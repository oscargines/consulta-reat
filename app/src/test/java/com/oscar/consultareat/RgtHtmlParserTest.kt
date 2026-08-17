package com.oscar.consultareat

import com.oscar.consultareat.data.parser.ParsedResult
import com.oscar.consultareat.data.parser.RgtHtmlParser
import com.oscar.consultareat.domain.TipoConsulta
import org.junit.Test

class RgtHtmlParserTest {

    private val parser = RgtHtmlParser()

    @Test
    fun `parse devuelve Error cuando HTML contiene mensaje de sin resultados`() {
        val html = """
            <html><body>
                <div class="container_generico">
                    <p>No se han encontrado resultados para esta consulta</p>
                </div>
            </body></html>
        """.trimIndent()

        val result = parser.parse(html, TipoConsulta.AUTORIZACIONES)
        assert(result is ParsedResult.Error)
        assert((result as ParsedResult.Error).mensaje.contains("resultados"))
    }

    @Test
    fun `parse extrae autorizaciones y titular`() {
        val html = """
            <html><body>
                <h3 class="ficha2_titulo">Datos autorización de transporte</h3>
                <table class="resultados" summary="Datos autorización de transporte">
                    <tbody>
                        <tr><th>Tipo de autorización</th><td>PUBLICO MERCANCIAS (MDPE)</td></tr>
                        <tr><th>Nº de vehículos</th><td>31</td></tr>
                    </tbody>
                </table>
                <h3 class="ficha2_titulo">Datos del titular</h3>
                <div class="container_generico listas_contactos">
                    <dl class="definition_list_contactos">
                        <dt>NIF</dt><dd>B04778627</dd>
                        <dt>Nombre</dt><dd>CRESPOSUR SL</dd>
                    </dl>
                </div>
            </body></html>
        """.trimIndent()

        val result = parser.parse(html, TipoConsulta.AUTORIZACIONES)
        assert(result is ParsedResult.Success)
        val success = result as ParsedResult.Success
        assert(success.identidadLabel == "Empresa / Titular")
        assert(success.identidadValor == "CRESPOSUR SL")
        assert(success.autorizaciones.size == 2)
        assert(success.autorizaciones[0].etiqueta == "Tipo de autorización")
        assert(success.autorizaciones[0].valor == "PUBLICO MERCANCIAS (MDPE)")
    }

    @Test
    fun `parse extrae vehiculos desde tabla con cabeceras`() {
        val html = """
            <html><body>
                <h3 class="ficha2_titulo">Datos vehículo/s</h3>
                <table class="resultados" summary="Datos vehículo/s">
                    <thead><tr><th>Matrícula</th><th>Tipo de vehículo</th><th>Observaciones</th></tr></thead>
                    <tbody>
                        <tr class="filaPar"><td>4686DPW</td><td>TRACTOR</td><td>-</td></tr>
                    </tbody>
                </table>
            </body></html>
        """.trimIndent()

        val result = parser.parse(html, TipoConsulta.VEHICULO)
        assert(result is ParsedResult.Success)
        val success = result as ParsedResult.Success
        assert(success.vehiculos.size == 2)
        assert(success.vehiculos[0].etiqueta == "Matrícula")
        assert(success.vehiculos[0].valor == "4686DPW")
    }

    @Test
    fun `parse extrae competencia, consejero y CAP`() {
        val html = """
            <html><body>
                <h3 class="ficha2_titulo">Datos competencia profesional</h3>
                <table class="resultados"><tbody>
                    <tr><th>Especialidad</th><td>Mercancías peligrosas</td></tr>
                </tbody></table>
                <h3 class="ficha2_titulo">Datos consejero de seguridad</h3>
                <table class="resultados"><tbody>
                    <tr><th>Nombre</th><td>CONSEJERO SL</td></tr>
                </tbody></table>
                <h3 class="ficha2_titulo">Datos CAP del conductor</h3>
                <table class="resultados"><tbody>
                    <tr><th>Fecha</th><td>2024-01-01</td></tr>
                </tbody></table>
            </body></html>
        """.trimIndent()

        val result = parser.parse(html, TipoConsulta.COMPETENCIA_CONSEJERO_CAP)
        assert(result is ParsedResult.Success)
        val success = result as ParsedResult.Success
        assert(success.competenciaProfesional.size == 1)
        assert(success.consejeroSeguridad.size == 1)
        assert(success.capConductor.size == 1)
    }
}