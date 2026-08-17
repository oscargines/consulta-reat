package com.oscar.consultareat.ui.consulta

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class DeteccionMatricula(
    val texto: String,
    val rect: Rect
)

class MatriculaOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun reconocerConCaja(bitmap: Bitmap): DeteccionMatricula? =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { texto ->
                    cont.resume(buscarMatricula(texto))
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

    private fun buscarMatricula(texto: Text): DeteccionMatricula? {
        for (bloque in texto.textBlocks) {
            for (linea in bloque.lines) {
                for (elemento in linea.elements) {
                    val normalizado = normalizar(elemento.text)
                    val caja = elemento.boundingBox
                    if (esMatricula(normalizado) && caja != null) {
                        return DeteccionMatricula(normalizado, caja)
                    }
                }
            }
        }
        for (bloque in texto.textBlocks) {
            for (linea in bloque.lines) {
                val normalizado = normalizar(linea.text)
                val caja = linea.boundingBox
                if (esMatricula(normalizado) && caja != null) {
                    return DeteccionMatricula(normalizado, caja)
                }
            }
        }
        return null
    }

    private fun normalizar(s: String): String =
        s.uppercase().replace(Regex("[^A-Z0-9]"), "")

    private fun esMatricula(s: String): Boolean =
        s.length in 6..7 && s.any { it.isDigit() } && s.any { it.isLetter() }
}