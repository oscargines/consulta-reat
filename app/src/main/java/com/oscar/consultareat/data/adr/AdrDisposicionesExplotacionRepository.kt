package com.oscar.consultareat.data.adr

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AdrDisposicionExplotacion(
    val codigo: String,
    val contenido: String,
    val paginaInicio: Int,
    val paginaFin: Int
)

// Errata de la Tabla A del BOE (ADR 2025): el ONU 3375 indica «S29» en la columna
// de explotación (8.5), pero el capítulo 8.5 solo contiene S1–S24 y la Tabla A
// oficial del ADR 2025 asigna «S9 S23» a ese ONU. Se corrige al consultar.
val CORRECCIONES_TABLA_A = mapOf("S29" to "S9")

private const val DATABASE_NAME = "adr_explotacion_2025.db"
private const val ASSET_NAME = "adr_explotacion_2025.db"

private fun exploitationDatabaseFile(context: Context): String {
    val file = context.getDatabasePath(DATABASE_NAME)
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        context.assets.open(ASSET_NAME).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return file.absolutePath
}

private class AdrDisposicionesExplotacionDatabase(context: Context) : SQLiteOpenHelper(
    context,
    exploitationDatabaseFile(context),
    null,
    1
) {
    override fun onCreate(db: SQLiteDatabase) = Unit

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

class AdrDisposicionesExplotacionRepository(context: Context) {
    private val database = AdrDisposicionesExplotacionDatabase(context)

    fun buscar(codigo: String): AdrDisposicionExplotacion? {
        val normalizedCode = codigo.trim().uppercase()
        val cursor = database.readableDatabase.query(
            "adr_disposiciones_explotacion",
            arrayOf("codigo", "contenido", "pagina_inicio", "pagina_fin"),
            "codigo = ?",
            arrayOf(normalizedCode),
            null,
            null,
            null
        )
        return cursor.use {
            if (!it.moveToFirst()) return@use null
            AdrDisposicionExplotacion(
                codigo = it.getString(0),
                contenido = it.getString(1),
                paginaInicio = it.getInt(2),
                paginaFin = it.getInt(3)
            )
        }
    }
}
