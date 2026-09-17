package com.oscar.consultareat.domain

import com.oscar.consultareat.data.parser.ParsedResult

data class ConsultaResultado(
    val identidadLabel: String?,
    val identidadValor: String?,
    val matricula: String?,
    val empresaTitular: String?,
    val numeroAutorizacion: String?,
    val autorizaciones: List<DatoItem> = emptyList(),
    val vehiculos: List<DatoItem> = emptyList(),
    val competenciaProfesional: List<DatoItem> = emptyList(),
    val consejeroSeguridad: List<DatoItem> = emptyList(),
    val capConductor: List<DatoItem> = emptyList(),
    val operadores: List<DatoItem> = emptyList(),
    val conjuntosDatos: List<DatoItem> = emptyList(),
    val error: String? = null,
    val avisoSinResultados: String? = null,
    val comentario: String? = null
)

data class DatoItem(
    val etiqueta: String,
    val valor: String
)

fun ParsedResult.Success.toConsultaResultado(): ConsultaResultado = ConsultaResultado(
    identidadLabel = identidadLabel,
    identidadValor = identidadValor,
    matricula = matricula,
    empresaTitular = empresaTitular,
    numeroAutorizacion = numeroAutorizacion,
    autorizaciones = autorizaciones,
    vehiculos = vehiculos,
    competenciaProfesional = competenciaProfesional,
    consejeroSeguridad = consejeroSeguridad,
    capConductor = capConductor,
    operadores = operadores,
    conjuntosDatos = conjuntosDatos,
    avisoSinResultados = avisoSinResultados
)
