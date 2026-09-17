package com.oscar.consultareat.data.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.oscar.consultareat.domain.ActaInspeccionData
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.printer.SGD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

private const val TAG = "PrinterInspeccion"
private val CHARS = StandardCharsets.ISO_8859_1

// ============================================================
// PAPER SPECIFICATION — Zebra RW420 y ZQ521 @ 203 DPI
// ============================================================
// Especificación técnica de papel térmico Zebra:
// - Ancho físico: 832 dots @ 203 DPI (aprox 104 mm / 4.1")
// - Altura: continua (rollo)
// - Márgenes: 20 dots lado + 120 dots abajo
// - Fuente: CPCL (Common Printer Command Language)
// ============================================================
private const val PAPER_W_DOTS  = 792
private const val MARGIN        = 20
private const val MARGIN_BOTTOM = 120

private val PAPER_W get() = PAPER_W_DOTS

// ============================================================
// TÍTULO  — Font 7 size 3
// ============================================================
private const val TITLE_FONT   = 7
private const val TITLE_SIZE   = 3
private const val TITLE_LINE_H = 34

// ============================================================
// CUERPO  — Font 7 size 2
// ============================================================
private const val BODY_FONT   = 7
private const val BODY_SIZE   = 2
private const val BODY_LINE_H = 22

// ============================================================
// DELIMITADORES CPCL
// ============================================================
private const val CMD_START = "! 0 200 200 %d 1\r\nPAGE-WIDTH %d\r\n"
private const val CMD_TEXT  = "TEXT %d %d %d %d %s\r\n%s\r\n"
private const val CMD_BOX   = "BOX %d %d %d %d %d\r\n"
private const val CMD_LINE  = "LINE %d %d %d %d %d\r\n"
private const val CMD_FORM  = "FORM\r\nPRINT\r\n"

private const val DELAY_BETWEEN_DOCS_MS = 5000L
private const val FINAL_DRAIN_BEFORE_CLOSE_MS = 15000L

/**
 * Configura la sesión de impresora: lenguaje CPCL y perfil según modelo.
 */
private fun configurePrinterSession(connection: Connection) {
    fun safeSetSgd(key: String, value: String) {
        try {
            SGD.SET(key, value, connection)
            Log.d(TAG, "SGD $key=$value aplicado")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo aplicar SGD $key=$value: ${e.message}")
        }
    }

    safeSetSgd("device.languages", "cpcl")

    val model = try {
        SGD.GET("device.product_name", connection).orEmpty()
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo leer device.product_name: ${e.message}")
        ""
    }

    if (model.contains("ZQ521", ignoreCase = true)) {
        safeSetSgd("media.type", "continuous")
        safeSetSgd("ezpl.print_mode", "tear_off")
        safeSetSgd("power.up_action", "no-motion")
        safeSetSgd("head.close_action", "no-motion")
        Log.d(TAG, "Perfil ZQ521 aplicado (media continuo)")
    } else {
        Log.d(TAG, "Perfil por defecto aplicado para modelo='$model'")
    }
}

/**
 * Abre una conexión Bluetooth y configura el lenguaje CPCL.
 * Llamar UNA VEZ antes de imprimir una serie de documentos.
 * Cerrar siempre con [closeSharedBtConnection] en un bloque finally.
 */
internal suspend fun openSharedBtConnection(mac: String): Connection =
    withContext(Dispatchers.IO) {
        val conn = BluetoothConnection(mac, 5_000, 2_000)
        conn.open()
        Log.d(TAG, "BT compartido abierto mac=$mac")
        configurePrinterSession(conn)
        Thread.sleep(500)
        conn
    }

/** Cierra la conexión BT compartida obtenida con [openSharedBtConnection]. */
internal suspend fun closeSharedBtConnection(conn: Connection) =
    withContext(Dispatchers.IO) {
        try { conn.close() } catch (_: IOException) {}
        Log.d(TAG, "BT compartido cerrado")
    }

// ============================================================
// sendToPrinter
// Si se pasa sharedConn (conexión ya abierta) se reutiliza;
// si no, se abre y cierra una conexión propia (uso individual).
// ============================================================
/**
 * Envía comandos CPCL a impresora Zebra.
 *
 * Si `sharedConn` es null, abre/cierra conexión individual.
 * Si no es null, reutiliza conexión compartida para lote.
 */
