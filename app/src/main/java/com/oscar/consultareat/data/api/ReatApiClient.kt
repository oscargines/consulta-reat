package com.oscar.consultareat.data.api

import android.util.Log
import com.oscar.consultareat.data.client.ConsultaOutcome
import com.oscar.consultareat.data.parser.ParsedResult
import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.TipoIdentificacion
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ReatApiClient(
    private val config: ReatApiConfig,
    private val mapper: ReatApiMapper = ReatApiMapper()
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val esConfigurada: Boolean get() = config.esConfigurada

    fun consultar(request: ConsultaRequest): ConsultaOutcome {
        if (!config.esConfigurada) {
            return ConsultaOutcome.Error("API NAP no configurada")
        }

        if (request.tipoIdentificacion != TipoIdentificacion.NOMBRE) {
            return ConsultaOutcome.Error("El NAP solo permite consultas por nombre")
        }

        val nombre = URLEncoder.encode(request.valor, "UTF-8")
        val url = "${config.baseUrl.trimEnd('/')}/api/Operador/GetByName/$nombre"
        val httpRequest = Request.Builder()
            .url(url)
            .get()
            .addHeader("ApiKey", config.apiKey)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", config.userAgent)
            .build()

        return try {
            Log.d(TAG, "Consultando NAP: $url")
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string() ?: return ConsultaOutcome.Error("Respuesta vacía del API")
                if (!response.isSuccessful) {
                    Log.w(TAG, "Error HTTP del NAP: ${response.code}")
                    return ConsultaOutcome.Error("Error del API NAP: ${response.code}")
                }
                when (val parsed = mapper.parse(body, request.tipoConsulta)) {
                    is ParsedResult.Success -> ConsultaOutcome.Resultado(parsed)
                    is ParsedResult.Error -> ConsultaOutcome.Error(parsed.mensaje)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción consultando NAP", e)
            ConsultaOutcome.Error("Error de red NAP: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ConsultaREAT.NapClient"
    }
}