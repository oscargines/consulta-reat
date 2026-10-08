package com.oscar.consultareat.data.adr

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AdrInstruccionEmbalaje(
    val codigo: String,
    val titulo: String,
    val contenido: String,
    val seccionAdr: String,
    val paginaInicio: Int,
    val paginaFin: Int
)

private const val DATABASE_NAME = "adr_embalajes_2025.db"
private const val ASSET_NAME = "adr_embalajes_2025.db"

private fun packagingDatabaseFile(context: Context): String {
    val file = context.getDatabasePath(DATABASE_NAME)
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        context.assets.open(ASSET_NAME).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return file.absolutePath
}

private class AdrInstruccionesEmbalajeDatabase(context: Context) : SQLiteOpenHelper(
    context,
    packagingDatabaseFile(context),
    null,
    1
) {
    override fun onCreate(db: SQLiteDatabase) = Unit

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

class AdrInstruccionesEmbalajeRepository(context: Context) {
    private val database = AdrInstruccionesEmbalajeDatabase(context)

    fun buscar(codigo: String): AdrInstruccionEmbalaje? {
        val normalizedCode = codigo
            .replace(Regex("""\(?\s*([a-z])\)""", RegexOption.IGNORE_CASE), "($1)")
            .replace(Regex("""\s+"""), "")
            .uppercase()
        val cursor = database.readableDatabase.query(
            "adr_instrucciones_embalaje",
            arrayOf("codigo", "titulo", "contenido", "seccion_adr", "pagina_inicio", "pagina_fin"),
            "codigo = ?",
            arrayOf(normalizedCode),
            null,
            null,
            null
        )
        return cursor.use {
            if (!it.moveToFirst()) return@use null
            AdrInstruccionEmbalaje(
                codigo = it.getString(0),
                titulo = it.getString(1),
                contenido = it.getString(2),
                seccionAdr = it.getString(3),
                paginaInicio = it.getInt(4),
                paginaFin = it.getInt(5)
            )
        }
    }
}
