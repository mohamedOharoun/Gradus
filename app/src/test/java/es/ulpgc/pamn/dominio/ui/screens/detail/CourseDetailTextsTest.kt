package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Publication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Los textos de la ficha de un curso (funciones puras, sin Compose).
class CourseDetailTextsTest {

    private val published = Publication(Author("u1", "Ana Ruiz"), version = 2, publishedAt = "2026-11-18T08:35:00Z")

    private fun course(modules: Int = 2, publication: Publication? = published) =
        Course("c", "Grafos", (1..modules).map { Module("m$it", "Módulo $it") }, publication = publication)

    // --- Autor ---

    @Test
    fun `the author line says who published it`() {
        assertEquals("por Ana Ruiz", course().authorText())
    }

    @Test
    fun `a course without publication has no author line`() {
        assertNull(course(publication = null).authorText())
    }

    // --- Módulos y fecha ---

    @Test
    fun `the meta line has the modules and the month, like the card in Search`() {
        assertEquals("2 módulos · nov 2026", course().metaText())
    }

    @Test
    fun `one module is singular`() {
        assertEquals("1 módulo · nov 2026", course(modules = 1).metaText())
    }

    @Test
    fun `without publication the meta line has only the modules`() {
        assertEquals("2 módulos", course(publication = null).metaText())
    }

    @Test
    fun `a date that cannot be read leaves only the modules`() {
        assertEquals("2 módulos", course(publication = published.copy(publishedAt = "ayer")).metaText())
    }

    // «nov 2026» se lee mal en voz alta: TalkBack oye el mes entero.
    @Test
    fun `TalkBack hears the whole month`() {
        assertEquals("2 módulos, publicado en noviembre de 2026", course().spokenMetaText())
        assertEquals("2 módulos", course(publication = null).spokenMetaText())
    }

    // --- Progreso y botón ---

    @Test
    fun `with nothing mastered the button starts the course and there is no progress line`() {
        assertEquals("Empezar el curso", startButtonText(masteredCount = 0))
        assertNull(progressText(masteredCount = 0, total = 2))
    }

    @Test
    fun `with something mastered the button continues and the progress is shown`() {
        assertEquals("Continuar el curso", startButtonText(masteredCount = 1))
        assertEquals("1 de 2 dominados", progressText(masteredCount = 1, total = 2))
    }

    @Test
    fun `with everything mastered the course can be reviewed`() {
        assertEquals("Repasar el curso", startButtonText(masteredCount = 2, total = 2))
        assertEquals("2 de 2 dominados", progressText(masteredCount = 2, total = 2))
    }
}
