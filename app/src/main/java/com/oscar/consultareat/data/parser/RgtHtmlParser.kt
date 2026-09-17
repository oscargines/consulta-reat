package com.oscar.consultareat.data.parser

import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.domain.TipoConsulta
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class RgtHtmlParser {

    fun parse(html: String, tipoConsulta: TipoConsulta): ParsedResult {
        val doc = Jsoup.parse(html)

        extraerAvisoSinResultados(doc)?.let { aviso ->
            return ParsedResult.Success(
                identidadLabel = null,
                identidadValor = null,
                matricula = null,
                empresaTitular = null,
                numeroAutorizacion = null,
                autorizaciones = emptyList(),
                vehiculos = emptyList(),
                competenciaProfesional = emptyList(),
                consejeroSeguridad = emptyList(),
                capConductor = emptyList(),
                avisoSinResultados = aviso
            )
        }

        val secciones = mutableMapOf<String, MutableList<DatoItem>>()
        var identidadLabel: String? = null
        var identidadValor: String? = null
        var matricula: String? = null
        var empresaTitular: String? = null
        var numeroAutorizacion: String? = null

        doc.select("h3.ficha2_titulo").forEach { h3 ->
            val titulo = h3.text().lowercase().trim()
            val siguiente = h3.nextElementSibling()
            when {
                siguiente?.`is`("table") == true -> {
                    val items = parseTabla(siguiente)
                    categoria(titulo)?.let { clave ->
                        secciones.getOrPut(clave) { mutableListOf() }.addAll(items)
                    }
                    if (titulo.contains("vehículo") || titulo.contains("vehiculo")) {
                        matricula = items.firstOrNull { it.etiqueta.lowercase().contains("matrícula") || it.etiqueta.lowercase().contains("matricula") }?.valor
                    }
                    if (titulo.contains("autorización") || titulo.contains("autorizacion")) {
                        numeroAutorizacion = items.firstOrNull { it.etiqueta.lowercase().contains("número") || it.etiqueta.lowercase().contains("numero") }?.valor
                    }
                }
                siguiente != null -> {
                    val dl = siguiente.selectFirst("dl.definition_list_contactos")
                    if (dl != null) {
                        val pares = paresDeDl(dl)
                        if (titulo.contains("titular")) {
                            val nombre = pares.firstOrNull { it.etiqueta.equals("Nombre", true) }
                            if (nombre != null) {
                                identidadLabel = "Empresa / Titular"
                                identidadValor = nombre.valor
                                empresaTitular = nombre.valor
                            } else {
                                val nif = pares.firstOrNull { it.etiqueta.equals("NIF", true) }
                                if (nif != null) {
                                    identidadLabel = "NIF"
                                    identidadValor = nif.valor
                                }
                            }
                        } else {
                            categoria(titulo)?.let { clave ->
                                secciones.getOrPut(clave) { mutableListOf() }.addAll(pares)
                            }
                        }
                    }
                }
            }
        }

        if (matricula == null) {
            matricula = secciones["vehiculos"]?.firstOrNull { it.etiqueta.lowercase().contains("matrícula") || it.etiqueta.lowercase().contains("matricula") }?.valor
        }
        if (numeroAutorizacion == null) {
            numeroAutorizacion = secciones["autorizaciones"]?.firstOrNull { it.etiqueta.lowercase().contains("número") || it.etiqueta.lowercase().contains("numero") }?.valor
        }
        if (empresaTitular == null) {
            empresaTitular = secciones["vehiculos"]?.firstOrNull { it.etiqueta.lowercase().contains("empresa") || it.etiqueta.lowercase().contains("titular") || it.etiqueta.lowercase().contains("nombre") }?.valor
        }

        return ParsedResult.Success(
            identidadLabel = identidadLabel,
            identidadValor = identidadValor,
            matricula = matricula,
            empresaTitular = empresaTitular,
            numeroAutorizacion = numeroAutorizacion,
            autorizaciones = secciones["autorizaciones"] ?: emptyList(),
            vehiculos = secciones["vehiculos"] ?: emptyList(),
            competenciaProfesional = secciones["competencia"] ?: emptyList(),
            consejeroSeguridad = secciones["consejero"] ?: emptyList(),
            capConductor = secciones["cap"] ?: emptyList()
        )
    }

    private fun paresDeDl(dl: Element): List<DatoItem> {
        return dl.select("dt").mapNotNull { dt ->
            val dd = dt.nextElementSibling() ?: return@mapNotNull null
            DatoItem(dt.ownText().trim(), dd.text().trim())
        }
    }

    private fun parseTabla(table: Element): List<DatoItem> {
        val headers = table.select("thead th").map { it.text().trim() }
        if (headers.isNotEmpty()) {
            return table.select("tbody tr").flatMap { row ->
                val cells = row.select("td")
                headers.mapIndexedNotNull { i, h ->
                    if (i < cells.size) {
                        val v = cells[i].text().trim()
                        if (v.isNotBlank() && v != "-") DatoItem(h, v) else null
                    } else null
                }
            }
        }
        return table.select("tbody tr").mapNotNull { row ->
            val th = row.selectFirst("th") ?: return@mapNotNull null
            val td = row.selectFirst("td") ?: return@mapNotNull null
            val v = td.text().trim()
            if (v.isBlank() || v == "-") null else DatoItem(th.text().trim(), v)
        }
    }

    private fun categoria(titulo: String): String? = when {
        titulo.contains("vehículo") || titulo.contains("vehiculo") -> "vehiculos"
        titulo.contains("autorización") || titulo.contains("autorizacion") -> "autorizaciones"
        titulo.contains("competencia") -> "competencia"
        titulo.contains("consejero") -> "consejero"
        titulo.contains("cap") || titulo.contains("conductor") ||
            titulo.contains("cualificación") || titulo.contains("cualificacion") -> "cap"
        else -> null
    }

    private fun extraerAvisoSinResultados(doc: Document): String? {
        val texto = doc.body().text()
        for (r in regexSinResultados) {
            val m = r.find(texto) ?: continue
            val inicio = texto.lastIndexOf('.', m.range.first)
            val inicioReal = if (inicio < 0) 0 else inicio + 1
            val fin = texto.indexOf('.', m.range.last)
            val finReal = if (fin < 0) texto.length else fin + 1
            val frase = texto.substring(inicioReal, finReal).trim()
            if (frase.isNotBlank()) return frase
        }
        return null
    }

    private companion object {
        val regexSinResultados = listOf(
            Regex("no se han encontrado resultados", RegexOption.IGNORE_CASE),
            Regex("no tiene títulos habilitantes", RegexOption.IGNORE_CASE),
            Regex("no tiene titulos habilitantes", RegexOption.IGNORE_CASE),
            Regex("no dispone de ningún", RegexOption.IGNORE_CASE),
            Regex("no dispone de ningun", RegexOption.IGNORE_CASE),
            Regex("no se ha encontrado", RegexOption.IGNORE_CASE),
            Regex("no existen datos", RegexOption.IGNORE_CASE),
            Regex("no constan datos", RegexOption.IGNORE_CASE),
            Regex("sin datos", RegexOption.IGNORE_CASE)
        )
    }
}

sealed interface ParsedResult {
    data class Success(
        val identidadLabel: String?,
        val identidadValor: String?,
        val matricula: String?,
        val empresaTitular: String?,
        val numeroAutorizacion: String?,
        val autorizaciones: List<DatoItem> = emptyList(),
        val vehiculos: List<DatoItem> = emptyList(),
        val competenciaProfesional: List<DatoItem> = emptyList(),
        val consejeroSeguridad: List<DatoItem> = emptyList(),
        val capConductor: List<DatoItem> = emptyList(),
        val operadores: List<DatoItem> = emptyList(),
        val conjuntosDatos: List<DatoItem> = emptyList(),
        val avisoSinResultados: String? = null
    ) : ParsedResult

    data class Error(val mensaje: String) : ParsedResult
}