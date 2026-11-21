package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.SearchProblem
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextsTest {

    // --- resultsCountText ---

    @Test
    fun `one result is singular`() {
        assertEquals("1 resultado", resultsCountText(1))
    }

    @Test
    fun `several results are plural`() {
        assertEquals("8 resultados", resultsCountText(8))
    }

    @Test
    fun `zero results is plural`() {
        assertEquals("0 resultados", resultsCountText(0))
    }

    // --- SearchProblem.toText ---

    @Test
    fun `unreachable asks to check the connection`() {
        assertEquals(
            SearchMessage("Sin conexión", "No se ha podido buscar. Comprueba la conexión y vuelve a intentarlo."),
            SearchProblem.UNREACHABLE.toText(),
        )
    }

    @Test
    fun `server failure asks to try again later`() {
        assertEquals(
            SearchMessage("Algo ha fallado", "El servidor no ha respondido bien. Vuelve a intentarlo en un momento."),
            SearchProblem.SERVER_FAILURE.toText(),
        )
    }

    // --- emptyResultsText ---

    @Test
    fun `empty with text says nothing matches`() {
        assertEquals(
            SearchMessage("No hay cursos que coincidan", "Prueba con otras palabras."),
            emptyResultsText(CourseQuery(text = "zzz")),
        )
    }

    @Test
    fun `empty with only tags says nothing matches`() {
        assertEquals(
            SearchMessage("No hay cursos que coincidan", "Prueba con otras palabras."),
            emptyResultsText(CourseQuery(tags = listOf("grafos"))),
        )
    }

    @Test
    fun `empty with text and tags says nothing matches`() {
        assertEquals(
            SearchMessage("No hay cursos que coincidan", "Prueba con otras palabras."),
            emptyResultsText(CourseQuery(text = "zzz", tags = listOf("grafos"))),
        )
    }

    @Test
    fun `empty without text or tags says there are no published courses`() {
        assertEquals(
            SearchMessage("Todavía no hay cursos publicados", ""),
            emptyResultsText(CourseQuery.EMPTY),
        )
    }

    @Test
    fun `blank text counts as no text`() {
        assertEquals(emptyResultsText(CourseQuery.EMPTY), emptyResultsText(CourseQuery(text = "   ")))
    }

    // --- MORE_FAILED_TEXT ---

    @Test
    fun `load more failure text`() {
        assertEquals("No se han podido cargar más cursos.", MORE_FAILED_TEXT)
    }
}
