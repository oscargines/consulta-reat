package com.oscar.consultareat.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.oscar.consultareat.domain.SavedBluetoothPrinter

/**
 * Gestor de persistencia de impresora Bluetooth predeterminada.
 *
 * Responsabilidades:
 * 1. **Guardar impresora predeterminada**: Persiste la MAC y nombre de la impresora seleccionada
 * 2. **Obtener impresora predeterminada**: Recupera la impresora guardada
 * 3. **Limpiar impresora predeterminada**: Elimina la referencia guardada
 *
 * **Almacenamiento**: SharedPreferences (clave: "bluetooth_printer_default")
 *
 * @property context Contexto de aplicación para acceso a SharedPreferences
 */
class BluetoothPrinterStorage(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "bluetooth_printer_storage"
        private const val KEY_DEFAULT_PRINTER_MAC = "default_printer_mac"
        private const val KEY_DEFAULT_PRINTER_NAME = "default_printer_name"
    }

    /**
     * Guarda una impresora como predeterminada.
     *
     * @param nombre Nombre descriptivo de la impresora (p.ej. "Impresora Inspección")
     * @param mac Dirección MAC (p.ej. "00:1A:7D:DA:71:13")
     * @return [SavedBluetoothPrinter] con datos guardados
     */
    fun saveDefaultPrinter(nombre: String, mac: String): SavedBluetoothPrinter {
        prefs.edit()
            .putString(KEY_DEFAULT_PRINTER_MAC, mac)
            .putString(KEY_DEFAULT_PRINTER_NAME, nombre)
            .apply()
        return SavedBluetoothPrinter(id = 1, nombre = nombre, mac = mac)
    }

    /**
     * Obtiene la impresora configurada como predeterminada.
     *
     * @return [SavedBluetoothPrinter] si existe, null si no hay por defecto
     */
    fun getDefaultPrinter(): SavedBluetoothPrinter? {
        val mac = prefs.getString(KEY_DEFAULT_PRINTER_MAC, null) ?: return null
        val nombre = prefs.getString(KEY_DEFAULT_PRINTER_NAME, "Impresora Zebra") ?: "Impresora Zebra"
        return SavedBluetoothPrinter(id = 1, nombre = nombre, mac = mac)
    }

    /**
     * Elimina la referencia de impresora predeterminada.
     */
    fun clearDefaultPrinter() {
        prefs.edit()
            .remove(KEY_DEFAULT_PRINTER_MAC)
            .remove(KEY_DEFAULT_PRINTER_NAME)
            .apply()
    }

    /**
     * Verifica si hay una impresora predeterminada configurada.
     */
    fun hasDefaultPrinter(): Boolean = prefs.contains(KEY_DEFAULT_PRINTER_MAC)
}