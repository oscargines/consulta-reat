package com.oscar.consultareat.data.baremo

data class BaremoIndice(
    val id: Int,
    val acronimo: String,
    val definicion: String
) {
    val etiqueta: String
        get() = "$acronimo - $definicion"
}

data class BaremoInfraccion(
    val codigo: String,
    val concepto: String,
    val indiceAcronimo: String,
    val indiceDefinicion: String,
    val subindiceAcronimo: String,
    val subindiceConcepto: String,
    val gravedad: String,
    val normasInfringidas: String?,
    val normaSancionadora: String?,
    val preceptoSancionador: String?,
    val cuantia: Int?,
    val observaciones: String?
)
