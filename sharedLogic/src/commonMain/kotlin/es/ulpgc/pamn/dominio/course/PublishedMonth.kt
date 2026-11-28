package es.ulpgc.pamn.dominio.course

// El año y el mes en que se publicó una versión («oct 2026» en la tarjeta). `month` va de 1 a 12.
//
// Es el mes de la hora UTC, sin ajustar a la zona del móvil (haría falta `kotlinx-datetime`).
// Los nombres de los meses («ene», «feb»…) son de la pantalla, no de aquí.
//
// Un mes fuera de 1..12 no se puede construir (ni con `copy`): los textos de la pantalla buscan el
// mes en una lista de 12 y se saldrían de ella. `parse` devuelve null en esos casos antes de llegar aquí.
data class PublishedMonth(val year: Int, val month: Int) {
    init {
        require(month in 1..12) { "month must be between 1 and 12, was $month" }
    }

    companion object {
        // «AAAA-MM» al principio, y da igual lo que venga detrás (día, hora, zona).
        // Devuelve null si no empieza así o si el mes no está entre 1 y 12.
        fun parse(publishedAt: String): PublishedMonth? {
            val head = publishedAt.take(LENGTH)
            if (head.length < LENGTH || head[SEPARATOR_AT] != '-') return null

            val year = head.substring(0, SEPARATOR_AT).toDigitsOrNull() ?: return null
            val month = head.substring(SEPARATOR_AT + 1).toDigitsOrNull() ?: return null
            return if (month in 1..12) PublishedMonth(year, month) else null
        }

        private const val SEPARATOR_AT = 4
        private const val LENGTH = 7   // «AAAA-MM»

        // Solo dígitos del 0 al 9 (`isDigit()` aceptaría también los de otros alfabetos).
        private fun String.toDigitsOrNull(): Int? =
            if (all { it in '0'..'9' }) toInt() else null
    }
}

val CourseSummary.publishedMonth: PublishedMonth? get() = PublishedMonth.parse(publishedAt)
