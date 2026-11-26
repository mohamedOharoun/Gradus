package es.ulpgc.pamn.dominio.ui.text

import es.ulpgc.pamn.dominio.R
import org.junit.Assert.assertEquals
import org.junit.Test

// Los nombres de los meses (docs/tareas/ajustes.md, paso 9a): los comparten las notas y las tarjetas de
// los cursos. Cada idioma los pone en strings.xml (month_short_*, month_long_*).
class MonthsTest {

    @Test
    fun `every month has its own short and long name`() {
        assertEquals(12, (1..12).map(::shortMonth).toSet().size)
        assertEquals(12, (1..12).map(::longMonth).toSet().size)
    }

    // El mes va de 1 (enero) a 12 (diciembre), como en java.time y en PublishedMonth.
    @Test
    fun `month one is january and month twelve is december`() {
        assertEquals(UiText.of(R.string.month_short_1), shortMonth(1))
        assertEquals(UiText.of(R.string.month_short_12), shortMonth(12))
        assertEquals(UiText.of(R.string.month_long_1), longMonth(1))
        assertEquals(UiText.of(R.string.month_long_12), longMonth(12))
    }
}
