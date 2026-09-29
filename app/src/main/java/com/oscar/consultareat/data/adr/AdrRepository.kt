package com.oscar.consultareat.data.adr

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.Locale

class AdrRepository(private val database: AdrDatabase) {

    fun findByNumeroOnu(onu: String): List<AdrTablaBItem> {
        val db = database.readableDatabase
        val cursor = db.query(
            "adr_tabla_b",
            arrayOf("id", "nombre", "numero_onu", "clase", "nota"),
            "numero_onu = ?",
            arrayOf(onu),
            null, null, "nombre ASC"
        )
        return cursor.use { parseTablaB(it) }
    }

    fun findByNombreLike(query: String): List<AdrTablaBItem> {
        val normalizedQuery = normalizeText(query)
        if (normalizedQuery.isBlank()) return emptyList()
        
        val db = database.readableDatabase
        val cursor = db.rawQuery("""
            SELECT id, nombre, numero_onu, clase, nota
            FROM adr_tabla_b
            WHERE nombre_normalized LIKE ?
            ORDER BY 
                CASE WHEN numero_onu = ? THEN 0 ELSE 1 END,
                nombre ASC
            LIMIT 100
        """.trimIndent(), arrayOf("%$normalizedQuery%", normalizedQuery))
        return cursor.use { parseTablaB(it) }
    }

    fun findByNombreOrOnu(query: String): List<AdrTablaBItem> {
        val normalizedQuery = normalizeText(query)
        if (normalizedQuery.isBlank()) return emptyList()
        
        val db = database.readableDatabase
        val like = "%$normalizedQuery%"
        val cursor = db.rawQuery("""
            SELECT * FROM (
                SELECT id, nombre, numero_onu, clase, nota
                FROM adr_tabla_b
                WHERE nombre_normalized LIKE ? OR numero_onu = ?
                UNION
                SELECT 0, nombre, numero_onu, clase, NULL
                FROM adr_tabla_a
                WHERE (nombre_normalized LIKE ? OR numero_onu = ?)
                  AND numero_onu NOT IN (SELECT numero_onu FROM adr_tabla_b)
            )
            ORDER BY 
                CASE WHEN numero_onu = ? THEN 0 ELSE 1 END,
                nombre ASC
            LIMIT 100
        """.trimIndent(), arrayOf(like, normalizedQuery, like, normalizedQuery, normalizedQuery))
        return cursor.use { parseTablaB(it) }
    }

    fun getTablaAByOnu(onu: String): List<AdrTablaAItem> {
        val db = database.readableDatabase
        val cursor = db.query(
            "adr_tabla_a",
            null,
            "numero_onu = ?",
            arrayOf(onu),
            null, null, "id ASC"
        )
        return cursor.use { parseTablaA(it) }
    }

    fun getAllForCalculo(): List<AdrTablaAItem> {
        val db = database.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM adr_tabla_a
            WHERE categoria_transporte != '' AND categoria_transporte IS NOT NULL
            ORDER BY numero_onu, nombre
        """.trimIndent(), null)
        return cursor.use { parseTablaA(it) }
    }

    fun searchForCalculo(query: String): List<AdrTablaAItem> {
        val normalizedQuery = normalizeText(query)
        if (normalizedQuery.isBlank()) return getAllForCalculo()
        
        val db = database.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM adr_tabla_a
            WHERE (nombre_normalized LIKE ? OR numero_onu = ?)
              AND (categoria_transporte != '' AND categoria_transporte IS NOT NULL)
            ORDER BY numero_onu, nombre
            LIMIT 100
        """.trimIndent(), arrayOf("%$normalizedQuery%", normalizedQuery))
        return cursor.use { parseTablaA(it) }
    }

    fun close() = database.close()

    private fun parseTablaB(cursor: Cursor): List<AdrTablaBItem> = buildList {
        while (cursor.moveToNext()) {
            add(AdrTablaBItem(
                id = cursor.getInt(0),
                nombre = cursor.getString(1),
                numeroOnu = cursor.getString(2),
                clase = cursor.getStringOrNull(3),
                nota = cursor.getStringOrNull(4)
            ))
        }
    }

    private fun parseTablaA(cursor: Cursor): List<AdrTablaAItem> = buildList {
        val columnCount = cursor.columnCount
        val columnNames = cursor.columnNames
        
        while (cursor.moveToNext()) {
            val values = mutableMapOf<String, String?>()
            for (i in 0 until columnCount) {
                values[columnNames[i]] = cursor.getStringOrNull(i)
            }
            add(AdrTablaAItem(
                id = values["id"]?.toIntOrNull() ?: 0,
                numeroOnu = values["numero_onu"]?.orEmpty() ?: "",
                nombre = values["nombre"]?.orEmpty() ?: "",
                clase = values["clase"],
                codigoClasificacion = values["codigo_clasificacion"],
                grupoEmbalaje = values["grupo_embalaje"],
                etiquetas = values["etiquetas"],
                disposicionesEspeciales = values["disposiciones_especiales"],
                cantidadesLimitadas = values["cantidades_limitadas"],
                cantidadesExceptuadas = values["cantidades_exceptuadas"],
                instruccionesEmbalaje = values["instrucciones_embalaje"],
                disposicionesEmbalaje = values["disposiciones_embalaje"],
                embalajeComun = values["embalaje_comun"],
                instruccionesTransporte = values["instrucciones_transporte"],
                disposicionesCisterna = values["disposiciones_cisterna"],
                codigoCisterna = values["codigo_cisterna"],
                disposicionesCisternaEsp = values["disposiciones_cisterna_esp"],
                vehiculosCisterna = values["vehiculos_cisterna"],
                categoriaTransporte = values["categoria_transporte"],
                disposicionesTransporteBultos = values["disposiciones_transporte_bultos"],
                disposicionesTransporteGranel = values["disposiciones_transporte_granel"],
                disposicionesCargaDescarga = values["disposiciones_carga_descarga"],
                disposicionesExplotacion = values["disposiciones_explotacion"],
                numeroPeligro = values["numero_peligro"]
            ))
        }
    }

    private fun normalizeText(text: String?): String =
        java.text.Normalizer.normalize(text.orEmpty(), java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trim()

    private fun Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)
}