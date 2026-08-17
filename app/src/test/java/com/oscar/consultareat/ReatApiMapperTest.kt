package com.oscar.consultareat

import com.oscar.consultareat.data.api.ReatApiMapper
import com.oscar.consultareat.data.parser.ParsedResult
import com.oscar.consultareat.domain.TipoConsulta
import org.junit.Test

class ReatApiMapperTest {

    private val mapper = ReatApiMapper()

    @Test
    fun `parse devuelve Error cuando la lista esta vacia`() {
        val json = "[]"

        val result = mapper.parse(json, TipoConsulta.AUTORIZACIONES)
        assert(result is ParsedResult.Error)
        assert((result as ParsedResult.Error).mensaje.contains("operador"))
    }

    @Test
    fun `parse extrae operadores y conjuntos de datos`() {
        val json = """
            [
              {
                "operadorId": 598,
                "nombre": "RENFE OPERADORA",
                "url": "http://www.renfe.com",
                "conjuntosDatos": [
                  {"conjuntoDatoId": 897, "nombre": "RENFE - Media y Larga Distancia", "descripcion": "Servicios regulares de tren"}
                ]
              },
              {
                "operadorId": 629,
                "nombre": "Renfe Cercanias",
                "url": "https://www.renfe.com",
                "conjuntosDatos": []
              }
            ]
        """.trimIndent()

        val result = mapper.parse(json, TipoConsulta.AUTORIZACIONES)
        assert(result is ParsedResult.Success)
        val success = result as ParsedResult.Success
        assert(success.identidadLabel == "Operadores encontrados")
        assert(success.identidadValor == "2")
        assert(success.operadores.size == 2)
        assert(success.operadores[0].etiqueta == "RENFE OPERADORA")
        assert(success.operadores[0].valor == "http://www.renfe.com")
        assert(success.conjuntosDatos.size == 1)
        assert(success.conjuntosDatos[0].etiqueta == "RENFE - Media y Larga Distancia")
    }

    @Test
    fun `parse usa valor por defecto cuando falta url`() {
        val json = """[{"operadorId":1,"nombre":"Operador A"}]"""

        val result = mapper.parse(json, TipoConsulta.AUTORIZACIONES)
        assert(result is ParsedResult.Success)
        assert((result as ParsedResult.Success).operadores[0].valor == "Sin enlace")
    }

    @Test
    fun `parse devuelve Error ante JSON invalido`() {
        val result = mapper.parse("no es json", TipoConsulta.AUTORIZACIONES)
        assert(result is ParsedResult.Error)
    }
}