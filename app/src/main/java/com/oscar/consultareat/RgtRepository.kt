package com.oscar.consultareat

import android.util.Log
import com.oscar.consultareat.data.cache.HistorialCache
import com.oscar.consultareat.data.cache.HistorialItem
import com.oscar.consultareat.data.client.ConsultaOutcome
import com.oscar.consultareat.data.client.RgtClient
import com.oscar.consultareat.data.parser.ParsedResult
import com.oscar.consultareat.domain.ConsultaInvoker
import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.ConsultarRgtCommand
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.TipoIdentificacion
import com.oscar.consultareat.domain.toConsultaResultado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

sealed interface GuardadoResultado {
    data class Guardado(val item: HistorialItem) : GuardadoResultado
    data class Duplicado(val existente: HistorialItem) : GuardadoResultado
}

class RgtRepository(
    private val client: RgtClient,
    private val cache: HistorialCache
) {
    private val invoker = ConsultaInvoker()

    suspend fun ejecutar(request: ConsultaRequest): ConsultaOutcome {
        Log.d(TAG, "ejecutar llamado con $request")
        val command = ConsultarRgtCommand(request, client)
        val result = invoker.ejecutar(command)
        Log.d(TAG, "invoker.ejecutar devolvió: ${result::class.simpleName}")
        return result
    }

    suspend fun guardarResultado(request: ConsultaRequest, parsed: ParsedResult.Success): GuardadoResultado {
        val existente = cache.items.first().firstOrNull {
            it.tipoConsulta == request.tipoConsulta &&
                it.tipoIdentificacion == request.tipoIdentificacion &&
                it.valor.equals(request.valor, ignoreCase = true)
        }
        if (existente != null) {
            Log.d(TAG, "Consulta duplicada en historial: ${request.valor}")
            return GuardadoResultado.Duplicado(existente)
        }
        val item = HistorialItem(
            id = UUID.randomUUID().toString(),
            tipoConsulta = request.tipoConsulta,
            tipoIdentificacion = request.tipoIdentificacion,
            valor = request.valor,
            obtenidoAt = System.currentTimeMillis(),
            resultado = parsed.toConsultaResultado()
        )
        cache.addItem(item)
        Log.d(TAG, "Consulta guardada en historial: ${request.valor}")
        return GuardadoResultado.Guardado(item)
    }

    suspend fun consultarNap(nombre: String): ParsedResult.Success? {
        val apiClient = client.apiClient ?: return null
        if (!apiClient.esConfigurada) return null
        val request = ConsultaRequest.Builder()
            .tipoConsulta(TipoConsulta.AUTORIZACIONES)
            .tipoIdentificacion(TipoIdentificacion.NOMBRE)
            .valor(nombre)
            .build()
        Log.d(TAG, "Consultando NAP para completar datos: '$nombre'")
        return when (val outcome = apiClient.consultar(request)) {
            is ConsultaOutcome.Resultado -> outcome.parsed as? ParsedResult.Success
            else -> null
        }
    }

    fun getHistorial(): Flow<List<HistorialItem>> = cache.items

    suspend fun eliminarHistorial(id: String) {
        cache.deleteItem(id)
    }

    suspend fun actualizarComentario(id: String, comentario: String) {
        cache.actualizarComentario(id, comentario)
    }

    suspend fun purgarExpirados(): Int = cache.purgarExpirados()

    companion object {
        private const val TAG = "ConsultaREAT.Repository"
    }
}