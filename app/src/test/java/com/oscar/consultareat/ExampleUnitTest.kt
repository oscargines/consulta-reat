package com.oscar.consultareat

import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.TipoIdentificacion
import org.junit.Test

class ConsultaRequestTest {

    @Test
    fun `Builder crea ConsultaRequest valida para Autorizaciones y NIF`() {
        val request = ConsultaRequest.Builder()
            .tipoConsulta(TipoConsulta.AUTORIZACIONES)
            .tipoIdentificacion(TipoIdentificacion.NIF)
            .valor("B12345678")
            .build()

        assert(request.tipoConsulta == TipoConsulta.AUTORIZACIONES)
        assert(request.tipoIdentificacion == TipoIdentificacion.NIF)
        assert(request.valor == "B12345678")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Builder falla si falta tipo de consulta`() {
        ConsultaRequest.Builder()
            .tipoIdentificacion(TipoIdentificacion.NIF)
            .valor("B12345678")
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Builder falla si falta tipo de identificacion`() {
        ConsultaRequest.Builder()
            .tipoConsulta(TipoConsulta.AUTORIZACIONES)
            .valor("B12345678")
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Builder falla si valor esta vacio`() {
        ConsultaRequest.Builder()
            .tipoConsulta(TipoConsulta.AUTORIZACIONES)
            .tipoIdentificacion(TipoIdentificacion.NIF)
            .valor("  ")
            .build()
    }

    @Test
    fun `Combinacion VEHICULO + MATRICULA es valida`() {
        val request = ConsultaRequest.Builder()
            .tipoConsulta(TipoConsulta.VEHICULO)
            .tipoIdentificacion(TipoIdentificacion.MATRICULA)
            .valor("1234ABC")
            .build()

        assert(request.tipoConsulta == TipoConsulta.VEHICULO)
        assert(request.tipoIdentificacion == TipoIdentificacion.MATRICULA)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Combinacion VEHICULO + NIF no es valida`() {
        ConsultaRequest.Builder()
            .tipoConsulta(TipoConsulta.VEHICULO)
            .tipoIdentificacion(TipoIdentificacion.NIF)
            .valor("B12345678")
            .build()
    }

    @Test
    fun `Builder trim valor`() {
        val request = ConsultaRequest.Builder()
            .tipoConsulta(TipoConsulta.AUTORIZACIONES)
            .tipoIdentificacion(TipoIdentificacion.NIF)
            .valor("  B12345678  ")
            .build()

        assert(request.valor == "B12345678")
    }
}
