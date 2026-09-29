package com.oscar.consultareat.domain

data class NfcTagDebugInfo(
    val hasTag: Boolean,
    val uid: String,
    val techList: String,
    val ageMs: Long,
    val capturedAtMillis: Long
)