package com.oscar.consultareat.domain

data class NfcDniPersonData(
    val firstName: String,
    val lastName1: String,
    val lastName2: String,
    val documentType: String,
    val documentNumber: String,
    val optionalData: String,
    val fatherName: String,
    val motherName: String,
    val birthDateAammdd: String,
    val birthPlace: String,
    val birthProvince: String,
    val residenceAddress: String,
    val residencePopulation: String,
    val residenceProvince: String,
    val nationality: String,
    val sex: String
) {
    val fullName: String
        get() = "$firstName $lastName1 $lastName2".trim()

    val fullNameLastFirst: String
        get() = "$lastName1 $lastName2 $firstName".trim()
}