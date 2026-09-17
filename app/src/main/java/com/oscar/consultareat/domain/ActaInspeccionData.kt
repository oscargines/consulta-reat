package com.oscar.consultareat.domain

data class ActaInspeccionData(
    val matricula: String,
    val empresaTitular: String,
    val numeroAutorizacion: String,
    val fechaInspeccion: String = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val horaInspeccion: String = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
)