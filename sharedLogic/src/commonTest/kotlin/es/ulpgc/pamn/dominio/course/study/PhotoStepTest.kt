package es.ulpgc.pamn.dominio.course.study

import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// El estado de una tarea con foto mientras se estudia (docs/tareas/ejercicio-foto.md, paso 4b):
// sin foto → con foto → entregando → entregada o fallo; reintentar, «Entregar otra vez» y «más tarde».
class PhotoStepTest {

    private val task = Step.Photo("foto", "Dibuja el grafo.", "Cuatro vértices.")
    private val empty = PhotoStep(task)
    private val submission = Submission(id = "s1", submittedAt = "2026-11-25T10:00:00Z")
    private val other = Submission(id = "s2", submittedAt = "2026-11-25T10:05:00Z")

    private fun ready(path: String = "a.jpg") = empty.take(path)
    private fun sending() = ready().send()
    private fun sent() = sending().finished(SubmitOutcome.Submitted(submission))
    private fun failed(problem: SubmitOutcome.Problem) = sending().finished(problem)

    // ── Sin foto ──

    @Test
    fun `it starts without a photo, not sent and not done`() {
        assertEquals(PhotoTask.Empty, empty.task)
        assertNull(empty.submission)
        assertFalse(empty.isDone)
        assertFalse(empty.canSend)
    }

    @Test
    fun `without a photo there is nothing to send or discard`() {
        assertEquals(empty, empty.send())
        assertEquals(empty, empty.discard())
    }

    // ── Con foto ──

    @Test
    fun `taking a photo makes it ready to send`() {
        assertEquals(PhotoTask.Ready("a.jpg"), ready().task)
        assertTrue(ready().canSend)
    }

    // Repetir la foto antes de entregarla: cuenta la nueva.
    @Test
    fun `taking another photo replaces the one that was ready`() {
        assertEquals(PhotoTask.Ready("b.jpg"), ready("a.jpg").take("b.jpg").task)
    }

    @Test
    fun `discarding the photo goes back to having none`() {
        assertEquals(PhotoTask.Empty, ready().discard().task)
    }

    // El fichero de la foto: la app lo borra cuando deja de estar aquí (decisión K).
    @Test
    fun `the photo file is known while it is ready, sending or failed`() {
        assertNull(empty.photoPath)
        assertEquals("a.jpg", ready().photoPath)
        assertEquals("a.jpg", sending().photoPath)
        assertEquals("a.jpg", failed(SubmitOutcome.Unreachable).photoPath)
        assertNull(sent().photoPath)
    }

    // ── Entregando ──

    @Test
    fun `sending keeps the photo and waits`() {
        assertEquals(PhotoTask.Sending("a.jpg"), sending().task)
        assertFalse(sending().canSend)
        assertFalse(sending().isDone)
    }

    // Mientras se entrega, nada cambia la foto ni se envía dos veces.
    @Test
    fun `while sending the photo cannot be changed, discarded or sent again`() {
        val sending = sending()
        assertEquals(sending, sending.take("b.jpg"))
        assertEquals(sending, sending.discard())
        assertEquals(sending, sending.send())
    }

    @Test
    fun `while sending it cannot be left for later`() {
        assertFalse(sending().canLeaveForLater)
        assertEquals(sending(), sending().leaveForLater())
    }

    // ── Entregada ──

    // Decisión C: el alumno no espera a la revisión; ve la solución y sigue.
    @Test
    fun `once sent it is done and keeps its submission`() {
        val sent = sent()
        assertEquals(PhotoTask.Sent, sent.task)
        assertEquals(submission, sent.submission)
        assertTrue(sent.isDone)
        assertTrue(sent.showsSolution)
    }

    @Test
    fun `the solution is not shown before sending`() {
        assertFalse(empty.showsSolution)
        assertFalse(ready().showsSolution)
        assertFalse(sending().showsSolution)
        assertFalse(failed(SubmitOutcome.Unreachable).showsSolution)
    }

    @Test
    fun `once sent there is no photo to change until submitting again`() {
        assertEquals(sent(), sent().take("b.jpg"))
        assertEquals(sent(), sent().send())
    }

    // Decisión I: cada envío es una entrega nueva y cuenta la última.
    @Test
    fun `submitting again starts without a photo but stays done`() {
        val again = sent().again()
        assertEquals(PhotoTask.Empty, again.task)
        assertEquals(submission, again.submission)
        assertTrue(again.isDone)
        assertFalse(again.showsSolution)
    }

