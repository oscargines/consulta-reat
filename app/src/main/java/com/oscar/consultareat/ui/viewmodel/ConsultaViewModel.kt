package com.oscar.consultareat.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.consultareat.GuardadoResultado
import com.oscar.consultareat.data.client.ConsultaOutcome
import com.oscar.consultareat.data.parser.ParsedResult
import com.oscar.consultareat.data.parser.RgtHtmlParser
import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.ConsultaResultado
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.toConsultaResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val resultado: ConsultaResultado) : UiState
    data class CaptchaRequerido(val request: ConsultaRequest) : UiState
    data class Error(val message: String) : UiState
}

class ConsultaViewModel(
    private val repository: com.oscar.consultareat.RgtRepository,
    private val parser: RgtHtmlParser = RgtHtmlParser()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _avisoDuplicado = MutableStateFlow<String?>(null)
    val avisoDuplicado: StateFlow<String?> = _avisoDuplicado.asStateFlow()

    private var ultimaConsulta: ConsultaRequest? = null

    val historial = repository.getHistorial()

    fun ejecutarConsulta(request: ConsultaRequest) {
        Log.d(TAG, "ejecutarConsulta llamado con $request")
        ultimaConsulta = request
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val outcome = repository.ejecutar(request)
                Log.d(TAG, "repository.ejecutar devolvió: $outcome")
                when (outcome) {
                    is ConsultaOutcome.Resultado -> {
                        val parsed = outcome.parsed
                        when (parsed) {
                            is ParsedResult.Success -> {
                                Log.d(TAG, "Success detalle: identidad=[${parsed.identidadLabel}]='${parsed.identidadValor}', autorizaciones=${parsed.autorizaciones.size}, vehiculos=${parsed.vehiculos.size}, competencia=${parsed.competenciaProfesional.size}, consejero=${parsed.consejeroSeguridad.size}, cap=${parsed.capConductor.size}, operadores=${parsed.operadores.size}, datasets=${parsed.conjuntosDatos.size}")
                                val enriquecido = enriquecerSiAplica(parsed)
                                _uiState.value = UiState.Success(enriquecido.toConsultaResultado())
                                guardarYAvisar(request, enriquecido)
                            }
                            is ParsedResult.Error -> {
                                Log.w(TAG, "ParsedResult.Error: ${parsed.mensaje}")
                                _uiState.value = UiState.Error(parsed.mensaje)
                            }
                        }
                    }
                    is ConsultaOutcome.RequiereCaptcha -> {
                        Log.w(TAG, "Captcha requerido para $request")
                        _uiState.value = UiState.CaptchaRequerido(request)
                    }
                    is ConsultaOutcome.Error -> {
                        Log.w(TAG, "ConsultaOutcome.Error: ${outcome.mensaje}")
                        _uiState.value = UiState.Error(outcome.mensaje)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en ejecutarConsulta", e)
                _uiState.value = UiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun parsearHtmlResultado(html: String, tipoConsulta: TipoConsulta) {
        Log.d(TAG, "parsearHtmlResultado llamado para $tipoConsulta (html=${html.length} chars)")
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val parsed = parser.parse(html, tipoConsulta)
                Log.d(TAG, "parser.parse devolvió: ${parsed::class.simpleName}")
                when (parsed) {
                    is ParsedResult.Success -> {
                        Log.d(TAG, "Success detalle: identidad=[${parsed.identidadLabel}]='${parsed.identidadValor}', autorizaciones=${parsed.autorizaciones.size}, vehiculos=${parsed.vehiculos.size}, competencia=${parsed.competenciaProfesional.size}, consejero=${parsed.consejeroSeguridad.size}, cap=${parsed.capConductor.size}, operadores=${parsed.operadores.size}, datasets=${parsed.conjuntosDatos.size}")
                        val enriquecido = enriquecerSiAplica(parsed)
                        _uiState.value = UiState.Success(enriquecido.toConsultaResultado())
                        ultimaConsulta?.let { request ->
                            guardarYAvisar(request, enriquecido)
                        }
                    }
                    is ParsedResult.Error -> {
                        Log.w(TAG, "ParsedResult.Error en parsearHtmlResultado: ${parsed.mensaje}")
                        _uiState.value = UiState.Error(parsed.mensaje)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en parsearHtmlResultado", e)
                _uiState.value = UiState.Error("Error al procesar la respuesta: ${e.message}")
            }
        }
    }

    fun volverAlFormulario() {
        Log.d(TAG, "volverAlFormulario llamado")
        _uiState.value = UiState.Idle
    }

    fun nuevaConsulta() {
        Log.d(TAG, "nuevaConsulta llamado")
        ultimaConsulta = null
        _uiState.value = UiState.Idle
    }

    fun mostrarHistorialItem(item: com.oscar.consultareat.data.cache.HistorialItem) {
        Log.d(TAG, "mostrarHistorialItem: ${item.valor}")
        _uiState.value = UiState.Success(item.resultado)
    }

    fun eliminarHistorial(item: com.oscar.consultareat.data.cache.HistorialItem) {
        Log.d(TAG, "eliminarHistorial: ${item.id}")
        viewModelScope.launch {
            repository.eliminarHistorial(item.id)
        }
    }

    fun avisoDuplicadoConsumido() {
        _avisoDuplicado.value = null
    }

    private suspend fun guardarYAvisar(request: ConsultaRequest, parsed: ParsedResult.Success) {
        when (repository.guardarResultado(request, parsed)) {
            is GuardadoResultado.Guardado -> Unit
            is GuardadoResultado.Duplicado -> {
                _avisoDuplicado.value =
                    "La consulta \"${request.valor}\" ya existe en el historial y no se añadirá de nuevo."
            }
        }
    }

    private suspend fun enriquecerSiAplica(parsed: ParsedResult.Success): ParsedResult.Success {
        if (parsed.operadores.isNotEmpty()) return parsed
        val nombre = parsed.identidadValor?.takeIf { it.isNotBlank() } ?: return parsed
        Log.d(TAG, "Completando resultado con NAP: nombre='$nombre'")
        return repository.consultarNap(nombre)?.let { nap ->
            parsed.copy(
                operadores = nap.operadores,
                conjuntosDatos = nap.conjuntosDatos
            )
        } ?: parsed
    }

    companion object {
        private const val TAG = "ConsultaREAT.ViewModel"
    }
}