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
            if (!file.exists()) {
                // Se distribuye como asset para que el baremo funcione sin conexión y se
                // copia al directorio SQLite privado de la app solo durante el primer acceso.
                file.parentFile?.mkdirs()
                context.assets.open(ASSET_NAME).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
            return file.absolutePath
        }
    }
}
