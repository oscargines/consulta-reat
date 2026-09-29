package com.oscar.consultareat.data.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.oscar.consultareat.domain.EstadoInspeccion
import com.oscar.consultareat.domain.InspeccionConcepto
import com.oscar.consultareat.domain.InspeccionInternaData
import java.io.File
import java.io.FileOutputStream

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 28f

fun generateInspeccionInternaPdf(context: Context, data: InspeccionInternaData): File {
    val document = PdfDocument()
    val page1 = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
    drawPageHeader(page1.canvas, data)
    drawFields(page1.canvas, data)
    drawConcepts(page1.canvas, data.conceptos, 0, 15, 365f)
    document.finishPage(page1)

    val page2 = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
    drawPageHeader(page2.canvas, data, compact = true)
    drawConcepts(page2.canvas, data.conceptos, 15, data.conceptos.size, 120f)
    document.finishPage(page2)

    val dir = File(context.cacheDir, "inspecciones").apply { mkdirs() }
    val file = File(dir, "inspeccion_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}

private fun drawPageHeader(canvas: android.graphics.Canvas, data: InspeccionInternaData, compact: Boolean = false) {
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = if (compact) 13f else 15f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("DIRECCIÓN GENERAL DE LA GUARDIA CIVIL", PAGE_WIDTH / 2f, 34f, title)
    canvas.drawText("INSPECCIÓN DE TRANSPORTE ESCOLAR", PAGE_WIDTH / 2f, 52f, title)
    canvas.drawText("DESTACAMENTO DE TRÁFICO", PAGE_WIDTH / 2f, 70f, title)
    canvas.drawLine(MARGIN, 82f, PAGE_WIDTH - MARGIN, 82f, Paint().apply { strokeWidth = 2f })
}

private fun drawFields(canvas: android.graphics.Canvas, data: InspeccionInternaData) {
    var y = 105f
    val fields = listOf(
        "LUGAR DE LA INSPECCIÓN" to data.lugar,
        "FECHA Y HORA" to "${data.fecha} ${data.hora}",
        "MATRÍCULA" to data.matricula,
        "MARCA Y MODELO" to data.marcaModelo,
        "TITULAR" to data.titular,
        "NIF/CIF" to data.nifCif,
        "DOMICILIO" to data.domicilio,
        "Nº TARJETA TTES" to data.tarjetaTransportes,
        "CLASE" to data.clase,
        "Nº PLAZAS" to data.plazas,
        "CONDUCTOR" to data.conductor,
        "DNI/NIE" to data.dniNie,
        "ENTIDAD ORGANIZADORA / CONTRATANTE" to data.entidadOrganizadora,
        "CIF" to data.entidadCif,
        "DIRECCIÓN" to data.entidadDireccion,
        "NÚMERO DE AUTORIZACIÓN ESPECÍFICA" to data.numeroAutorizacion,
        "KM ORIGEN / ORIGEN" to "${data.kmOrigen} / ${data.origen}",
        "Nº ALUMNOS / DESTINO" to "${data.alumnos} / ${data.destino}",
        "CENTRO EDUCATIVO" to data.centroEducativo
    )
    fields.forEach { (label, value) ->
        if (y > 350f) return@forEach
        drawField(canvas, label, value, y)
        y += 20f
    }
}

private fun drawField(canvas: android.graphics.Canvas, label: String, value: String, y: Float) {
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 6.5f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8f
    }
    canvas.drawRect(MARGIN, y - 11f, 190f, y + 7f, Paint().apply { color = Color.LTGRAY })
    canvas.drawRect(MARGIN, y - 11f, PAGE_WIDTH - MARGIN, y + 7f, Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = .6f
    })
    canvas.drawText(label, MARGIN + 4f, y + 1f, labelPaint)
    canvas.drawText(value, 195f, y + 1f, valuePaint)
}

private fun drawConcepts(canvas: android.graphics.Canvas, concepts: List<InspeccionConcepto>, from: Int, until: Int, startY: Float) {
    var y = startY
    val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("CONCEPTO", MARGIN, y, header)
    canvas.drawText("SI", 515f, y, header)
    canvas.drawText("NO", 550f, y, header)
    y += 12f
    var previousCategory = ""
    concepts.subList(from, until).forEach { concept ->
        if (y > PAGE_HEIGHT - 35f) return@forEach
        if (concept.categoria != previousCategory) {
            previousCategory = concept.categoria
            canvas.drawText(concept.categoria, MARGIN, y, header)
            y += 11f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 6.8f; color = Color.BLACK }
        val lines = wrap(concept.texto + (concept.referencia?.let { " ($it)" } ?: ""), 76)
        lines.forEach { line ->
            canvas.drawText(line, MARGIN + 8f, y, textPaint)
            y += 9f
        }
        canvas.drawText(if (concept.estado == EstadoInspeccion.SI) "X" else "", 516f, y - 9f, header)
        canvas.drawText(if (concept.estado == EstadoInspeccion.NO) "X" else "", 551f, y - 9f, header)
        canvas.drawLine(MARGIN, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, Paint().apply { color = Color.LTGRAY })
        y += 5f
    }
}

private fun wrap(text: String, max: Int): List<String> {
    val result = mutableListOf<String>()
    var current = ""
    text.split(" ").forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (candidate.length <= max) current = candidate
        else {
            if (current.isNotEmpty()) result += current
            current = word
        }
    }
    if (current.isNotEmpty()) result += current
    return result.ifEmpty { listOf("") }
}
