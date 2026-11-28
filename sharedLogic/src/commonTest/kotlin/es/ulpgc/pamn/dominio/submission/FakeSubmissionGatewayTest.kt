package es.ulpgc.pamn.dominio.submission

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

// El gateway falso de entregas: para las previews, los tests del ViewModel y probar la app sin backend.
class FakeSubmissionGatewayTest {

    private val session = Session("token", "2026-12-25T00:00:00Z", Account("u1", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))
    private val photo = PhotoSubmission("c1", 1, "repr-foto", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))

    @Test
    fun `by default it accepts every photo`() = runTest {
        assertIs<SubmitOutcome.Submitted>(FakeSubmissionGateway().submit(session, photo))
    }

    // Decisión I: cada envío es una entrega nueva.
    @Test
    fun `every accepted photo is a new submission`() = runTest {
        val gateway = FakeSubmissionGateway()
        val first = assertIs<SubmitOutcome.Submitted>(gateway.submit(session, photo)).submission
        val second = assertIs<SubmitOutcome.Submitted>(gateway.submit(session, photo)).submission
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `it answers what it is told`() = runTest {
        assertEquals(SubmitOutcome.Unreachable, FakeSubmissionGateway(SubmitOutcome.Unreachable).submit(session, photo))
        val rejected = SubmitOutcome.PhotoRejected(PhotoRejection.TOO_LARGE)
        assertEquals(rejected, FakeSubmissionGateway(rejected).submit(session, photo))
    }

    // Respuestas distintas en cada intento: fallar y, al reintentar, entregar.
    @Test
    fun `it can answer differently each time`() = runTest {
        val answers = ArrayDeque(listOf(SubmitOutcome.Unreachable, SubmitOutcome.SessionExpired))
        val gateway = FakeSubmissionGateway(answer = { _, _ -> answers.removeFirst() })
        assertEquals(SubmitOutcome.Unreachable, gateway.submit(session, photo))
        assertEquals(SubmitOutcome.SessionExpired, gateway.submit(session, photo))
    }

    @Test
    fun `it remembers what it was asked, with which session`() = runTest {
        val gateway = FakeSubmissionGateway()
        gateway.submit(session, photo)
        assertEquals(listOf(photo), gateway.received)
        assertEquals(listOf(session), gateway.sessions)
    }
}
