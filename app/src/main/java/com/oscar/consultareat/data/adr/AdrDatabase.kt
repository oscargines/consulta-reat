package com.oscar.consultareat.data.adr

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "adr_2025.db"
private const val ASSET_NAME = "adr_2025.db"
private const val DATABASE_VERSION = 2

private fun databaseFile(context: Context): String {
    val file = context.getDatabasePath(DATABASE_NAME)
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        context.assets.open(ASSET_NAME).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return file.absolutePath
}

class AdrDatabase(context: Context) : SQLiteOpenHelper(
    context,
    databaseFile(context),
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) = Unit

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add nombre_normalized columns if missing
            val columnsA = db.rawQuery("PRAGMA table_info(adr_tabla_a)", null).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(1)) }
            }
            val columnsB = db.rawQuery("PRAGMA table_info(adr_tabla_b)", null).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(1)) }
            }

            if (!columnsA.contains("nombre_normalized")) {
                db.execSQL("ALTER TABLE adr_tabla_a ADD COLUMN nombre_normalized TEXT NOT NULL DEFAULT ''")
            }
            if (!columnsB.contains("nombre_normalized")) {
                db.execSQL("ALTER TABLE adr_tabla_b ADD COLUMN nombre_normalized TEXT NOT NULL DEFAULT ''")
            }

            // Backfill normalized names in steps to avoid SQLite expression depth limit
            // Step 1: lowercase
            db.execSQL("UPDATE adr_tabla_a SET nombre_normalized = lower(nombre)")
            db.execSQL("UPDATE adr_tabla_b SET nombre_normalized = lower(nombre)")

            // Step 2: replace accented chars one by one
            val replacements = arrayOf(
                "Á" to "a", "É" to "e", "Í" to "i", "Ó" to "o", "Ú" to "u",
                "á" to "a", "é" to "e", "í" to "i", "ó" to "o", "ú" to "u",
                "Ñ" to "n", "ñ" to "n", "Ü" to "u", "ü" to "u"
            )
            for ((from, to) in replacements) {
                db.execSQL("UPDATE adr_tabla_a SET nombre_normalized = replace(nombre_normalized, '$from', '$to')")
                db.execSQL("UPDATE adr_tabla_b SET nombre_normalized = replace(nombre_normalized, '$from', '$to')")
            }

            // Create indexes if missing
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tabla_a_nombre_normalized ON adr_tabla_a(nombre_normalized)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tabla_b_nombre_normalized ON adr_tabla_b(nombre_normalized)")
        }
    }
}