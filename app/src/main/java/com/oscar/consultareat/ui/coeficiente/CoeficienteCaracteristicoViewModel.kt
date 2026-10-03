package com.oscar.consultareat.ui.coeficiente

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class TireSpec(
    val widthMm: Int,
    val profilePercent: Int,
    val rimInches: Double
)

data class TireOption(
    val label: String,
    val spec: TireSpec
)

data class CalculationResult(
    val lTeorica: Double,
    val lRevision: Double,
    val porcentajeDesvio: Double,
    val hayFraude: Boolean
)

private const val PI_TEORICA = 3.1416
private const val ALTURA_MIN_MM = 400.0
private const val ALTURA_MAX_MM = 1500.0

val CATALOGO_RUEDAS: List<TireOption> = listOf(
    TireOption("315/70 R 22.5", TireSpec(315, 70, 22.5)),
    TireOption("295/80 R 22.5", TireSpec(295, 80, 22.5)),
    TireOption("385/65 R 22.5", TireSpec(385, 65, 22.5)),
    TireOption("275/70 R 22.5", TireSpec(275, 70, 22.5)),
    TireOption("305/70 R 22.5", TireSpec(305, 70, 22.5)),
    TireOption("315/80 R 22.5", TireSpec(315, 80, 22.5)),
    TireOption("295/60 R 22.5", TireSpec(295, 60, 22.5)),
    TireOption("385/60 R 22.5", TireSpec(385, 60, 22.5)),
    TireOption("265/70 R 19.5", TireSpec(265, 70, 19.5)),
    TireOption("245/70 R 19.5", TireSpec(245, 70, 19.5)),
    TireOption("235/75 R 17.5", TireSpec(235, 75, 17.5)),
    TireOption("215/75 R 17.5", TireSpec(215, 75, 17.5)),
    TireOption("195/65 R 17.5", TireSpec(195, 65, 17.5)),
    TireOption("215/65 R 16", TireSpec(215, 65, 16.0)),
    TireOption("225/65 R 16", TireSpec(225, 65, 16.0)),
    TireOption("205/60 R 16", TireSpec(205, 60, 16.0)),
    TireOption("205/55 R 16", TireSpec(205, 55, 16.0)),
    TireOption("215/55 R 17", TireSpec(215, 55, 17.0)),
    TireOption("225/45 R 17", TireSpec(225, 45, 17.0)),
    TireOption("195/65 R 15", TireSpec(195, 65, 15.0))
)

class CoeficienteCaracteristicoViewModel : ViewModel() {
    var coeficienteW by mutableStateOf("")
    var circunferenciaL by mutableStateOf("")
    var circunferenciaLr by mutableStateOf("")
    var tamanoRuedaSeleccionada by mutableStateOf<TireOption?>(null)

    var showAlturaDialog by mutableStateOf(false)
    var alturaNeumatico by mutableStateOf("")
    var alturaError by mutableStateOf<String?>(null)
    var alturaConfirmadaMm by mutableStateOf<Double?>(null)

    var calculationResult by mutableStateOf<CalculationResult?>(null)
    var error by mutableStateOf<String?>(null)
    var isCalculating by mutableStateOf(false)
    var showResultDialog by mutableStateOf(false)

    fun onCoeficienteWChange(value: String) {
        coeficienteW = value
        calculationResult = null
        error = null
    }

    fun onCircunferenciaLChange(value: String) {
        circunferenciaL = value
        calculationResult = null
        error = null
    }

    fun onCircunferenciaLrChange(value: String) {
        circunferenciaLr = value
        calculationResult = null
        error = null
    }

    fun onTamanoRuedaSeleccionada(opcion: TireOption?) {
        tamanoRuedaSeleccionada = opcion
        if (opcion != null) {
            alturaConfirmadaMm = null
            alturaNeumatico = ""
            alturaError = null
        }
        calculationResult = null
        error = null
    }

    fun abrirDialogoAltura() {
        alturaNeumatico = alturaConfirmadaMm?.let { formatearAltura(it) } ?: ""
        alturaError = null
        showAlturaDialog = true
    }

    fun cancelarDialogoAltura() {
        showAlturaDialog = false
        alturaError = null
    }

    fun onAlturaChange(value: String) {
        val limpio = value.filter { it.isDigit() || it == '.' || it == ',' }
            .replace(',', '.')
        var resultado = ""
        var puntoVisto = false
        for (c in limpio) {
            if (c == '.') {
                if (puntoVisto) continue
                puntoVisto = true
            }
            resultado += c
        }
        alturaNeumatico = resultado.take(6)
        alturaError = null
        calculationResult = null
        error = null
    }

