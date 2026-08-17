package com.oscar.consultareat.domain

data class ConsultaRequest(
    val tipoConsulta: TipoConsulta,
    val tipoIdentificacion: TipoIdentificacion,
    val valor: String
) {
    class Builder {
        private var tipoConsulta: TipoConsulta? = null
        private var tipoIdentificacion: TipoIdentificacion? = null
        private var valor: String = ""

        fun tipoConsulta(tipo: TipoConsulta): Builder {
            this.tipoConsulta = tipo
            return this
        }

        fun tipoIdentificacion(tipo: TipoIdentificacion): Builder {
            this.tipoIdentificacion = tipo
            return this
        }

        fun valor(valor: String): Builder {
            this.valor = valor.trim()
            return this
        }

        fun build(): ConsultaRequest {
            val tc = tipoConsulta ?: throw IllegalArgumentException("Falta tipo de consulta")
            val ti = tipoIdentificacion ?: throw IllegalArgumentException("Falta tipo de identificación")

            require(valor.isNotBlank()) { "El valor a consultar no puede estar vacío" }
            require(esCombinacionValida(tc, ti)) {
                "Combinación no soportada por el registro: $tc + $ti"
            }
            require(validarFormato(ti, valor)) {
                "Formato inválido para ${ti.name}: $valor"
            }

            return ConsultaRequest(tc, ti, valor)
        }

        private fun esCombinacionValida(c: TipoConsulta, i: TipoIdentificacion): Boolean {
            return when (c) {
                TipoConsulta.AUTORIZACIONES ->
                    i in listOf(TipoIdentificacion.NIF, TipoIdentificacion.NOMBRE,
                        TipoIdentificacion.AUTORIZACION, TipoIdentificacion.LICENCIA_INTERNACIONAL)
                TipoConsulta.COMPETENCIA_CONSEJERO_CAP ->
                    i in listOf(TipoIdentificacion.NIF, TipoIdentificacion.NOMBRE)
                TipoConsulta.VEHICULO ->
                    i == TipoIdentificacion.MATRICULA
            }
        }

        private fun validarFormato(tipo: TipoIdentificacion, valor: String): Boolean {
            return when (tipo) {
                TipoIdentificacion.NIF -> valor.let { it.length in 8..9 }
                TipoIdentificacion.MATRICULA -> valor.let {
                    val normalized = it.replace("-", "").replace(" ", "").uppercase()
                    val regex = Regex("^[0-9]{4}[A-Z]{3}$|^[0-9]{7}[A-Z]{2}$")
                    regex.matches(normalized) || it.length in 7..9
                }
                else -> valor.isNotBlank()
            }
        }
    }
}
