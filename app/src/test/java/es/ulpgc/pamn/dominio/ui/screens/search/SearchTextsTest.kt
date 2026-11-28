package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.SearchProblem
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// Desde docs/tareas/ajustes.md, paso 9b, los textos se traducen: cada función dice qué texto de
// strings.xml va y con qué datos.
class SearchTextsTest {

    private val noMatches = SearchMessage(UiText.of(R.string.search_no_matches_title), UiText.of(R.string.search_no_matches_body))

    // --- resultsCountText ---

    // Plural: cada idioma decide la forma («1 resultado», «8 resultados», «0 resultados»).
    @Test
    fun `the result count is a plural with the number`() {
        assertEquals(UiText.plural(R.plurals.search_results, 1), resultsCountText(1))
        assertEquals(UiText.plural(R.plurals.search_results, 8), resultsCountText(8))
        assertEquals(UiText.plural(R.plurals.search_results, 0), resultsCountText(0))
    }

    // --- SearchProblem.toText ---

    @Test
    fun `unreachable asks to check the connection`() {
        assertEquals(
            SearchMessage(UiText.of(R.string.search_unreachable_title), UiText.of(R.string.search_unreachable_body)),
            SearchProblem.UNREACHABLE.toText(),
        )
    }

    @Test
    fun `server failure asks to try again later`() {
        assertEquals(
            SearchMessage(UiText.of(R.string.search_server_failure_title), UiText.of(R.string.search_server_failure_body)),
            SearchProblem.SERVER_FAILURE.toText(),
        )
    }

    // --- emptyResultsText ---

    @Test
    fun `empty with text says nothing matches`() {
        assertEquals(
            noMatches,
            emptyResultsText(CourseQuery(text = "zzz")),
        )
    }

    @Test
    fun `empty with only tags says nothing matches`() {
        assertEquals(
            noMatches,
            emptyResultsText(CourseQuery(tags = listOf("grafos"))),
        )
    }

    @Test
    fun `empty with text and tags says nothing matches`() {
        assertEquals(
            noMatches,
            emptyResultsText(CourseQuery(text = "zzz", tags = listOf("grafos"))),
        )
    }

    @Test
    fun `empty without text or tags says there are no published courses`() {
        assertEquals(
            SearchMessage(UiText.of(R.string.search_catalog_empty)),
            emptyResultsText(CourseQuery.EMPTY),
        )
    }

    @Test
    fun `blank text counts as no text`() {
        assertEquals(emptyResultsText(CourseQuery.EMPTY), emptyResultsText(CourseQuery(text = "   ")))
    }

    // Sin texto debajo: no hay nada que el alumno pueda cambiar.
    @Test
    fun `an empty catalog has no body`() {
        assertEquals(null, emptyResultsText(CourseQuery.EMPTY).body)
    }

    // --- MORE_FAILED_TEXT ---

    @Test
    fun `load more failure text`() {
        assertEquals(UiText.of(R.string.search_more_failed), MORE_FAILED_TEXT)
    }
}