private suspend fun sendToPrinter(
    context: Context, mac: String,
    contentH: Int, bodyCpcl: String, pw: Int,
    sharedConn: Connection? = null
) {
    return withContext(Dispatchers.IO) {
        if (sharedConn != null) {
            sharedConn.write(
                String.format(CMD_START, contentH, pw)
                    .toByteArray(CHARS)
            )
            sharedConn.write(bodyCpcl.toByteArray(CHARS))
            Log.d(TAG, "Enviados ${bodyCpcl.length} bytes (conexión compartida)")
            Thread.sleep(4000)
        } else {
            var connection: Connection? = null
            try {
                connection = BluetoothConnection(mac, 5_000, 2_000)
                connection.open()
                Log.d(TAG, "BT abierto")
                configurePrinterSession(connection)
                Thread.sleep(500)
                connection.write(
                    String.format(CMD_START, contentH, pw)
                        .toByteArray(CHARS)
                )
                connection.write(bodyCpcl.toByteArray(CHARS))
                Log.d(TAG, "Enviados ${bodyCpcl.length} bytes")
                Thread.sleep(4000)
            } finally {
                try { connection?.close() } catch (_: IOException) {}
                Log.d(TAG, "BT cerrado")
            }
        }
    }
}

private fun showNoMac(context: Context) =
    Toast.makeText(context, "No hay impresora configurada", Toast.LENGTH_SHORT).show()

private fun showError(context: Context, e: Exception) {
    Log.e(TAG, "Error: ${e.message}", e)
    CoroutineScope(Dispatchers.Main).launch {
        Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Construye el cuerpo CPCL para el acta de inspección.
 */
private fun buildActaInspeccionCpcl(data: ActaInspeccionData): String {
    val lines = mutableListOf<String>()
    var y = MARGIN

    fun addText(text: String, font: Int = BODY_FONT, size: Int = BODY_SIZE, lineH: Int = BODY_LINE_H, bold: Boolean = false, x: Int = MARGIN) {
        val style = if (bold) "B" else ""
        lines.add(String.format(CMD_TEXT, font, size, x, y, style, text))
        y += lineH
    }

    fun addTitle(text: String) {
        addText(text, TITLE_FONT, TITLE_SIZE, TITLE_LINE_H, bold = true, x = (PAPER_W - text.length * 12) / 2)
    }

    fun addSeparator() {
        lines.add(String.format(CMD_LINE, MARGIN, y, PAPER_W - MARGIN, y, 2))
        y += BODY_LINE_H
    }

    fun addBox(label: String, value: String) {
        val labelW = label.length * 10
        val valueW = value.length * 10
        val boxW = maxOf(labelW, valueW) + 40
        val boxH = BODY_LINE_H * 2 + 10
        val boxX = (PAPER_W - boxW) / 2

        lines.add(String.format(CMD_BOX, boxX, y, boxX + boxW, y + boxH, 2))
        addText(label, x = boxX + 10, bold = true)
        addText(value, x = boxX + 10)
        y += 10
    }

    // Título principal
    addTitle("ACTA DE INSPECCIÓN")
    addTitle("TRANSPORTE ESCOLAR")
    addSeparator()

    // Datos del acta
    addText("Fecha: ${data.fechaInspeccion}    Hora: ${data.horaInspeccion}", bold = true)
    addSeparator()

    // Datos del vehículo
    addText("DATOS DEL VEHÍCULO", bold = true)
    addBox("Matrícula", data.matricula)
    addSeparator()

    // Datos de la empresa
    addText("EMPRESA TITULAR", bold = true)
    addBox("Empresa", data.empresaTitular)
    addSeparator()

    // Datos de la autorización
    addText("AUTORIZACIÓN DE TRANSPORTE", bold = true)
    addBox("Nº Autorización", data.numeroAutorizacion)
    addSeparator()

    // Firma
    addText("Firma del Inspector:", bold = true)
    y += BODY_LINE_H * 3
    lines.add(String.format(CMD_LINE, MARGIN, y, PAPER_W - MARGIN, y, 1))

    val contentH = y + MARGIN_BOTTOM
    return String.format(CMD_START, contentH, PAPER_W) + lines.joinToString("")
}

/**
 * Imprime el acta de inspección en impresora Zebra.
 *
 * @param context Contexto de la aplicación
 * @param mac Dirección MAC de la impresora
 * @param data Datos del acta a imprimir
 */
fun printActaInspeccion(context: Context, mac: String, data: ActaInspeccionData) {
    if (mac.isBlank()) {
        showNoMac(context)
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val cpcl = buildActaInspeccionCpcl(data)
            val contentH = cpcl.lines().size * BODY_LINE_H + MARGIN_BOTTOM + 200
            sendToPrinter(context, mac, contentH, cpcl, PAPER_W)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Acta enviada a impresora", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            showError(context, e)
        }
    }
}