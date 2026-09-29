package com.oscar.consultareat.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.oscar.consultareat.domain.SavedBluetoothPrinter

/**
 * Gestor de persistencia de impresora Bluetooth predeterminada.
 *
 * Responsabilidades:
 * 1. **Guardar impresora predeterminada**: Persiste la MAC y nombre de la impresora seleccionada
 * 2. **Obtener impresora predeterminada**: Recupera la impresora guardada
 * 3. **Listado persistente**: Mantiene todas las impresoras aceptadas
 * 4. **Limpiar impresora predeterminada**: Elimina la referencia guardada
 *
 * **Almacenamiento**: SQLite local. La preferencia usada por versiones anteriores
 * se migra automáticamente a la tabla al realizar la primera lectura.
 *
 * @property context Contexto de aplicación para acceso a la base SQLite local
 */
class BluetoothPrinterStorage(private val context: Context) {

    private val database = PrinterDatabase(context)

    companion object {
        private const val LEGACY_PREFS_NAME = "bluetooth_printer_storage"
        private const val LEGACY_MAC = "default_printer_mac"
        private const val LEGACY_NAME = "default_printer_name"
    }

    /**
     * Guarda una impresora como predeterminada.
     *
     * @param nombre Nombre descriptivo de la impresora (p.ej. "Impresora Inspección")
     * @param mac Dirección MAC (p.ej. "00:1A:7D:DA:71:13")
     * @return [SavedBluetoothPrinter] con datos guardados
     */
    fun saveDefaultPrinter(nombre: String, mac: String, modelo: String? = null): SavedBluetoothPrinter {
        database.writableDatabase.use { db ->
            db.execSQL(
                """
                INSERT INTO printers(nombre, mac, modelo, es_predeterminada)
                VALUES(?, ?, ?, 1)
                ON CONFLICT(mac) DO UPDATE SET
                    nombre = excluded.nombre,
                    modelo = COALESCE(excluded.modelo, printers.modelo),
                    es_predeterminada = 1
                """.trimIndent(),
                arrayOf(nombre, mac, modelo)
            )
            db.execSQL("UPDATE printers SET es_predeterminada = 0 WHERE mac <> ?", arrayOf(mac))
        }
        return loadSavedPrinters().firstOrNull { it.mac == mac }
            ?: SavedBluetoothPrinter(1, nombre, mac, modelo, true)
    }

    /** Guarda una impresora en el listado sin cambiar la predeterminada. */
    fun savePrinter(nombre: String, mac: String, modelo: String? = null): SavedBluetoothPrinter {
        database.writableDatabase.use { db ->
            db.execSQL(
                """
                INSERT INTO printers(nombre, mac, modelo, es_predeterminada)
                VALUES(?, ?, ?, 0)
                ON CONFLICT(mac) DO UPDATE SET
                    nombre = excluded.nombre,
                    modelo = COALESCE(excluded.modelo, printers.modelo)
                """.trimIndent(),
                arrayOf(nombre, mac, modelo)
            )
        }
        return loadSavedPrinters().first { it.mac == mac }
    }

    /** Carga todas las impresoras guardadas, con la predeterminada primero. */
    fun loadSavedPrinters(): List<SavedBluetoothPrinter> {
        migrateLegacyPreferenceIfNeeded()
        database.readableDatabase.use { db ->
            db.query(
                "printers",
                arrayOf("id", "nombre", "mac", "modelo", "es_predeterminada"),
                null,
                null,
                null,
                null,
                "es_predeterminada DESC, nombre COLLATE NOCASE ASC"
            ).use { cursor ->
                val result = mutableListOf<SavedBluetoothPrinter>()
                while (cursor.moveToNext()) {
                    result += SavedBluetoothPrinter(
                        id = cursor.getInt(0),
                        nombre = cursor.getString(1),
                        mac = cursor.getString(2),
                        modelo = cursor.getString(3),
                        esPredeterminada = cursor.getInt(4) != 0
                    )
                }
                return result
            }
        }
    }

    /**
     * Obtiene la impresora configurada como predeterminada.
     *
     * @return [SavedBluetoothPrinter] si existe, null si no hay por defecto
     */
    fun getDefaultPrinter(): SavedBluetoothPrinter? {
        migrateLegacyPreferenceIfNeeded()
        return loadSavedPrinters().firstOrNull { it.esPredeterminada }
    }

    /**
     * Elimina la referencia de impresora predeterminada.
     */
    fun clearDefaultPrinter() {
        database.writableDatabase.use { db ->
            db.execSQL("UPDATE printers SET es_predeterminada = 0")
        }
    }

    /**
     * Verifica si hay una impresora predeterminada configurada.
     */
    fun hasDefaultPrinter(): Boolean = getDefaultPrinter() != null

    fun setDefaultPrinter(mac: String) {
        database.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                db.execSQL("UPDATE printers SET es_predeterminada = 0")
                db.execSQL("UPDATE printers SET es_predeterminada = 1 WHERE mac = ?", arrayOf(mac))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun migrateLegacyPreferenceIfNeeded() {
        val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val mac = prefs.getString(LEGACY_MAC, null) ?: return
        val existing = database.readableDatabase.use { db ->
            db.query("printers", arrayOf("id"), "mac = ?", arrayOf(mac), null, null, null).use { it.moveToFirst() }
        }
        if (!existing) {
            val nombre = prefs.getString(LEGACY_NAME, "Impresora Zebra") ?: "Impresora Zebra"
            saveDefaultPrinter(nombre, mac)
        } else {
            setDefaultPrinter(mac)
        }
        prefs.edit().clear().apply()
    }
}

private class PrinterDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "bluetooth_printers.db",
    null,
    1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE printers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                mac TEXT NOT NULL UNIQUE,
                modelo TEXT,
                es_predeterminada INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
