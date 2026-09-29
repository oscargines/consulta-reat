package com.oscar.consultareat.domain

data class SavedBluetoothPrinter(
    val id: Int,
    val nombre: String,
    val mac: String,
    val modelo: String? = null,
    val esPredeterminada: Boolean = false
)
