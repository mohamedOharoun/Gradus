package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// Tarea 7b de buscar-cursos.md: los textos de los filtros, como funciones puras (sin Compose).
// Desde docs/tareas/ajustes.md, paso 9c, se traducen: cada función dice qué texto de strings.xml va y
// con qué datos. Las etiquetas y el nombre del creador los escribe la persona: van tal cual.
class FilterTextsTest {

    // --- Textos fijos ---

    @Test
    fun `the filters button is called Filtros`() {
        assertEquals(UiText.of(R.string.filters), FILTERS_LABEL)
    }

    @Test
    fun `with no tags to choose the sheet says so`() {
        assertEquals(UiText.of(R.string.filters_no_tags), NO_TAGS_TEXT)
    }

    // --- showCoursesText (tarea 8b: el botón de la hoja) ---

    // Plural: cada idioma decide la forma («Mostrar 1 curso», «Mostrar 8 cursos»).
    @Test
    fun `the button shows the number of courses the draft would give`() {
        assertEquals(UiText.plural(R.plurals.filters_show_count, 8), showCoursesText(DraftCount.Known(8)))
    }

    @Test
    fun `one course is the plural with one`() {
        assertEquals(UiText.plural(R.plurals.filters_show_count, 1), showCoursesText(DraftCount.Known(1)))
    }

    @Test
    fun `a big number keeps all its digits`() {
        assertEquals(UiText.plural(R.plurals.filters_show_count, 120), showCoursesText(DraftCount.Known(120)))
    }

    @Test
    fun `with no courses the button says that none match`() {
        assertEquals(NO_COURSES_MATCH_TEXT, showCoursesText(DraftCount.Known(0)))
        assertEquals(UiText.of(R.string.filters_no_courses_match), NO_COURSES_MATCH_TEXT)
    }

    @Test
    fun `while counting the button has no number`() {
        assertEquals(SHOW_COURSES_TEXT, showCoursesText(DraftCount.Counting))
        assertEquals(UiText.of(R.string.filters_show_courses), SHOW_COURSES_TEXT)
    }

    @Test
    fun `when the count failed the button has no number either`() {
        assertEquals(SHOW_COURSES_TEXT, showCoursesText(DraftCount.Unknown))
    }

    // --- filtersButtonDescription (lo que lee TalkBack) ---

    @Test
    fun `without filters the button is just Filtros`() {
        assertEquals(FILTERS_LABEL, filtersButtonDescription(0))
    }

    @Test
    fun `active filters are a plural with the number`() {
        assertEquals(UiText.plural(R.plurals.filters_active, 1), filtersButtonDescription(1))
        assertEquals(UiText.plural(R.plurals.filters_active, 2), filtersButtonDescription(2))
        assertEquals(UiText.plural(R.plurals.filters_active, 12), filtersButtonDescription(12))
    }

    @Test
    fun `a negative count is read as no filters`() {
        assertEquals(FILTERS_LABEL, filtersButtonDescription(-1))
    }

    // --- tagChipLabel («#» no se traduce) ---

    @Test
    fun `the chip label puts a hash before the tag`() {
        assertEquals("#grafos", tagChipLabel("grafos"))
    }

    @Test
    fun `the chip label keeps capitals, accents and inner spaces`() {
        assertEquals("#Bases de datos", tagChipLabel("Bases de datos"))
        assertEquals("#árboles", tagChipLabel("árboles"))
    }

    // --- removeFilterDescription (lo que lee TalkBack) ---

    @Test
    fun `removing a filter is read without the hash`() {
        assertEquals(UiText.of(R.string.filters_remove, "grafos"), removeFilterDescription("grafos"))
    }

    @Test
    fun `removing a filter keeps inner spaces`() {
        assertEquals(UiText.of(R.string.filters_remove, "bases de datos"), removeFilterDescription("bases de datos"))
    }

    // --- Creador (tarea 11a-2) ---

    @Test
    fun `the author chip starts with Creador like the sheet section`() {
        assertEquals(UiText.of(R.string.filters_author_chip, "Ana"), authorChipLabel("Ana"))
    }

    @Test
    fun `the author chip keeps capitals accents and inner spaces`() {
        assertEquals(UiText.of(R.string.filters_author_chip, "Ángel Ruiz"), authorChipLabel("Ángel Ruiz"))
    }

    @Test
    fun `removing the author filter says that it is the creator`() {
        assertEquals(UiText.of(R.string.filters_remove_author, "Ana"), removeAuthorFilterDescription("Ana"))
    }

    @Test
    fun `removing the author filter keeps the whole name`() {
        assertEquals(UiText.of(R.string.filters_remove_author, "Ángel Ruiz"), removeAuthorFilterDescription("Ángel Ruiz"))
    }

    // --- Fecha de publicación (tarea 11a-2) ---

    @Test
    fun `each publication date has its label`() {
        assertEquals(UiText.of(R.string.published_any), publishedLabel(PublishedWithin.ANY))
        assertEquals(UiText.of(R.string.published_last_month), publishedLabel(PublishedWithin.LAST_MONTH))
        assertEquals(UiText.of(R.string.published_last_year), publishedLabel(PublishedWithin.LAST_YEAR))
    }

    // Dentro de la frase la fecha va en minúsculas («Quitar filtro último año»): cada idioma tiene su forma
    // (antes se hacía con lowercase(), que no sirve para un texto todavía sin traducir).
    @Test
    fun `removing the date filter reads the date as part of the sentence`() {
        assertEquals(
            UiText.of(R.string.filters_remove, UiText.of(R.string.published_last_month_inline)),
            removePublishedFilterDescription(PublishedWithin.LAST_MONTH),
        )
        assertEquals(
            UiText.of(R.string.filters_remove, UiText.of(R.string.published_last_year_inline)),
            removePublishedFilterDescription(PublishedWithin.LAST_YEAR),
        )
    }

    // «Cualquiera» no tiene chip (no filtra), pero la función responde a las tres fechas.
    @Test
    fun `removing any date would read cualquiera`() {
        assertEquals(
            UiText.of(R.string.filters_remove, UiText.of(R.string.published_any_inline)),
            removePublishedFilterDescription(PublishedWithin.ANY),
        )
    }
}
