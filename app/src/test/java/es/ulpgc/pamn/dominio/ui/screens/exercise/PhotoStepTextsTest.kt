package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.study.PhotoStep
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

// Los textos del paso con foto (docs/tareas/ejercicio-foto.md, paso 5c; con UiText desde ajustes.md, paso 8b):
// qué se dice en cada fallo y qué botones hay abajo según el estado.
class PhotoStepTextsTest {

    private val empty = PhotoStep(Step.Photo("foto", "Dibuja el grafo.", "Cuatro vértices."))
    private val ready = empty.take("a.jpg")
    private val sent = ready.send().finished(SubmitOutcome.Submitted(Submission("s1", "2026-11-25T10:00:00Z")))

    private fun failed(problem: SubmitOutcome.Problem) = ready.send().finished(problem)

    // ── Los fallos ──

    // Decisión J: sin conexión, se puede reintentar.
    @Test
    fun `without connection it says so and invites to try again`() {
        assertEquals(UiText.of(R.string.photo_error_unreachable), SubmitOutcome.Unreachable.text())
    }

    // Decisión Y.
    @Test
    fun `an expired session asks to log in again`() {
        assertEquals(UiText.of(R.string.photo_error_session_expired), SubmitOutcome.SessionExpired.text())
    }

    @Test
    fun `every problem has its own text`() {
        val problems = listOf(
            SubmitOutcome.Unreachable,
            SubmitOutcome.ServerFailure,
            SubmitOutcome.SessionExpired,
            SubmitOutcome.NotFound,
        ) + PhotoRejection.entries.map { SubmitOutcome.PhotoRejected(it) }
        val texts = problems.map { it.text() }
        assertEquals(texts.size, texts.toSet().size)
    }

    // Una foto rechazada no se reintenta: el texto pide otra.
    @Test
    fun `a rejected photo asks for another one`() {
        assertEquals(UiText.of(R.string.photo_error_missing), SubmitOutcome.PhotoRejected(PhotoRejection.MISSING).text())
        assertEquals(UiText.of(R.string.photo_error_not_jpeg), SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG).text())
        assertEquals(UiText.of(R.string.photo_error_too_large), SubmitOutcome.PhotoRejected(PhotoRejection.TOO_LARGE).text())
    }

    // ── El botón principal ──

    private val submit = UiText.of(R.string.photo_submit)

    @Test
    fun `the main button submits, and is only enabled with a photo`() {
        assertEquals(MainPhotoButton(submit, enabled = false), empty.mainButton(canSubmit = true))
        assertEquals(MainPhotoButton(submit, enabled = true), ready.mainButton(canSubmit = true))
    }

    @Test
    fun `while sending it says so and cannot be pressed`() {
        assertEquals(MainPhotoButton(UiText.of(R.string.photo_submitting), enabled = false), ready.send().mainButton(canSubmit = true))
    }

    @Test
    fun `after a problem that can be retried it retries`() {
        assertEquals(MainPhotoButton(UiText.of(R.string.action_retry), enabled = true), failed(SubmitOutcome.Unreachable).mainButton(canSubmit = true))
    }

    // Una foto rechazada: «Entregar» queda apagado hasta que se haga otra.
    @Test
    fun `after a rejected photo it waits for another one`() {
        val rejected = failed(SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG))
        assertEquals(MainPhotoButton(submit, enabled = false), rejected.mainButton(canSubmit = true))
    }

    // Decisión Z: en un curso que no está publicado nunca se puede entregar.
    @Test
    fun `in a course that is not published it cannot be pressed`() {
        assertFalse(ready.mainButton(canSubmit = false).enabled)
    }

    // Entregada: el principal es «Continuar» (decisión C: no se espera a la revisión).
    @Test
    fun `once sent the main button goes on`() {
        assertEquals(MainPhotoButton(UiText.of(R.string.action_continue), enabled = true), sent.mainButton(canSubmit = true))
    }

    // ── El botón secundario ──

    @Test
    fun `without a submission the second button leaves it for later`() {
        assertEquals(SecondaryPhotoButton.LATER, empty.secondaryButton())
        assertEquals(SecondaryPhotoButton.LATER, failed(SubmitOutcome.NotFound).secondaryButton())
    }

    @Test
    fun `once sent the second button submits again`() {
        assertEquals(SecondaryPhotoButton.AGAIN, sent.secondaryButton())
    }

    // Tras «Entregar otra vez», la anterior sigue valiendo: se puede seguir sin volver a entregar.
    @Test
    fun `after submitting again the second button goes on with the previous submission`() {
        assertEquals(SecondaryPhotoButton.GO_ON, sent.again().secondaryButton())
    }

    @Test
    fun `each second button says what it does`() {
        assertEquals(UiText.of(R.string.photo_later), SecondaryPhotoButton.LATER.text)
        assertEquals(UiText.of(R.string.photo_again), SecondaryPhotoButton.AGAIN.text)
        assertEquals(UiText.of(R.string.photo_go_on), SecondaryPhotoButton.GO_ON.text)
    }

    @Test
    fun `while sending there is no second button`() {
        assertEquals(null, ready.send().secondaryButton())
    }

    @Test
    fun `the texts of the two buttons are never the same`() {
        listOf(empty, ready, sent, sent.again(), failed(SubmitOutcome.Unreachable)).forEach {
            assertNotEquals(it.mainButton(canSubmit = true).text, it.secondaryButton()?.text)
        }
    }
}
