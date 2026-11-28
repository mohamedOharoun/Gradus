package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// SessionProgressSync: la sesión guardada + ProgressSync + el 401 (borra la sesión si sigue siendo la misma).
class SessionProgressSyncTest {

    private fun ProgressFixture.sessionSyncWith(gateway: ProgressGateway) =
        SessionProgressSync(sessions, syncWith(gateway))

    private fun answering(outcome: SyncOutcome) = FakeProgressGateway(answer = { _, _ -> outcome })

    @Test
    fun `without a session it returns NoSession and does not call the server`() = runTest {
        val fixture = ProgressFixture()
        val gateway = FakeProgressGateway()
        val result = fixture.sessionSyncWith(gateway).run()
        assertEquals(SessionSyncResult.NoSession, result)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun `it syncs with the stored session`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val gateway = FakeProgressGateway()
        fixture.sessionSyncWith(gateway).run()
        assertEquals(listOf("ana"), gateway.calls.map { it.userId })
    }

    @Test
    fun `Synced keeps the session and is returned as it is`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val outcome = synced()
        val result = fixture.sessionSyncWith(answering(outcome)).run()
        assertEquals(SessionSyncResult.Finished(outcome), result)
        assertEquals(ANA, fixture.sessions.current())
    }

    @Test
    fun `SessionExpired clears the session and is returned as it is`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val result = fixture.sessionSyncWith(answering(SyncOutcome.SessionExpired)).run()
        assertEquals(SessionSyncResult.Finished(SyncOutcome.SessionExpired), result)
        assertNull(fixture.sessions.current())
    }

    @Test
    fun `SessionExpired does not clear a newer session of the same user`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val newer = ANA.copy(token = "t-nuevo")
        assertEquals(newer, savedWhileRequestTravels(fixture, newer))
    }

    @Test
    fun `SessionExpired does not clear the session of another user`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val other = sessionOf("beto")
        assertEquals(other, savedWhileRequestTravels(fixture, other))
    }

    // Si la sesión ya se borró mientras la petición viajaba, no lanza y sigue sin sesión.
    @Test
    fun `SessionExpired when the session was already cleared does not throw`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val gate = CompletableDeferred<SyncOutcome>()
        val sessionSync = fixture.sessionSyncWith(FakeProgressGateway(answer = { _, _ -> gate.await() }))
        val result = async { sessionSync.run() }
        yield() // la petición sale y espera
        fixture.sessions.clear()
        gate.complete(SyncOutcome.SessionExpired)
        assertEquals(SessionSyncResult.Finished(SyncOutcome.SessionExpired), result.await())
        assertNull(fixture.sessions.current())
    }

    @Test
    fun `Unreachable keeps the session and what is pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val result = fixture.sessionSyncWith(answering(SyncOutcome.Unreachable)).run()
        assertEquals(SessionSyncResult.Finished(SyncOutcome.Unreachable), result)
        assertEquals(ANA, fixture.sessions.current())
        assertFalse(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `ServerFailure keeps the session and what is pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val result = fixture.sessionSyncWith(answering(SyncOutcome.ServerFailure)).run()
        assertEquals(SessionSyncResult.Finished(SyncOutcome.ServerFailure), result)
        assertEquals(ANA, fixture.sessions.current())
        assertFalse(fixture.store.pending("ana").isEmpty)
    }

    // Solo Synced vacía la cola (settle): con SessionExpired lo pendiente sigue pendiente.
    @Test
    fun `SessionExpired keeps what is pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.sessionSyncWith(answering(SyncOutcome.SessionExpired)).run()
        assertFalse(fixture.store.pending("ana").isEmpty)
    }

    // Lanza la sincronización, guarda `other` mientras la petición viaja y responde 401.
    // Devuelve la sesión que queda guardada.
    private suspend fun CoroutineScope.savedWhileRequestTravels(
        fixture: ProgressFixture,
        other: Session,
    ): Session? {
        val gate = CompletableDeferred<SyncOutcome>()
        val sessionSync = fixture.sessionSyncWith(FakeProgressGateway(answer = { _, _ -> gate.await() }))
        val result = async { sessionSync.run() }
        yield() // la petición sale y espera
        fixture.sessions.save(other)
        gate.complete(SyncOutcome.SessionExpired)
        result.await()
        return fixture.sessions.current()
    }
}
