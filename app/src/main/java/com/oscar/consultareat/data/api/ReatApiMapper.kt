package com.oscar.consultareat.data.api

import com.oscar.consultareat.data.parser.ParsedResult
import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.domain.TipoConsulta
import org.json.JSONArray

class ReatApiMapper {

    fun parse(json: String, tipoConsulta: TipoConsulta): ParsedResult {
        return try {
            val root = JSONArray(json)
            val operadores = mutableListOf<DatoItem>()
            val conjuntosDatos = mutableListOf<DatoItem>()

            for (i in 0 until root.length()) {
                val operador = root.optJSONObject(i) ?: continue
                val nombre = operador.optString("nombre").ifBlank { "Operador sin nombre" }
                val url = operador.optString("url")
                operadores.add(DatoItem(nombre, url.ifBlank { "Sin enlace" }))

                val datasets = operador.optJSONArray("conjuntosDatos")
                if (datasets != null) {
                    for (j in 0 until datasets.length()) {
                        val ds = datasets.optJSONObject(j) ?: continue
                        val nombreDs = ds.optString("nombre").ifBlank { "Conjunto de datos" }
                        val descripcion = ds.optString("descripcion").ifBlank { nombreDs }
                        conjuntosDatos.add(DatoItem(nombreDs, descripcion))
                    }
                }
            }

            if (operadores.isEmpty()) {
                return ParsedResult.Error("No se han encontrado operadores para esta consulta en el NAP")
            }

            ParsedResult.Success(
                identidadLabel = "Operadores encontrados",
                identidadValor = operadores.size.toString(),
                operadores = operadores,
                conjuntosDatos = conjuntosDatos
            )
        } catch (e: Exception) {
            ParsedResult.Error("Error al interpretar la respuesta del NAP: ${e.message}")
        }
    }
}