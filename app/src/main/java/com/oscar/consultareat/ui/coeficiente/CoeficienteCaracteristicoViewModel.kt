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

data class CalculationResult(
    val lTeorica: Double,
    val lRevision: Double,
    val porcentajeDesvio: Double,
    val hayFraude: Boolean
)

class CoeficienteCaracteristicoViewModel : ViewModel() {
    var coeficienteW by mutableStateOf("")
    var circunferenciaL by mutableStateOf("")
    var circunferenciaLr by mutableStateOf("")
    var tamanoRueda by mutableStateOf("")
    
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

    fun onTamanoRuedaChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        val formatted = formatTireSizeInput(filtered)
        tamanoRueda = formatted
        calculationResult = null
        error = null
    }

    private fun formatTireSizeInput(digitsOnly: String): String {
        val sb = StringBuilder()
        var idx = 0
        
        // Ancho: 2-3 dígitos
        val widthLen = if (digitsOnly.length >= 3) 3 else 2
        if (digitsOnly.isNotEmpty()) {
            sb.append(digitsOnly.substring(0, minOf(widthLen, digitsOnly.length)))
            idx = minOf(widthLen, digitsOnly.length)
        }
        
        // Perfil: 1-2 dígitos después de /
        if (idx < digitsOnly.length) {
            sb.append("/")
            val profileEnd = minOf(idx + 2, digitsOnly.length)
            sb.append(digitsOnly.substring(idx, profileEnd))
            idx = profileEnd
        }
        
        // Diámetro: resto con posible decimal
        if (idx < digitsOnly.length) {
            sb.append(" R ")
            val remaining = digitsOnly.substring(idx)
            if (remaining.length <= 2) {
                sb.append(remaining)
            } else {
                sb.append(remaining.substring(0, 2))
                if (remaining.length > 2) {
                    sb.append(".")
                    sb.append(remaining.substring(2, minOf(remaining.length, 3)))
                }
            }
        }
        
        return sb.toString()
    }

    fun getTireSizeRawValue(): String {
        return tamanoRueda.filter { it.isDigit() || it == '.' }
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
        if (tamanoRueda.trim().isBlank()) {
            error = "Introduce el tamaño de la rueda (ej. 315/70 R 22.5)"
            return
        }

        val tireSpec = parseTireSize(tamanoRueda.trim())
        if (tireSpec == null) {
            error = "Formato de rueda inválido. Usa: 315/70 R 22.5"
            return
        }

        isCalculating = true
        
        CoroutineScope(Dispatchers.Default).launch {
            val lTeorica = calculateLTeorica(tireSpec)
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

    private fun parseTireSize(size: String): TireSpec? {
        // Acepta tanto formato con separadores (315/70 R 22.5) como solo dígitos
        val cleanSize = size.filter { it.isDigit() || it == '.' || it == '/' || it == 'r' || it == 'R' || it == ' ' }
        
        // Primero intenta con formato completo
        val regex = """^(\d{2,3})/(\d{1,2})\s*[Rr]\s*(\d+(?:\.\d+)?)$""".toRegex()
        val match = regex.matchEntire(cleanSize)
        
        if (match != null) {
            val width = match.groupValues[1].toIntOrNull()
            val profile = match.groupValues[2].toIntOrNull()
            val rim = match.groupValues[3].toDoubleOrNull()
            
            if (width != null && profile != null && rim != null &&
                width in 150..500 && profile in 20..100 && rim in 15.0..30.0) {
                return TireSpec(widthMm = width, profilePercent = profile, rimInches = rim)
            }
        }
        
        // Si no coincide con formato completo, intenta extraer solo dígitos
        val digitsOnly = cleanSize.filter { it.isDigit() || it == '.' }
        return parseTireSizeFromDigits(digitsOnly)
    }

    private fun parseTireSizeFromDigits(digits: String): TireSpec? {
        // Intenta parsear solo dígitos: ancho(3) + perfil(2) + diámetro(resto)
        if (digits.length < 5) return null
        
        val width = digits.substring(0, 3).toIntOrNull()
        if (width == null || width !in 150..500) return null
        
        val profileStart = 3
        val profileEnd = minOf(5, digits.length)
        val profile = digits.substring(profileStart, profileEnd).toIntOrNull()
        if (profile == null || profile !in 20..100) return null
        
        val rimStr = if (digits.length > 5) digits.substring(5) else ""
        if (rimStr.isBlank()) return null
        
        val rim = rimStr.toDoubleOrNull()
        if (rim == null || rim !in 15.0..30.0) return null
        
        return TireSpec(widthMm = width, profilePercent = profile, rimInches = rim)
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
        val lTeorica = diametroTotal * 3.1416
        
        return lTeorica
    }

    fun dismissResultDialog() {
        showResultDialog = false
    }

    fun reset() {
        coeficienteW = ""
        circunferenciaL = ""
        circunferenciaLr = ""
        tamanoRueda = ""
        calculationResult = null
        error = null
        isCalculating = false
        showResultDialog = false
    }

    val isFormValid: Boolean
        get() = coeficienteW.isNotBlank() && circunferenciaL.isNotBlank() && 
                circunferenciaLr.isNotBlank() && tamanoRueda.isNotBlank()
}

class CoeficienteCaracteristicoViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CoeficienteCaracteristicoViewModel() as T
    }
}