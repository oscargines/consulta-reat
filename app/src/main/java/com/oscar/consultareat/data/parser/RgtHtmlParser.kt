package com.oscar.consultareat.data.parser

import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.domain.TipoConsulta
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class RgtHtmlParser {

    fun parse(html: String, tipoConsulta: TipoConsulta): ParsedResult {
        val doc = Jsoup.parse(html)

        if (doc.body().text().contains("No se han encontrado resultados")) {
            return ParsedResult.Error("No se han encontrado resultados para esta consulta")
        }

        val secciones = mutableMapOf<String, MutableList<DatoItem>>()
        var identidadLabel: String? = null
        var identidadValor: String? = null

        doc.select("h3.ficha2_titulo").forEach { h3 ->
            val titulo = h3.text().lowercase().trim()
            val siguiente = h3.nextElementSibling()
            when {
                siguiente?.`is`("table") == true -> {
                    val items = parseTabla(siguiente)
                    categoria(titulo)?.let { clave ->
                        secciones.getOrPut(clave) { mutableListOf() }.addAll(items)
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

        return ParsedResult.Success(
            identidadLabel = identidadLabel,
            identidadValor = identidadValor,
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
}

sealed interface ParsedResult {
    data class Success(
        val identidadLabel: String?,
        val identidadValor: String?,
        val autorizaciones: List<DatoItem> = emptyList(),
        val vehiculos: List<DatoItem> = emptyList(),
        val competenciaProfesional: List<DatoItem> = emptyList(),
        val consejeroSeguridad: List<DatoItem> = emptyList(),
        val capConductor: List<DatoItem> = emptyList(),
        val operadores: List<DatoItem> = emptyList(),
        val conjuntosDatos: List<DatoItem> = emptyList()
    ) : ParsedResult

    data class Error(val mensaje: String) : ParsedResult
}