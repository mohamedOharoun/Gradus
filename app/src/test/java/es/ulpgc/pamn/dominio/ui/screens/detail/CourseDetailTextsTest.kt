package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.longMonth
import es.ulpgc.pamn.dominio.ui.text.shortMonth
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
        // El nombre lo escribe la persona: va como dato, sin traducir.
        assertEquals(UiText.of(R.string.detail_by_author, "Ana Ruiz"), course().authorText())
    }

    @Test
    fun `a course without publication has no author line`() {
        assertNull(course(publication = null).authorText())
    }

    // --- Estadísticas y publicación (rediseño de la ficha) ---

    private val nov2026 = UiText.of(R.string.month_year_short, shortMonth(11), 2026)
    private val publishedNov2026 = UiText.of(R.string.course_published, UiText.of(R.string.month_year_long, longMonth(11), 2026))

    @Test
    fun `modules stat label uses plural for zero, singular for one and plural for many`() {
        assertEquals(UiText.plural(R.plurals.detail_modules_label, 0), modulesStatLabel(0))
        assertEquals(UiText.plural(R.plurals.detail_modules_label, 1), modulesStatLabel(1))
        assertEquals(UiText.plural(R.plurals.detail_modules_label, 14), modulesStatLabel(14))
    }

    @Test
    fun `published short date formats abbreviated month and year`() {
        assertEquals(nov2026, course().publishedShortDate())
    }

    @Test
    fun `published short date is null without publication or with invalid date`() {
        assertNull(course(publication = null).publishedShortDate())
        assertNull(course(publication = published.copy(publishedAt = "ayer")).publishedShortDate())
        assertNull(course(publication = published.copy(publishedAt = "")).publishedShortDate())
    }

    @Test
    fun `published in line uses abbreviated month and TalkBack hears the full month`() {
        assertEquals(UiText.of(R.string.detail_published_in, nov2026), course().publishedInText())
        assertEquals(publishedNov2026, course().spokenPublishedText())
    }

    @Test
    fun `published in line and spoken text are null without publication or with invalid date`() {
        assertNull(course(publication = null).publishedInText())
        assertNull(course(publication = null).spokenPublishedText())
        assertNull(course(publication = published.copy(publishedAt = "ayer")).publishedInText())
        assertNull(course(publication = published.copy(publishedAt = "ayer")).spokenPublishedText())
    }

    // --- Bloque de progreso ---

    @Test
    fun `progress percent formats the rounded percentage`() {
        assertEquals(UiText.of(R.string.detail_progress_percent, 0), progressPercentText(0))
        assertEquals(UiText.of(R.string.detail_progress_percent, 43), progressPercentText(43))
        assertEquals(UiText.of(R.string.detail_progress_percent, 100), progressPercentText(100))
    }

    @Test
    fun `mastered modules text picks singular for one total module and plural otherwise`() {
        assertEquals(UiText.plural(R.plurals.detail_progress_modules, 0, 0, 0), masteredModulesText(0, 0))
        assertEquals(UiText.plural(R.plurals.detail_progress_modules, 1, 0, 1), masteredModulesText(0, 1))
        assertEquals(UiText.plural(R.plurals.detail_progress_modules, 1, 1, 1), masteredModulesText(1, 1))
        assertEquals(UiText.plural(R.plurals.detail_progress_modules, 14, 0, 14), masteredModulesText(0, 14))
        assertEquals(UiText.plural(R.plurals.detail_progress_modules, 14, 6, 14), masteredModulesText(6, 14))
        assertEquals(UiText.plural(R.plurals.detail_progress_modules, 14, 14, 14), masteredModulesText(14, 14))
    }

    // --- Botón inferior CTA y confirmación de baja ---

    @Test
    fun `CTA button reflects enrollment and enrolling states`() {
        assertEquals(UiText.of(R.string.detail_enroll), ctaButtonText(enrolled = false, enrolling = false))
        assertEquals(UiText.of(R.string.enroll_enrolling), ctaButtonText(enrolled = false, enrolling = true))
        assertEquals(UiText.of(R.string.detail_go_to_course), ctaButtonText(enrolled = true, enrolling = false))
        // Caso defensivo: si ya está inscrito, siempre prevalece «Ir al curso».
        assertEquals(UiText.of(R.string.detail_go_to_course), ctaButtonText(enrolled = true, enrolling = true))
    }

    @Test
    fun `unenroll confirm button shows sending state only while sending`() {
        assertEquals(UiText.of(R.string.detail_unenroll), UnenrollDialogState.Closed.confirmButtonText())
        assertEquals(UiText.of(R.string.detail_unenroll), UnenrollDialogState.Confirming.confirmButtonText())
        assertEquals(UiText.of(R.string.detail_unenrolling), UnenrollDialogState.Sending.confirmButtonText())
        assertEquals(
            UiText.of(R.string.detail_unenroll),
            UnenrollDialogState.Failed(FailureReason.UNREACHABLE).confirmButtonText(),
        )
        assertEquals(
            UiText.of(R.string.detail_unenroll),
            UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE).confirmButtonText(),
        )
    }
}
