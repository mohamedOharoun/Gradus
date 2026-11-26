package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.PublishedMonth
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.longMonth
import es.ulpgc.pamn.dominio.ui.text.shortMonth
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

    // Los textos se traducen (docs/tareas/ajustes.md, paso 9a): cada función dice qué texto de strings.xml
    // va y con qué datos. Título, autor y etiquetas los escribe la persona: van tal cual.
    private val modules20 = UiText.plural(R.plurals.course_modules, 20)
    private val oct2026 = UiText.of(R.string.month_year_short, shortMonth(10), 2026)
    private val publishedOct2026 = UiText.of(R.string.course_published, UiText.of(R.string.month_year_long, longMonth(10), 2026))
    private val byAna = UiText.of(R.string.detail_by_author, "Ana Ruiz")

    // --- moduleCountText ---

    // Plural: cada idioma decide la forma («1 módulo», «20 módulos»).
    @Test
    fun `the module count is a plural with the number`() {
        assertEquals(UiText.plural(R.plurals.course_modules, 1), moduleCountText(1))
        assertEquals(modules20, moduleCountText(20))
        assertEquals(UiText.plural(R.plurals.course_modules, 0), moduleCountText(0))
    }

    // --- PublishedMonth.shortLabel / spokenLabel ---

    @Test
    fun `short label has the abbreviated month and the year`() {
        assertEquals(oct2026, PublishedMonth(2026, 10).shortLabel())
    }

    @Test
    fun `short label of the twelve months`() {
        (1..12).forEach {
            assertEquals(UiText.of(R.string.month_year_short, shortMonth(it), 2026), PublishedMonth(2026, it).shortLabel())
        }
    }

    // «oct 2026» se lee mal en voz alta (TalkBack): el mes entero.
    @Test
    fun `spoken label has the full month`() {
        assertEquals(UiText.of(R.string.month_year_long, longMonth(10), 2026), PublishedMonth(2026, 10).spokenLabel())
    }

    @Test
    fun `spoken label of the twelve months`() {
        (1..12).forEach {
            assertEquals(UiText.of(R.string.month_year_long, longMonth(it), 2026), PublishedMonth(2026, it).spokenLabel())
        }
    }

    // --- CourseSummary.metaText ---

    @Test
    fun `meta text joins modules and month`() {
        assertEquals(UiText.of(R.string.course_meta, modules20, oct2026), course().metaText())
    }

    @Test
    fun `meta text is singular with one module`() {
        assertEquals(UiText.of(R.string.course_meta, UiText.plural(R.plurals.course_modules, 1), oct2026), course(moduleCount = 1).metaText())
    }

    @Test
    fun `meta text has only the modules when the date is not understood`() {
        assertEquals(modules20, course(publishedAt = "pronto").metaText())
    }

    // --- CourseSummary.spokenText (lo que lee TalkBack) ---

    @Test
    fun `spoken text reads title author tags modules and full month`() {
        assertEquals(
            joinedWithCommas(listOf("Estructuras de datos", byAna, "grafos", "árboles", "algoritmos", modules20, publishedOct2026)),
            course().spokenText(),
        )
    }

    @Test
    fun `spoken text leaves out the date when it is not understood`() {
        assertEquals(
            joinedWithCommas(listOf("Estructuras de datos", byAna, "grafos", "árboles", "algoritmos", modules20)),
            course(publishedAt = "").spokenText(),
        )
    }

    @Test
    fun `spoken text skips the tags when there are none`() {
        assertEquals(
            joinedWithCommas(listOf("Estructuras de datos", byAna, modules20, publishedOct2026)),
            course(tags = emptyList()).spokenText(),
        )
    }

    @Test
    fun `spoken text reads all the tags even the ones the card hides`() {
        assertEquals(
            joinedWithCommas(listOf("Estructuras de datos", byAna, "a", "b", "c", "d", "e", modules20, publishedOct2026)),
            course(tags = listOf("a", "b", "c", "d", "e")).spokenText(),
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