    @Test
    fun `the last submission is the one that counts`() {
        val second = sent().again().take("b.jpg").send().finished(SubmitOutcome.Submitted(other))
        assertEquals(other, second.submission)
    }

    // Si la segunda falla, la primera sigue valiendo.
    @Test
    fun `a failed second submission keeps the first`() {
        val second = sent().again().take("b.jpg").send().finished(SubmitOutcome.Unreachable)
        assertEquals(submission, second.submission)
        assertTrue(second.isDone)
    }

    @Test
    fun `submitting again only makes sense after sending`() {
        assertEquals(ready(), ready().again())
        assertEquals(sending(), sending().again())
    }

    // ── Fallos ──

    // Decisión J: sin conexión, aviso y «Reintentar» con la misma foto.
    @Test
    fun `without connection it fails keeping the photo and can be retried`() {
        val failed = failed(SubmitOutcome.Unreachable)
        assertEquals(PhotoTask.Failed("a.jpg", SubmitOutcome.Unreachable), failed.task)
        assertTrue(failed.canSend)
        assertEquals(PhotoTask.Sending("a.jpg"), failed.send().task)
    }

    @Test
    fun `a server failure can be retried`() {
        assertTrue(failed(SubmitOutcome.ServerFailure).canSend)
    }

    // Decisión L: tras volver a iniciar sesión, se reintenta con la misma foto.
    @Test
    fun `an expired session can be retried after logging in again`() {
        assertTrue(failed(SubmitOutcome.SessionExpired).canSend)
    }

    // Repetir el envío de una foto rechazada daría lo mismo: hay que hacer otra.
    @Test
    fun `a rejected photo cannot be sent again but another one can`() {
        val rejected = failed(SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG))
        assertFalse(rejected.canSend)
        assertEquals(rejected, rejected.send())
        assertEquals(PhotoTask.Ready("b.jpg"), rejected.take("b.jpg").task)
    }

    // El curso o el ejercicio ya no están en el servidor: reintentar no sirve; queda «más tarde».
    @Test
    fun `an exercise that no longer exists cannot be retried`() {
        val gone = failed(SubmitOutcome.NotFound)
        assertFalse(gone.canSend)
        assertTrue(gone.canLeaveForLater)
    }

    @Test
    fun `a failed photo can be discarded`() {
        assertEquals(PhotoTask.Empty, failed(SubmitOutcome.Unreachable).discard().task)
    }

    // ── Respuestas fuera de lugar ──

    // Solo cuenta la respuesta de un envío en curso: una tardía o repetida no cambia nada.
    @Test
    fun `an outcome that arrives when nothing is being sent changes nothing`() {
        assertEquals(ready(), ready().finished(SubmitOutcome.Submitted(other)))
        assertEquals(sent(), sent().finished(SubmitOutcome.Submitted(other)))
        assertEquals(empty, empty.finished(SubmitOutcome.Unreachable))
    }

    // ── Más tarde (decisión S) ──

    @Test
    fun `it can be left for later with or without a photo, and then it is done`() {
        assertTrue(empty.leaveForLater().isDone)
        assertTrue(ready().leaveForLater().isDone)
        assertTrue(failed(SubmitOutcome.Unreachable).leaveForLater().isDone)
    }

    // Decisión K: al salir del paso sin entregar, la foto temporal se borra.
    @Test
    fun `leaving for later lets go of the photo`() {
        assertEquals(PhotoTask.Empty, ready().leaveForLater().task)
        assertNull(failed(SubmitOutcome.Unreachable).leaveForLater().photoPath)
    }

    // Ya entregada, no hay nada que dejar para más tarde.
    @Test
    fun `a sent photo is not left for later`() {
        assertFalse(sent().canLeaveForLater)
        assertEquals(sent(), sent().leaveForLater())
    }

    @Test
    fun `which problems can be retried`() {
        assertTrue(SubmitOutcome.Unreachable.canRetry)
        assertTrue(SubmitOutcome.ServerFailure.canRetry)
        assertTrue(SubmitOutcome.SessionExpired.canRetry)
        assertFalse(SubmitOutcome.NotFound.canRetry)
        PhotoRejection.entries.forEach { assertFalse(SubmitOutcome.PhotoRejected(it).canRetry) }
    }

    @Test
    fun `a photo step keeps the task it belongs to`() {
        assertIs<Step.Photo>(sent().photo)
        assertEquals("foto", sent().photo.id)
    }
}
