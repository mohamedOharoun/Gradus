package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// UnenrollFromCourse: validación de sesión, desenlaces del servidor, propagación de fallos del
// almacén y exhaustividad de UnenrollResult.
class UnenrollFromCourseTest {

    // Envuelve un EnrollmentStore para registrar llamadas a unenroll y simular fallos del almacén.
    private class SpyEnrollmentStore(
        private val delegate: EnrollmentStore,
        private val onUnenroll: suspend (userId: String, courseId: String) -> Unit = { userId, courseId ->
            delegate.unenroll(userId, courseId)
        },
    ) : EnrollmentStore by delegate {
        val unenrollCalls = mutableListOf<Pair<String, String>>()

        override suspend fun unenroll(userId: String, courseId: String) {
            unenrollCalls += userId to courseId
            onUnenroll(userId, courseId)
        }
    }

    // Envuelve SessionStore para simular fallos al leer o borrar la sesión.
    private class FailingSessionStore(
        private val delegate: SessionStore,
        private val failOnCurrent: Throwable? = null,
        private val failOnClear: Throwable? = null,
    ) : SessionStore by delegate {
        override suspend fun current(): Session? = failOnCurrent?.let { throw it } ?: delegate.current()
        override suspend fun clear() = failOnClear?.let { throw it } ?: delegate.clear()
    }

    private fun answering(outcome: UnenrollOutcome) =
        FakeProgressGateway(unenrollAnswer = { _, _ -> outcome })

    private suspend fun anaWithGraphsAndTrees(): ProgressFixture =
        ProgressFixture().enrolledIn("grafos", "arboles").also {
            it.store.markMastered("grafos", "bfs")
            it.store.markMastered("arboles", "raiz")
        }

    @Test
    fun `without a session it returns NoSession without calling gateway or store`() = runTest {
        val fixture = ProgressFixture()
        val spyStore = SpyEnrollmentStore(fixture.store)
        val gateway = FakeProgressGateway()

        val result = UnenrollFromCourse(fixture.sessions, gateway, spyStore).unenroll("grafos")

        assertEquals(UnenrollResult.NoSession, result)
        assertTrue(gateway.unenrollCalls.isEmpty())
        assertTrue(spyStore.unenrollCalls.isEmpty())
    }

