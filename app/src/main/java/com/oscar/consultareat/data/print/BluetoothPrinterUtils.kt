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
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
private const val MARGIN_BOTTOM = 8

private val PAPER_W get() = PAPER_W_DOTS

// ============================================================
// TÍTULO  — Font 7 size 3
// ============================================================
private const val TITLE_FONT   = 7
private const val TITLE_SIZE   = 3
private const val TITLE_LINE_H = 36

// ============================================================
// SUBTÍTULO  — tamaño más pequeño para no solaparse con el título
// ============================================================
private const val SUBTITLE_FONT   = 7
private const val SUBTITLE_SIZE   = 1
private const val SUBTITLE_LINE_H = 24

// ============================================================
// CUERPO  — un punto menor y más separación entre líneas
// ============================================================
private const val BODY_FONT   = 7
private const val BODY_SIZE   = 1
private const val BODY_LINE_H = 34
private const val PARAGRAPH_GAP = 18

// ============================================================
// DELIMITADORES CPCL
// ============================================================
private const val CMD_START = "! 0 200 200 %d 1\r\nPAGE-WIDTH %d\r\n"
private const val CMD_TEXT  = "TEXT %d %d %d %d %s\r\n"
private const val CMD_BOX   = "BOX %d %d %d %d %d\r\n"
private const val CMD_LINE  = "LINE %d %d %d %d %d\r\n"
private const val CMD_FORM  = "FORM\r\nPRINT\r\n"
private const val CMD_CONTINUOUS_MEDIA =
    "! U1 setvar \"media.type\" \"continuous\"\r\n" +
        "! U1 setvar \"media.sense_mode\" \"continuous\"\r\n"

private const val DELAY_BETWEEN_DOCS_MS = 5000L
private const val FINAL_DRAIN_BEFORE_CLOSE_MS = 15000L

sealed interface PrintStatus {
    data object Connecting : PrintStatus
    data object Sending : PrintStatus
    data object Completed : PrintStatus
    data class Failed(val message: String) : PrintStatus
}

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

    // Ambas impresoras usan rollo continuo para este acta. Mantener el mismo
    // perfil evita que la RW420 interprete el contenido como una etiqueta ZPL.
    safeSetSgd("media.type", "continuous")
    safeSetSgd("media.sense_mode", "continuous")
    safeSetSgd("power.up_action", "no-motion")
    safeSetSgd("head.close_action", "no-motion")
    Log.d(TAG, "Perfil CPCL aplicado para modelo='$model'")
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
            (CMD_CONTINUOUS_MEDIA + String.format(CMD_START, contentH, pw))
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
                    (CMD_CONTINUOUS_MEDIA + String.format(CMD_START, contentH, pw))
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
private data class RenderedActa(val height: Int, val body: String)

private data class EgImage(val width: Int, val height: Int, val bytesPerRow: Int, val hexData: String)

private fun bitmapToEg(bitmap: Bitmap): EgImage {
    val width = bitmap.width
    val height = bitmap.height
    val bytesPerRow = (width + 7) / 8
    val lastBits = width % 8
    val lastMask = if (lastBits == 0) 0xFF else (0xFF shl (8 - lastBits)) and 0xFF
    val hex = StringBuilder(bytesPerRow * height * 2)

    for (row in 0 until height) {
        for (byteIndex in 0 until bytesPerRow) {
            var value = 0
            for (bit in 0 until 8) {
                val column = byteIndex * 8 + bit
                if (column < width) {
                    val pixel = bitmap.getPixel(column, row)
                    val luminance = (
                        0.299 * ((pixel shr 16) and 0xFF) +
                            0.587 * ((pixel shr 8) and 0xFF) +
                            0.114 * (pixel and 0xFF)
                        ).toInt()
                    if (luminance < 128) value = value or (1 shl (7 - bit))
                }
            }
            if (byteIndex == bytesPerRow - 1) value = value and lastMask
            hex.append("%02X".format(value))
        }
    }
    return EgImage(width, height, bytesPerRow, hex.toString())
}

private fun loadEscudo(context: Context, assetName: String, maxWidth: Int, maxHeight: Int): EgImage {
    val source = context.assets.open(assetName).use { BitmapFactory.decodeStream(it) }
        ?: throw IOException("No se pudo cargar $assetName")
    val scale = minOf(maxWidth.toFloat() / source.width, maxHeight.toFloat() / source.height, 1f)
    val scaled = Bitmap.createScaledBitmap(
        source,
        (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1),
        true
    )
    val result = bitmapToEg(scaled)
    if (scaled !== source) scaled.recycle()
    source.recycle()
    return result
}

private fun loadEscTrafico(context: Context): EgImage =
    loadEscudo(context, "EscTrafico.png", 220, 120)

private fun loadEscEspana(context: Context): EgImage =
    loadEscudo(context, "EscEspana_bw.png", 120, 120)

private fun wrapActaText(text: String, maxChars: Int): List<String> {
    if (text.isBlank()) return listOf("")
    val lines = mutableListOf<String>()
    text.split("\n").forEach { paragraph ->
        var current = ""
        paragraph.trim().split(Regex("\\s+")).forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (candidate.length <= maxChars) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
    }
    return lines.ifEmpty { listOf("") }
}

