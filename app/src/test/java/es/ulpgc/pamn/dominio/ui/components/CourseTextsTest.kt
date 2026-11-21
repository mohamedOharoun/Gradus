package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.PublishedMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseTextsTest {

    private fun course(
        tags: List<String> = listOf("grafos", "árboles", "algoritmos"),
        moduleCount: Int = 20,
        publishedAt: String = "2026-10-03T08:35:00Z",
    ) = CourseSummary(
        id = "c1",
        version = 1,
        author = Author(id = "u1", name = "Ana Ruiz"),
        publishedAt = publishedAt,
        title = "Estructuras de datos",
        description = "",
        language = "es",
        tags = tags,
        moduleCount = moduleCount,
    )

    // --- moduleCountText ---

    @Test
    fun `one module is singular`() {
        assertEquals("1 módulo", moduleCountText(1))
    }

    @Test
    fun `several modules are plural`() {
        assertEquals("20 módulos", moduleCountText(20))
    }

    @Test
    fun `zero modules is plural`() {
        assertEquals("0 módulos", moduleCountText(0))
    }

    // --- PublishedMonth.shortLabel / spokenLabel ---

    @Test
    fun `short label has the abbreviated month and the year`() {
        assertEquals("oct 2026", PublishedMonth(2026, 10).shortLabel())
    }

    @Test
    fun `short label of the twelve months`() {
        val expected = listOf(
            "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
        )
        val actual = (1..12).map { PublishedMonth(2026, it).shortLabel().substringBefore(' ') }
        assertEquals(expected, actual)
    }

    @Test
    fun `spoken label has the full month`() {
        assertEquals("octubre de 2026", PublishedMonth(2026, 10).spokenLabel())
    }

    @Test
    fun `spoken label of the twelve months`() {
        val expected = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        )
        val actual = (1..12).map { PublishedMonth(2026, it).spokenLabel().substringBefore(" de ") }
        assertEquals(expected, actual)
    }

    // --- CourseSummary.metaText ---

    @Test
    fun `meta text joins modules and month`() {
        assertEquals("20 módulos · oct 2026", course().metaText())
    }

    @Test
    fun `meta text is singular with one module`() {
        assertEquals("1 módulo · oct 2026", course(moduleCount = 1).metaText())
    }

    @Test
    fun `meta text has only the modules when the date is not understood`() {
        assertEquals("20 módulos", course(publishedAt = "pronto").metaText())
    }

    // --- CourseSummary.spokenText (lo que lee TalkBack) ---

    @Test
    fun `spoken text reads title author tags modules and full month`() {
        assertEquals(
            "Estructuras de datos, por Ana Ruiz, grafos, árboles, algoritmos, " +
                "20 módulos, publicado en octubre de 2026",
            course().spokenText(),
        )
    }

    @Test
    fun `spoken text leaves out the date when it is not understood`() {
        assertEquals(
            "Estructuras de datos, por Ana Ruiz, grafos, árboles, algoritmos, 20 módulos",
            course(publishedAt = "").spokenText(),
        )
    }

    @Test
    fun `spoken text skips the tags when there are none`() {
        assertEquals(
            "Estructuras de datos, por Ana Ruiz, 20 módulos, publicado en octubre de 2026",
            course(tags = emptyList()).spokenText(),
        )
    }

    @Test
    fun `spoken text reads all the tags even the ones the card hides`() {
        val text = course(tags = listOf("a", "b", "c", "d", "e")).spokenText()
        assertEquals(
            "Estructuras de datos, por Ana Ruiz, a, b, c, d, e, 20 módulos, " +
                "publicado en octubre de 2026",
            text,
        )
    }

    // --- CourseSummary.tagsPreview ---

    @Test
    fun `preview shows fewer tags than the limit as they are`() {
        assertEquals(
            TagsPreview(visible = listOf("grafos", "árboles"), hidden = 0),
            course(tags = listOf("grafos", "árboles")).tagsPreview(),
        )
    }

    @Test
    fun `preview shows exactly the limit without hidden`() {
        assertEquals(
            TagsPreview(visible = listOf("a", "b", "c"), hidden = 0),
            course(tags = listOf("a", "b", "c")).tagsPreview(),
        )
    }

    @Test
    fun `preview keeps the first three and counts the rest`() {
        assertEquals(
            TagsPreview(visible = listOf("a", "b", "c"), hidden = 2),
            course(tags = listOf("a", "b", "c", "d", "e")).tagsPreview(),
        )
    }

    @Test
    fun `preview of a course without tags is empty`() {
        assertEquals(TagsPreview(visible = emptyList(), hidden = 0), course(tags = emptyList()).tagsPreview())
    }

    @Test
    fun `preview limit can be changed`() {
        assertEquals(
            TagsPreview(visible = listOf("a"), hidden = 2),
            course(tags = listOf("a", "b", "c")).tagsPreview(max = 1),
        )
    }
}
