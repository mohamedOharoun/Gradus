package es.ulpgc.pamn.dominio.course

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Tarea 4 de buscar-cursos.md: el año y el mes de `publishedAt` («oct 2026» en la tarjeta).
class PublishedMonthTest {

    @Test
    fun `it reads the year and the month of a full timestamp`() {
        assertEquals(PublishedMonth(2026, 10), PublishedMonth.parse("2026-10-03T08:35:00Z"))
    }

    @Test
    fun `it reads a date without time`() {
        assertEquals(PublishedMonth(2026, 1), PublishedMonth.parse("2026-01-31"))
    }

    @Test
    fun `it keeps the UTC month without adjusting the time zone`() {
        assertEquals(PublishedMonth(2026, 10), PublishedMonth.parse("2026-10-31T23:59:59Z"))
    }

    @Test
    fun `month 0 is not a date`() {
        assertNull(PublishedMonth.parse("2026-00-10T08:35:00Z"))
    }

    @Test
    fun `month 13 is not a date`() {
        assertNull(PublishedMonth.parse("2026-13-10T08:35:00Z"))
    }

    @Test
    fun `an empty text is not a date`() {
        assertNull(PublishedMonth.parse(""))
    }

    @Test
    fun `a text that is not a date is not a date`() {
        assertNull(PublishedMonth.parse("pronto"))
        assertNull(PublishedMonth.parse("2026"))
        assertNull(PublishedMonth.parse("2026/10/03"))
        assertNull(PublishedMonth.parse("20a6-10-03"))
    }

    @Test
    fun `a course exposes the month it was published`() {
        val course = CourseSummary(
            id = "c1",
            version = 1,
            author = Author(id = "u1", name = "Ana Ruiz"),
            publishedAt = "2026-10-03T08:35:00Z",
            title = "Estructuras de datos",
            description = "",
            language = "es",
            tags = emptyList(),
            moduleCount = 20,
        )

        assertEquals(PublishedMonth(2026, 10), course.publishedMonth)
        assertNull(course.copy(publishedAt = "").publishedMonth)
    }
}
