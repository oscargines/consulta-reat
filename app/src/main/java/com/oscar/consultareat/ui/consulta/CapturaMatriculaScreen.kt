package com.oscar.consultareat.ui.consulta

import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.oscar.consultareat.ui.theme.VerdeEstado
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

private data class DeteccionVista(
    val texto: String,
    val rect: Rect,
    val anchoImg: Int,
    val altoImg: Int
)

@OptIn(ExperimentalGetImage::class)
@Composable
fun CapturaMatriculaScreen(
    onMatricula: (String) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var deteccion by remember { mutableStateOf<DeteccionVista?>(null) }
    var confirmando by remember { mutableStateOf<String?>(null) }
    var cooldownHasta by remember { mutableStateOf(0L) }

    val previewView = remember { PreviewView(context) }
    val analisis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    val preview = remember { Preview.Builder().build() }
    val ocr = remember { MatriculaOcr() }
    val ejecutorAnalisis: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val analizando = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        val futuro = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = runCatching { futuro.get() }.getOrNull() ?: return@Runnable
            provider.unbindAll()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            var estable: String? = null
            var contador = 0
            analisis.setAnalyzer(ejecutorAnalisis) { imageProxy ->
                if (analizando.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val ahora = System.currentTimeMillis()
                if (confirmando != null || ahora < cooldownHasta) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                analizando.set(true)
                val bitmap = runCatching { imageProxy.toBitmap() }.getOrNull()
                imageProxy.close()
                if (bitmap != null) {
                    scope.launch {
                        val det = ocr.reconocerConCaja(bitmap)
                        if (confirmando == null && System.currentTimeMillis() >= cooldownHasta) {
                            if (det != null) {
                                deteccion = DeteccionVista(det.texto, det.rect, bitmap.width, bitmap.height)
                                if (det.texto == estable) {
                                    contador++
                                    if (contador >= 3) {
                                        confirmando = det.texto
                                    }
                                } else {
                                    estable = det.texto
                                    contador = 1
                                }
                            } else {
                                deteccion = null
                                estable = null
                                contador = 0
                            }
                        }
                        analizando.set(false)
                    }
                } else {
                    analizando.set(false)
                }
            }
            runCatching {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analisis
                )
            }
        }
        futuro.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { ejecutorAnalisis.shutdown() }
            val futuro2 = ProcessCameraProvider.getInstance(context)
            futuro2.addListener(
                { runCatching { futuro2.get().unbindAll() } },
                ContextCompat.getMainExecutor(context)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val ancho = size.width
            val alto = size.height
            val gw = ancho * 0.78f
            val gh = gw * 0.32f
            val gt = alto * 0.28f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset((ancho - gw) / 2f, gt),
                size = Size(gw, gh),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            deteccion?.let { d ->
                val caja = mapearCaja(d.rect, d.anchoImg, d.altoImg, ancho, alto)
                drawRoundRect(
                    color = VerdeEstado,
                    topLeft = Offset(caja.left, caja.top),
                    size = Size(caja.width(), caja.height()),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        IconButton(
            onClick = onCancelar,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Cerrar escáner",
                tint = Color.White
            )
        }

        Text(
            text = "Enfoca la cámara hacia la matrícula",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 32.dp)
        )
    }

    confirmando?.let { texto ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Matrícula detectada") },
            text = {
                Column {
                    Text("Se ha reconocido la siguiente matrícula:")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = texto,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onMatricula(texto) }) { Text("Usar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmando = null
                    deteccion = null
                    cooldownHasta = System.currentTimeMillis() + 1500L
                }) { Text("Reintentar") }
            }
        )
    }
}

private fun mapearCaja(rect: Rect, anchoImg: Int, altoImg: Int, anchoVista: Float, altoVista: Float): RectF {
    val escala = max(anchoVista / anchoImg, altoVista / altoImg)
    val anchoEscalado = anchoImg * escala
    val altoEscalado = altoImg * escala
    val offX = (anchoEscalado - anchoVista) / 2f
    val offY = (altoEscalado - altoVista) / 2f
    return RectF(
        rect.left * escala - offX,
        rect.top * escala - offY,
        rect.right * escala - offX,
        rect.bottom * escala - offY
    )
}