    fun confirmarAltura() {
        val valor = alturaNeumatico.replace(',', '.').toDoubleOrNull()
        if (valor == null || valor < ALTURA_MIN_MM || valor > ALTURA_MAX_MM) {
            alturaError = "Introduce una altura total válida (entre 400 y 1500 mm)"
            return
        }
        alturaConfirmadaMm = valor
        tamanoRuedaSeleccionada = null
        alturaError = null
        showAlturaDialog = false
        calculationResult = null
        error = null
    }

    fun limpiarAltura() {
        alturaConfirmadaMm = null
        alturaNeumatico = ""
        alturaError = null
        calculationResult = null
        error = null
    }

    fun lTeoricaPorAltura(): Double? = alturaConfirmadaMm?.let { it * PI_TEORICA }

    private fun formatearAltura(valor: Double): String {
        return if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()
    }

    fun calculate() {
        error = null
        
        val w = coeficienteW.replace(',', '.').toDoubleOrNull()
        val l = circunferenciaL.replace(',', '.').toDoubleOrNull()
        val lr = circunferenciaLr.replace(',', '.').toDoubleOrNull()
        
        if (w == null || w <= 0) {
            error = "Introduce un coeficiente característico (W) válido"
            return
        }
        if (l == null || l <= 0) {
            error = "Introduce una circunferencia efectiva (L) válida"
            return
        }
        if (lr == null || lr <= 0) {
            error = "Introduce una circunferencia de revisión (Lr) válida"
            return
        }
        val specSeleccionada = tamanoRuedaSeleccionada?.spec
        val alturaMedida = alturaConfirmadaMm
        if (specSeleccionada == null && alturaMedida == null) {
            error = "Selecciona el tamaño de la rueda o mide la altura del neumático"
            return
        }

        isCalculating = true

        CoroutineScope(Dispatchers.Default).launch {
            val lTeorica = if (specSeleccionada != null) {
                calculateLTeorica(specSeleccionada)
            } else {
                calculateLTeoricaPorAltura(alturaMedida!!)
            }
            val porcentajeDesvio = (lTeorica / lr) * 100
            val hayFraude = porcentajeDesvio >= 104.0 || porcentajeDesvio <= 96.0

            calculationResult = CalculationResult(
                lTeorica = lTeorica,
                lRevision = lr,
                porcentajeDesvio = porcentajeDesvio,
                hayFraude = hayFraude
            )
            isCalculating = false
            showResultDialog = true
        }
    }

    private fun calculateLTeorica(spec: TireSpec): Double {
        // Clave 1: Calcular la altura de la goma (el perfil) x 2
        // Fórmula: (Ancho × (Perfil / 100)) × 2
        val alturaGoma = (spec.widthMm * (spec.profilePercent / 100.0)) * 2

        // Clave 2: Convertir la llanta a milímetros
        // Fórmula: Pulgadas de la llanta × 25.4
        val diametroLlantaMm = spec.rimInches * 25.4

        // Clave 3: Obtener el diámetro total y multiplicarlo por Pi
        // Fórmula: (Altura de goma + Diámetro llanta) × 3.1416
        val diametroTotal = alturaGoma + diametroLlantaMm
        val lTeorica = diametroTotal * PI_TEORICA

        return lTeorica
    }

    private fun calculateLTeoricaPorAltura(alturaMm: Double): Double {
        // Altura total medida (diámetro exterior) × Pi
        return alturaMm * PI_TEORICA
    }

    fun dismissResultDialog() {
        showResultDialog = false
    }

    fun reset() {
        coeficienteW = ""
        circunferenciaL = ""
        circunferenciaLr = ""
        tamanoRuedaSeleccionada = null
        showAlturaDialog = false
        alturaNeumatico = ""
        alturaError = null
        alturaConfirmadaMm = null
        calculationResult = null
        error = null
        isCalculating = false
        showResultDialog = false
    }

    val isFormValid: Boolean
        get() = coeficienteW.isNotBlank() && circunferenciaL.isNotBlank() &&
                circunferenciaLr.isNotBlank() &&
                (tamanoRuedaSeleccionada != null || alturaConfirmadaMm != null)
}

class CoeficienteCaracteristicoViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CoeficienteCaracteristicoViewModel() as T
    }
}