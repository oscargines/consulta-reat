package com.oscar.consultareat.data.baremo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "baremo_sancionador.db"
private const val ASSET_NAME = "BasermoSancionador.db"

class BaremoDatabase(context: Context) : SQLiteOpenHelper(
    context,
    databaseFile(context),
    null,
    1
) {
    override fun onCreate(db: SQLiteDatabase) = Unit

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    companion object {
        private fun databaseFile(context: Context): String {
            val file = context.getDatabasePath(DATABASE_NAME)
            // Siempre copiamos la base de datos del asset para asegurar que la versión
            // más reciente del baremo se use en todo momento. Esto garantiza que las
            // actualizaciones del baremo sancionador se reflejan inmediatamente.
            file.parentFile?.mkdirs()
            context.assets.open(ASSET_NAME).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            return file.absolutePath
        }
    }
}