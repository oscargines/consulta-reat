package com.oscar.consultareat.data.adr

data class AdrTablaAItem(
    val id: Int,
    val numeroOnu: String,
    val nombre: String,
    val clase: String?,
    val codigoClasificacion: String?,
    val grupoEmbalaje: String?,
    val etiquetas: String?,
    val disposicionesEspeciales: String?,
    val cantidadesLimitadas: String?,
    val cantidadesExceptuadas: String?,
    val instruccionesEmbalaje: String?,
    val disposicionesEmbalaje: String?,
    val embalajeComun: String?,
    val instruccionesTransporte: String?,
    val disposicionesCisterna: String?,
    val codigoCisterna: String?,
    val disposicionesCisternaEsp: String?,
    val vehiculosCisterna: String?,
    val categoriaTransporte: String?,
    val disposicionesTransporteBultos: String?,
    val disposicionesTransporteGranel: String?,
    val disposicionesCargaDescarga: String?,
    val disposicionesExplotacion: String?,
    val numeroPeligro: String?
) {
    val categoriaTransporteNumero: Int?
        get() {
            if (categoriaTransporte.isNullOrBlank()) return null
            val match = Regex("""(\d+)""").find(categoriaTransporte!!)
            return match?.groupValues?.get(1)?.toIntOrNull()
        }

    val codigoTunel: String?
        get() {
            if (categoriaTransporte.isNullOrBlank()) return null
            val match = Regex("""\(([A-Z]\d+)\)""").find(categoriaTransporte!!)
            return match?.groupValues?.get(1)
        }
}

data class AdrTablaBItem(
    val id: Int,
    val nombre: String,
    val numeroOnu: String,
    val clase: String?,
    val nota: String?
)

data class AdrConsultaResultado(
    val tablaB: AdrTablaBItem,
    val tablaAItems: List<AdrTablaAItem>
)

data class AdrMercanciaCalculo(
    val tablaAItem: AdrTablaAItem,
    val cantidad: Double,
    val unidad: String
) {
    val puntos: Double
        get() = cantidad * factorCategoria

    val factorCategoria: Double
        get() = when (tablaAItem.categoriaTransporteNumero) {
            1 -> if (tablaAItem.numeroOnu.trim().toIntOrNull() in ONUS_NOTA_A_CAT1) 20.0 else 50.0
            2 -> 3.0
            3 -> 1.0
            else -> 0.0 // Cat. 0 bloquea la exención y cat. 4 ilimitada: no computan
        }

    val esExento: Boolean
        get() = tablaAItem.categoriaTransporteNumero == 0 || tablaAItem.categoriaTransporteNumero == 4

    val nombreDisplay: String
        get() = "${tablaAItem.numeroOnu} - ${tablaAItem.nombre}"
}

// ADR 1.1.3.6.3 nota a): Nos. ONU de cat. 1 con multiplicador 20 en lugar de 50
private val ONUS_NOTA_A_CAT1 = setOf(81, 82, 84, 241, 331, 332, 482, 1005, 1017)