private fun renderActaTemplate(context: Context, data: ActaInspeccionData): JSONObject {
    val template = context.assets.open("acta_inspeccion.json").bufferedReader().use { JSONObject(it.readText()) }
    val date = runCatching { LocalDate.parse(data.fechaInspeccion, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
        .getOrDefault(LocalDate.now())
    val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    val values = mapOf(
        "fecha" to data.fechaInspeccion,
        "hora" to data.horaInspeccion,
        "lugar" to data.lugar,
        "matricula" to data.matricula,
        "numero_autorizacion" to data.numeroAutorizacion,
        "empresa" to data.empresaTitular,
        "tip" to data.tip,
        "unidad" to data.unidad,
        "dia" to date.dayOfMonth.toString().padStart(2, '0'),
        "mes" to months[date.monthValue - 1],
        "anio" to date.year.toString()
    )

    fun resolve(value: String): String = values.entries.fold(value) { result, (key, replacement) ->
        result.replace("[[$key]]", replacement)
    }

    val body = template.getJSONObject("documento").getJSONObject("cuerpo")
    body.put("titulo", resolve(body.getString("titulo")))
    body.put("cierre", resolve(body.getString("cierre")))
    body.put("tip", resolve(body.getString("tip")))
    body.put("unidad", resolve(body.getString("unidad")))
    val paragraphs = body.getJSONArray("parrafos")
    for (index in 0 until paragraphs.length()) {
        paragraphs.put(index, resolve(paragraphs.getString(index)))
    }
    val header = template.getJSONObject("documento").getJSONObject("encabezado")
    header.put("titulo", resolve(header.getString("titulo")))
    header.put("subtitulo", resolve(header.getString("subtitulo")))
    return template
}

private fun buildActaInspeccionCpcl(context: Context, data: ActaInspeccionData): RenderedActa {
    val template = renderActaTemplate(context, data).getJSONObject("documento")
    val header = template.getJSONObject("encabezado")
    val body = template.getJSONObject("cuerpo")
    val lines = mutableListOf<String>()
    var y = MARGIN

    fun addText(text: String, font: Int = BODY_FONT, size: Int = BODY_SIZE, lineH: Int = BODY_LINE_H, x: Int = MARGIN) {
        lines.add(String.format(CMD_TEXT, font, size, x, y, text))
        y += lineH
    }

    fun addCentered(text: String, font: Int, size: Int, lineH: Int, maxChars: Int) {
        val charWidth = if (font == 7) 12 else 8
        wrapActaText(text, maxChars).forEach { line ->
            val x = ((PAPER_W - line.length * charWidth) / 2).coerceAtLeast(MARGIN)
            addText(line, font, size, lineH, x)
        }
    }

    fun addSeparator() {
        lines.add(String.format(CMD_LINE, MARGIN, y, PAPER_W - MARGIN, y, 2))
        y += BODY_LINE_H
    }

    val logoIzquierdo = loadEscEspana(context)
    val logoDerecho = loadEscTrafico(context)
    val logoTopY = MARGIN
    lines += "EG ${logoIzquierdo.bytesPerRow} ${logoIzquierdo.height} $MARGIN $logoTopY ${logoIzquierdo.hexData}\r\n"
    lines += "EG ${logoDerecho.bytesPerRow} ${logoDerecho.height} ${PAPER_W - MARGIN - logoDerecho.width} $logoTopY ${logoDerecho.hexData}\r\n"
    y = MARGIN + 20

    addCentered(header.getString("titulo"), TITLE_FONT, TITLE_SIZE, TITLE_LINE_H, 32)
    y += 8
    addCentered(header.getString("subtitulo"), SUBTITLE_FONT, SUBTITLE_SIZE, SUBTITLE_LINE_H, 62)
    y += 18
    addSeparator()

    addCentered(body.getString("titulo"), TITLE_FONT, TITLE_SIZE, TITLE_LINE_H, 44)
    y += 14
    addSeparator()
    val paragraphArray = body.getJSONArray("parrafos")
    for (index in 0 until paragraphArray.length()) {
        wrapActaText(paragraphArray.getString(index), 62).forEach { addText(it) }
        y += PARAGRAPH_GAP
    }
    wrapActaText(body.getString("cierre"), 62).forEach { addText(it) }
    y += PARAGRAPH_GAP
    addText(body.getString("tip"))
    addText(body.getString("unidad"))
    y += BODY_LINE_H * 2
    lines.add(String.format(CMD_LINE, MARGIN, y, PAPER_W - MARGIN, y, 1))

    val contentH = y + MARGIN_BOTTOM
    lines += CMD_FORM
    return RenderedActa(contentH, lines.joinToString(""))
}

/**
 * Imprime el acta de inspección en impresora Zebra.
 *
 * @param context Contexto de la aplicación
 * @param mac Dirección MAC de la impresora
 * @param data Datos del acta a imprimir
 */
fun printActaInspeccion(
    context: Context,
    mac: String,
    data: ActaInspeccionData,
    onStatus: (PrintStatus) -> Unit = {}
) {
    if (mac.isBlank()) {
        showNoMac(context)
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            withContext(Dispatchers.Main) { onStatus(PrintStatus.Connecting) }
            val rendered = buildActaInspeccionCpcl(context, data)
            withContext(Dispatchers.Main) { onStatus(PrintStatus.Sending) }
            sendToPrinter(context, mac, rendered.height, rendered.body, PAPER_W)
            CoroutineScope(Dispatchers.Main).launch {
                onStatus(PrintStatus.Completed)
                Toast.makeText(context, "Acta enviada a impresora", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onStatus(PrintStatus.Failed(e.message ?: "Error de comunicación")) }
            showError(context, e)
        }
    }
}
