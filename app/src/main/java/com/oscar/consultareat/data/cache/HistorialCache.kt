package com.oscar.consultareat.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oscar.consultareat.domain.ConsultaResultado
import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.TipoIdentificacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val DATASTORE_NAME = "historial_nosql"
private const val TIEMPO_VIDA_MS = 30L * 24 * 60 * 60 * 1000
private const val MAX_ITEMS = 20

private val Context.historialDataStore by preferencesDataStore(name = DATASTORE_NAME)

class HistorialCache(private val context: Context) {

    private object Keys {
        val ITEMS = stringPreferencesKey("documentos")
    }

    val items: Flow<List<HistorialItem>> = context.historialDataStore.data
        .map { prefs -> leerItems(prefs[Keys.ITEMS]) }

    suspend fun addItem(item: HistorialItem) {
        context.historialDataStore.edit { prefs ->
            val items = leerItems(prefs[Keys.ITEMS])
            val updated = (listOf(item) + items).take(MAX_ITEMS)
            prefs[Keys.ITEMS] = JSONArray(updated.map { itemToJson(it) }).toString()
        }
    }

    suspend fun purgarExpirados(): Int {
        val limite = System.currentTimeMillis() - TIEMPO_VIDA_MS
        val actual = context.historialDataStore.data.first()
        val items = leerItems(actual[Keys.ITEMS])
        val vigentes = items.filter { it.obtenidoAt >= limite }
        val eliminados = items.size - vigentes.size
        if (eliminados > 0) {
            context.historialDataStore.edit { prefs ->
                prefs[Keys.ITEMS] = JSONArray(vigentes.map { itemToJson(it) }).toString()
            }
        }
        return eliminados
    }

    suspend fun deleteItem(id: String) {
        context.historialDataStore.edit { prefs ->
            val items = leerItems(prefs[Keys.ITEMS])
            val updated = items.filterNot { it.id == id }
            prefs[Keys.ITEMS] = JSONArray(updated.map { itemToJson(it) }).toString()
        }
    }

    suspend fun clearAll() {
        context.historialDataStore.edit { it.clear() }
    }

    private fun leerItems(raw: String?): List<HistorialItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { jsonToItem(it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun itemToJson(item: HistorialItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("tipoConsulta", item.tipoConsulta.name)
        put("tipoIdentificacion", item.tipoIdentificacion.name)
        put("valor", item.valor)
        put("obtenidoAt", item.obtenidoAt)
        put("resultado", resultadoToJson(item.resultado))
    }

    private fun jsonToItem(json: JSONObject): HistorialItem? {
        val resultado = json.optJSONObject("resultado") ?: return null
        val id = json.optString("id").ifBlank { return null }
        val tipoConsulta = runCatching { TipoConsulta.valueOf(json.optString("tipoConsulta")) }.getOrNull()
            ?: return null
        val tipoIdentificacion = runCatching { TipoIdentificacion.valueOf(json.optString("tipoIdentificacion")) }
            .getOrNull() ?: return null
        return HistorialItem(
            id = id,
            tipoConsulta = tipoConsulta,
            tipoIdentificacion = tipoIdentificacion,
            valor = json.optString("valor"),
            obtenidoAt = json.optLong("obtenidoAt"),
            resultado = jsonToResultado(resultado)
        )
    }

    private fun resultadoToJson(r: ConsultaResultado): JSONObject = JSONObject().apply {
        r.identidadLabel?.let { put("identidadLabel", it) }
        r.identidadValor?.let { put("identidadValor", it) }
        put("autorizaciones", datosToJson(r.autorizaciones))
        put("vehiculos", datosToJson(r.vehiculos))
        put("competenciaProfesional", datosToJson(r.competenciaProfesional))
        put("consejeroSeguridad", datosToJson(r.consejeroSeguridad))
        put("capConductor", datosToJson(r.capConductor))
        put("operadores", datosToJson(r.operadores))
        put("conjuntosDatos", datosToJson(r.conjuntosDatos))
        r.error?.let { put("error", it) }
    }

    private fun jsonToResultado(json: JSONObject): ConsultaResultado = ConsultaResultado(
        identidadLabel = json.optString("identidadLabel").takeIf { it.isNotEmpty() },
        identidadValor = json.optString("identidadValor").takeIf { it.isNotEmpty() },
        autorizaciones = jsonToDatos(json.optJSONArray("autorizaciones")),
        vehiculos = jsonToDatos(json.optJSONArray("vehiculos")),
        competenciaProfesional = jsonToDatos(json.optJSONArray("competenciaProfesional")),
        consejeroSeguridad = jsonToDatos(json.optJSONArray("consejeroSeguridad")),
        capConductor = jsonToDatos(json.optJSONArray("capConductor")),
        operadores = jsonToDatos(json.optJSONArray("operadores")),
        conjuntosDatos = jsonToDatos(json.optJSONArray("conjuntosDatos")),
        error = json.optString("error").takeIf { it.isNotEmpty() }
    )

    private fun datosToJson(items: List<DatoItem>): JSONArray = JSONArray(
        items.map { d -> JSONObject().put("etiqueta", d.etiqueta).put("valor", d.valor) }
    )

    private fun jsonToDatos(arr: JSONArray?): List<DatoItem> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            DatoItem(o.optString("etiqueta"), o.optString("valor"))
        }
    }
}

data class HistorialItem(
    val id: String,
    val tipoConsulta: TipoConsulta,
    val tipoIdentificacion: TipoIdentificacion,
    val valor: String,
    val obtenidoAt: Long,
    val resultado: ConsultaResultado
)