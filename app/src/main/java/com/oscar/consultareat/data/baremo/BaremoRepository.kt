package com.oscar.consultareat.data.baremo

class BaremoRepository(private val database: BaremoDatabase) {
    fun findIndices(): List<BaremoIndice> = database.readableDatabase.query(
        "indice",
        arrayOf("id", "acronimo", "definicion"),
        null,
        null,
        null,
        null,
        "acronimo ASC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    BaremoIndice(
                        id = cursor.getInt(0),
                        acronimo = cursor.getString(1),
                        definicion = cursor.getString(2)
                    )
                )
            }
        }
    }

    fun findInfracciones(
        indiceId: Int?,
        gravedad: String?,
        search: String?
    ): List<BaremoInfraccion> {
        val args = mutableListOf<String>()
        val where = buildString {
            append("1 = 1")
            if (indiceId != null) {
                append(" AND ind.id = ?")
                args += indiceId.toString()
            }
            if (!gravedad.isNullOrBlank() && gravedad != TODAS) {
                append(" AND t.concepto = ?")
                args += gravedad
            }
        }

        val query = """
            SELECT inf.codigo, inf.concepto, ind.acronimo, ind.definicion,
                   s.acronimo, s.concepto, t.concepto, inf.normas_infringidas,
                   inf.norma_sancionadora, inf.precepto_sancionador,
                   inf.cuantia, inf.observaciones
            FROM infraccion inf
            JOIN subindice s ON inf.id_subindice = s.id
            JOIN indice ind ON s.id_indice = ind.id
            JOIN tipo_infraccion t ON inf.id_tipo_infraccion = t.id
            WHERE $where
            ORDER BY ind.acronimo, s.acronimo, inf.codigo
        """.trimIndent()

        val result = database.readableDatabase.rawQuery(query, args.toTypedArray()).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        BaremoInfraccion(
                            codigo = cursor.getString(0),
                            concepto = cursor.getString(1),
                            indiceAcronimo = cursor.getString(2),
                            indiceDefinicion = cursor.getString(3),
                            subindiceAcronimo = cursor.getString(4),
                            subindiceConcepto = cursor.getString(5),
                            gravedad = cursor.getString(6),
                            normasInfringidas = cursor.getStringOrNull(7),
                            normaSancionadora = cursor.getStringOrNull(8),
                            preceptoSancionador = cursor.getStringOrNull(9),
                            cuantia = cursor.getIntOrNull(10),
                            observaciones = cursor.getStringOrNull(11)
                        )
                    )
                }
            }
        }

        // SQLite resuelve los filtros estructurales; la búsqueda textual se normaliza en
        // Kotlin para que conceptos, normas y códigos funcionen también sin acentos.
        return result.filter { matchesBaremoSearch(it, search) }
    }

    fun close() = database.close()

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    companion object {
        const val TODAS = "TODAS"
    }
}

internal fun matchesBaremoSearch(item: BaremoInfraccion, search: String?): Boolean {
    val normalizedSearch = normalizeBaremoText(search)
    if (normalizedSearch.isBlank()) return true
    return listOf(
        item.codigo,
        item.concepto,
        item.indiceAcronimo,
        item.indiceDefinicion,
        item.subindiceAcronimo,
        item.subindiceConcepto,
        item.gravedad,
        item.normasInfringidas,
        item.normaSancionadora,
        item.preceptoSancionador,
        item.observaciones
    ).any { normalizeBaremoText(it).contains(normalizedSearch) }
}

internal fun normalizeBaremoText(value: String?): String =
    java.text.Normalizer.normalize(value.orEmpty(), java.text.Normalizer.Form.NFD)
        .replace("\\p{Mn}".toRegex(), "")
        .lowercase()
        .trim()