    @Test
    fun `Unenrolled sends stored session and courseId clears local progress and keeps session`() = runTest {
        val fixture = anaWithGraphsAndTrees()
        val spyStore = SpyEnrollmentStore(fixture.store)
        var receivedSession: Session? = null
        val gateway = FakeProgressGateway(
            unenrollAnswer = { session, _ ->
                receivedSession = session
                UnenrollOutcome.Unenrolled
            },
        )

        val result = UnenrollFromCourse(fixture.sessions, gateway, spyStore).unenroll("grafos")

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.Unenrolled), result)
        assertEquals(ANA, receivedSession)
        assertEquals(listOf(UnenrollCall("ana", "grafos")), gateway.unenrollCalls)
        assertEquals(listOf("ana" to "grafos"), spyStore.unenrollCalls)
        assertFalse(fixture.store.enrolled("grafos").first())
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
        assertTrue(fixture.store.enrolled("arboles").first())
        assertEquals(setOf("raiz"), fixture.store.mastered("arboles").first())
        assertEquals(ANA, fixture.sessions.current())
    }

    @Test
    fun `Unenrolled delegates to store even when course was not enrolled locally`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        val spyStore = SpyEnrollmentStore(fixture.store)
        val gateway = answering(UnenrollOutcome.Unenrolled)

        val result = UnenrollFromCourse(fixture.sessions, gateway, spyStore).unenroll("grafos")

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.Unenrolled), result)
        assertEquals(listOf("ana" to "grafos"), spyStore.unenrollCalls)
        assertEquals(ANA, fixture.sessions.current())
    }

    @Test
    fun `Unreachable keeps session and local progress without calling store and makes one call`() = runTest {
        assertFailureKeepsState(UnenrollOutcome.Unreachable)
    }

    @Test
    fun `ServerFailure keeps session and local progress without calling store and makes one call`() = runTest {
        assertFailureKeepsState(UnenrollOutcome.ServerFailure)
    }

    private suspend fun assertFailureKeepsState(outcome: UnenrollOutcome) {
        val fixture = anaWithGraphsAndTrees()
        val before = fixture.store.progressOf("ana")
        val spyStore = SpyEnrollmentStore(fixture.store)
        val gateway = answering(outcome)

        val result = UnenrollFromCourse(fixture.sessions, gateway, spyStore).unenroll("grafos")

        assertEquals(UnenrollResult.Finished(outcome), result)
        assertEquals(listOf(UnenrollCall("ana", "grafos")), gateway.unenrollCalls)
        assertTrue(spyStore.unenrollCalls.isEmpty())
        assertEquals(before, fixture.store.progressOf("ana"))
        assertEquals(ANA, fixture.sessions.current())
    }

    @Test
    fun `SessionExpired clears current session keeps local progress and makes one call`() = runTest {
        val fixture = anaWithGraphsAndTrees()
        val before = fixture.store.progressOf("ana")
        val spyStore = SpyEnrollmentStore(fixture.store)
        val gateway = answering(UnenrollOutcome.SessionExpired)

        val result = UnenrollFromCourse(fixture.sessions, gateway, spyStore).unenroll("grafos")

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.SessionExpired), result)
        assertEquals(listOf(UnenrollCall("ana", "grafos")), gateway.unenrollCalls)
        assertTrue(spyStore.unenrollCalls.isEmpty())
        assertEquals(before, fixture.store.progressOf("ana"))
        assertNull(fixture.sessions.current())
    }

    @Test
    fun `exception from sessions current propagates without calling gateway or store`() = runTest {
        val fixture = anaWithGraphsAndTrees()
        val failingSessions = FailingSessionStore(fixture.sessions, failOnCurrent = IllegalStateException("storage"))
        val spyStore = SpyEnrollmentStore(fixture.store)
        val gateway = FakeProgressGateway()

        val error = assertFailsWith<IllegalStateException> {
            UnenrollFromCourse(failingSessions, gateway, spyStore).unenroll("grafos")
        }

        assertEquals("storage", error.message)
        assertTrue(gateway.unenrollCalls.isEmpty())
        assertTrue(spyStore.unenrollCalls.isEmpty())
    }

    @Test
    fun `exception from enrollments unenroll propagates`() = runTest {
        val fixture = anaWithGraphsAndTrees()
        val spyStore = SpyEnrollmentStore(fixture.store) { _, _ -> throw IllegalStateException("db failed") }
        val gateway = answering(UnenrollOutcome.Unenrolled)

        val error = assertFailsWith<IllegalStateException> {
            UnenrollFromCourse(fixture.sessions, gateway, spyStore).unenroll("grafos")
        }

        assertEquals("db failed", error.message)
        assertEquals(ANA, fixture.sessions.current())
    }

    @Test
    fun `exception from sessions clear propagates on SessionExpired`() = runTest {
        val fixture = anaWithGraphsAndTrees()
        val failingSessions = FailingSessionStore(fixture.sessions, failOnClear = IllegalStateException("clear failed"))
        val spyStore = SpyEnrollmentStore(fixture.store)
        val gateway = answering(UnenrollOutcome.SessionExpired)

        val error = assertFailsWith<IllegalStateException> {
            UnenrollFromCourse(failingSessions, gateway, spyStore).unenroll("grafos")
        }

        assertEquals("clear failed", error.message)
        assertTrue(spyStore.unenrollCalls.isEmpty())
    }

    @Test
    fun `UnenrollResult and UnenrollOutcome support exhaustive when without else`() {
        val results: List<UnenrollResult> = listOf(
            UnenrollResult.NoSession,
            UnenrollResult.Finished(UnenrollOutcome.Unenrolled),
            UnenrollResult.Finished(UnenrollOutcome.SessionExpired),
            UnenrollResult.Finished(UnenrollOutcome.Unreachable),
            UnenrollResult.Finished(UnenrollOutcome.ServerFailure),
        )

        val labels = results.map { result ->
            when (result) {
                UnenrollResult.NoSession -> "no-session"
                is UnenrollResult.Finished -> when (result.outcome) {
                    UnenrollOutcome.Unenrolled -> "unenrolled"
                    UnenrollOutcome.SessionExpired -> "session-expired"
                    UnenrollOutcome.Unreachable -> "unreachable"
                    UnenrollOutcome.ServerFailure -> "server-failure"
                }
            }
        }

        assertEquals(
            listOf("no-session", "unenrolled", "session-expired", "unreachable", "server-failure"),
            labels,
        )
    }
}
