package com.oscar.consultareat.domain

import com.oscar.consultareat.data.client.ConsultaOutcome
import com.oscar.consultareat.data.client.RgtClient
import com.oscar.consultareat.data.parser.ParsedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ConsultaCommand {
    val request: ConsultaRequest
    suspend fun execute(): ConsultaOutcome
}

class ConsultarRgtCommand(
    override val request: ConsultaRequest,
    private val client: RgtClient
) : ConsultaCommand {
    override suspend fun execute(): ConsultaOutcome {
        val outcome = withContext(Dispatchers.IO) {
            client.consultar(request)
        }
        return when (outcome) {
            is ConsultaOutcome.Resultado -> {
                when (val parsed = outcome.parsed) {
                    is ParsedResult.Success -> ConsultaOutcome.Resultado(
                        ParsedResult.Success(
                            identidadLabel = parsed.identidadLabel,
                            identidadValor = parsed.identidadValor,
                            matricula = parsed.matricula,
                            empresaTitular = parsed.empresaTitular,
                            numeroAutorizacion = parsed.numeroAutorizacion,
                            autorizaciones = parsed.autorizaciones,
                            vehiculos = parsed.vehiculos,
                            competenciaProfesional = parsed.competenciaProfesional,
                            consejeroSeguridad = parsed.consejeroSeguridad,
                            capConductor = parsed.capConductor,
                            operadores = parsed.operadores,
                            conjuntosDatos = parsed.conjuntosDatos
                        )
                    )
                    is ParsedResult.Error -> ConsultaOutcome.Error(parsed.mensaje)
                }
            }
            is ConsultaOutcome.RequiereCaptcha -> ConsultaOutcome.RequiereCaptcha
            is ConsultaOutcome.Error -> ConsultaOutcome.Error(outcome.mensaje)
        }
    }
}

class ConsultaInvoker {
    private val historial = mutableListOf<ConsultaCommand>()

    suspend fun ejecutar(command: ConsultaCommand): ConsultaOutcome {
        historial.add(0, command)
        return command.execute()
    }

    fun historial(): List<ConsultaCommand> = historial.toList()

    suspend fun repetir(command: ConsultaCommand): ConsultaOutcome = command.execute()
}