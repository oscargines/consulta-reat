package com.oscar.consultareat.data.client

import android.util.Log
import com.oscar.consultareat.data.api.ReatApiClient
import com.oscar.consultareat.data.parser.ParsedResult
import com.oscar.consultareat.data.parser.RgtHtmlParser
import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.TipoIdentificacion
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed interface ConsultaOutcome {
    data class Resultado(val parsed: ParsedResult) : ConsultaOutcome
    data object RequiereCaptcha : ConsultaOutcome
    data class Error(val mensaje: String) : ConsultaOutcome
}

class RgtClient(
    private val parser: RgtHtmlParser = RgtHtmlParser(),
    val apiClient: ReatApiClient? = null
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun consultar(request: ConsultaRequest): ConsultaOutcome {
        val viaApi = apiClient?.takeIf { it.esConfigurada }
        if (viaApi != null) {
            Log.d(TAG, "API REAT configurada, intentando consulta vía API")
            val resultado = viaApi.consultar(request)
            Log.d(TAG, "Resultado API: ${resultado::class.simpleName}")
            if (resultado is ConsultaOutcome.Resultado || resultado is ConsultaOutcome.RequiereCaptcha) {
                return resultado
            }
        } else {
            Log.d(TAG, "API REAT no configurada, usando flujo web")
        }

        val tpsolic = when (request.tipoConsulta) {
            TipoConsulta.AUTORIZACIONES -> "E"
            TipoConsulta.COMPETENCIA_CONSEJERO_CAP -> "P"
            TipoConsulta.VEHICULO -> "M"
        }

        val accion = when (request.tipoIdentificacion) {
            TipoIdentificacion.NIF -> "consultar_nif"
            TipoIdentificacion.NOMBRE -> "consultar_empre"
            TipoIdentificacion.MATRICULA -> "consultar_nif"
            TipoIdentificacion.AUTORIZACION -> "consultar_auto"
            TipoIdentificacion.LICENCIA_INTERNACIONAL -> "consultar_inter"
        }

        val formParams = "modulo=datosconsulta&_ftkn=&g-recaptcha-response=&_vld_empresa=&tpsolic=$tpsolic&accion=$accion&consulta=${request.valor}&btnOk=Consultar"

        val httpRequest = Request.Builder()
            .url("https://apps.fomento.gob.es/crgt/servlet/ServletController")
            .post(formParams.toRequestBody())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Referer", baseUrl())
            .addHeader("User-Agent", "ConsultaREAT Android App - contacto@oscar.com")
            .build()

        return try {
            Log.d(TAG, "Enviando POST web: accion=$accion, tpsolic=$tpsolic, consulta=${request.valor}")
            client.newCall(httpRequest).execute().use { response ->
                Log.d(TAG, "Respuesta HTTP: ${response.code}")
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP error: ${response.code}")
                    return ConsultaOutcome.Error("Error de red: ${response.code}")
                }
                val html = response.body?.string() ?: return ConsultaOutcome.Error("Respuesta vacía del servidor")
                Log.d(TAG, "HTML recibido: ${html.length} caracteres")
                when {
                    esFormularioCaptcha(html) -> {
                        Log.w(TAG, "Detectado formulario con captcha")
                        ConsultaOutcome.RequiereCaptcha
                    }
                    else -> {
                        val parsed = parser.parse(html, request.tipoConsulta)
                        Log.d(TAG, "Parser devolvió: ${parsed::class.simpleName}")
                        ConsultaOutcome.Resultado(parsed)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en POST web", e)
            ConsultaOutcome.Error("Error de red: ${e.message}")
        }
    }

    private fun esFormularioCaptcha(html: String): Boolean {
        return html.contains("id=\"consultaForm\"") ||
            html.contains("id='consultaForm'")
    }

    companion object {
        fun baseUrl() = "https://apps.fomento.gob.es/crgt/servlet/ServletController?modulo=datosconsulta&accion=inicio&lang=es&estilo=default"

        private const val TAG = "ConsultaREAT.RgtClient"
    }
}