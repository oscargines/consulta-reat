package com.oscar.consultareat.ui.coeficiente

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.math.abs

enum class MetodoCalculo {
    W_CONSTANTE,
    NEUMATICOS
}

enum class EstadoCoeficiente {
    DENTRO_DE_PARAMETROS,
    INDICIO_MANIPULACION
}

data class CalculationResult(
    val metodo: MetodoCalculo,
    val diferenciaW: Double?,
    val gamma: Double?,
    val beta: Double?,
    val alfa: Double?,
    val z: Double,
    val estado: EstadoCoeficiente
)

private const val TOLERANCIA_L = 1e-6
private const val UMBRAL_MANIPULACION = 4.0

fun calcularResultado(wa: Double, la: Double, wp: Double, lp: Double): CalculationResult {
    val mismaL = abs(la - lp) < TOLERANCIA_L

    if (mismaL) {
        val diferenciaW = wp - wa
        val z = (diferenciaW * 100.0) / wa
        return CalculationResult(
            metodo = MetodoCalculo.W_CONSTANTE,
            diferenciaW = diferenciaW,
            gamma = null,
            beta = null,
            alfa = null,
            z = z,
            estado = if (abs(z) > UMBRAL_MANIPULACION) {
                EstadoCoeficiente.INDICIO_MANIPULACION
            } else {
                EstadoCoeficiente.DENTRO_DE_PARAMETROS
            }
        )
    }

    val gamma = wa * la
    val beta = wp * lp
    val alfa = beta - gamma
    val z = (alfa * 100.0) / gamma

    return CalculationResult(
        metodo = MetodoCalculo.NEUMATICOS,
        diferenciaW = null,
        gamma = gamma,
        beta = beta,
        alfa = alfa,
        z = z,
        estado = if (abs(z) > UMBRAL_MANIPULACION) {
            EstadoCoeficiente.INDICIO_MANIPULACION
        } else {
            EstadoCoeficiente.DENTRO_DE_PARAMETROS
        }
    )
}

class CoeficienteCaracteristicoViewModel : ViewModel() {
    var coeficienteWa by mutableStateOf("")
    var circunferenciaLa by mutableStateOf("")
    var coeficienteWp by mutableStateOf("")
    var circunferenciaLp by mutableStateOf("")

    var calculationResult by mutableStateOf<CalculationResult?>(null)
    var error by mutableStateOf<String?>(null)
    var showResultDialog by mutableStateOf(false)

    fun onCoeficienteWaChange(value: String) {
        coeficienteWa = value
        limpiarResultado()
    }

    fun onCircunferenciaLaChange(value: String) {
        circunferenciaLa = value
        limpiarResultado()
    }

    fun onCoeficienteWpChange(value: String) {
        coeficienteWp = value
        limpiarResultado()
    }

    fun onCircunferenciaLpChange(value: String) {
        circunferenciaLp = value
        limpiarResultado()
    }

    private fun limpiarResultado() {
        calculationResult = null
        error = null
    }

    fun calculate() {
        error = null

        val wa = coeficienteWa.replace(',', '.').toDoubleOrNull()
        val la = circunferenciaLa.replace(',', '.').toDoubleOrNull()
        val wp = coeficienteWp.replace(',', '.').toDoubleOrNull()
        val lp = circunferenciaLp.replace(',', '.').toDoubleOrNull()

        if (wa == null || wa <= 0) {
            error = "Introduce un coeficiente característico (Wa) válido"
            return
        }
        if (la == null || la <= 0) {
            error = "Introduce una circunferencia efectiva (La) válida"
            return
        }
        if (wp == null || wp <= 0) {
            error = "Introduce un coeficiente característico (Wp) válido"
            return
        }
        if (lp == null || lp <= 0) {
            error = "Introduce una circunferencia efectiva (Lp) válida"
            return
        }

        calculationResult = calcularResultado(wa, la, wp, lp)
        showResultDialog = true
    }

    fun dismissResultDialog() {
        showResultDialog = false
    }

    fun reset() {
        coeficienteWa = ""
        circunferenciaLa = ""
        coeficienteWp = ""
        circunferenciaLp = ""
        calculationResult = null
        error = null
        showResultDialog = false
    }

    val isFormValid: Boolean
        get() = coeficienteWa.isNotBlank() && circunferenciaLa.isNotBlank() &&
                coeficienteWp.isNotBlank() && circunferenciaLp.isNotBlank()
}

class CoeficienteCaracteristicoViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CoeficienteCaracteristicoViewModel() as T
    }
}
