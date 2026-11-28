package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Concurrencia y cancelación en UnenrollFromCourse: cambios o cierre de sesión mientras el DELETE
// viaja por la red, y propagación limpia de CancellationException.
class UnenrollFromCourseConcurrencyTest {

    private suspend fun anaWithGraphs(): ProgressFixture =
        ProgressFixture().enrolledIn("grafos").also { it.store.markMastered("grafos", "bfs") }

    // Inicia la baja de «grafos», aplica `whileInFlight` mientras el DELETE espera y suelta `outcome`.
    private suspend fun CoroutineScope.runWhileSessionChanges(
        fixture: ProgressFixture,
        outcome: UnenrollOutcome,
        whileInFlight: suspend () -> Unit,
    ): UnenrollResult {
        val gate = CompletableDeferred<UnenrollOutcome>()
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> gate.await() })
        val useCase = UnenrollFromCourse(fixture.sessions, gateway, fixture.store)
        val deferred = async { useCase.unenroll("grafos") }
        yield() // el DELETE ya salió y espera respuesta
        whileInFlight()
        gate.complete(outcome)
        return deferred.await()
    }

    @Test
    fun `SessionExpired does not clear a newer session of the same user`() = runTest {
        val fixture = anaWithGraphs()
        val before = fixture.store.progressOf("ana")
        val newer = ANA.copy(token = "t-nuevo")

        val result = runWhileSessionChanges(fixture, UnenrollOutcome.SessionExpired) {
            fixture.sessions.save(newer)
        }

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.SessionExpired), result)
        assertEquals(newer, fixture.sessions.current())
        assertEquals(before, fixture.store.progressOf("ana"))
    }

    @Test
    fun `SessionExpired does not clear the session of another user`() = runTest {
        val fixture = anaWithGraphs()
        val before = fixture.store.progressOf("ana")
        val beto = sessionOf("beto")

        val result = runWhileSessionChanges(fixture, UnenrollOutcome.SessionExpired) {
            fixture.sessions.save(beto)
        }

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.SessionExpired), result)
        assertEquals(beto, fixture.sessions.current())
        assertEquals(before, fixture.store.progressOf("ana"))
    }

    @Test
    fun `SessionExpired when the session was already cleared does not throw`() = runTest {
        val fixture = anaWithGraphs()
        val before = fixture.store.progressOf("ana")

        val result = runWhileSessionChanges(fixture, UnenrollOutcome.SessionExpired) {
            fixture.sessions.clear()
        }

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.SessionExpired), result)
        assertNull(fixture.sessions.current())
        assertEquals(before, fixture.store.progressOf("ana"))
    }

    @Test
    fun `Unenrolled after session changes to another user removes only the original user course`() = runTest {
        val fixture = anaWithGraphs()
        val beto: Session = sessionOf("beto")
        fixture.sessions.save(beto)
        fixture.store.enroll("grafos")
        fixture.store.markMastered("grafos", "dfs")
        val expectedBeto = fixture.store.progressOf("beto")
        fixture.sessions.save(ANA)

        val result = runWhileSessionChanges(fixture, UnenrollOutcome.Unenrolled) {
            fixture.sessions.save(beto)
        }

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.Unenrolled), result)
        assertEquals(beto, fixture.sessions.current())
        assertEquals(UserProgress(), fixture.store.progressOf("ana"))
        assertEquals(expectedBeto, fixture.store.progressOf("beto"))
        assertTrue(fixture.store.enrolled("grafos").first())
        assertEquals(setOf("dfs"), fixture.store.mastered("grafos").first())
    }

    @Test
    fun `Unenrolled after session is cleared still removes the original user course`() = runTest {
        val fixture = anaWithGraphs()

        val result = runWhileSessionChanges(fixture, UnenrollOutcome.Unenrolled) {
            fixture.sessions.clear()
        }

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.Unenrolled), result)
        assertNull(fixture.sessions.current())
        assertEquals(UserProgress(), fixture.store.progressOf("ana"))
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `Unenrolled after session is renewed keeps the newer session and removes the course`() = runTest {
        val fixture = anaWithGraphs()
        val newer = ANA.copy(token = "t-nuevo")

        val result = runWhileSessionChanges(fixture, UnenrollOutcome.Unenrolled) {
            fixture.sessions.save(newer)
        }

        assertEquals(UnenrollResult.Finished(UnenrollOutcome.Unenrolled), result)
        assertEquals(newer, fixture.sessions.current())
        assertFalse(fixture.store.enrolled("grafos").first())
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
    }

    @Test
    fun `CancellationException while waiting for gateway propagates without touching store or session`() = runTest {
        val fixture = anaWithGraphs()
        val before = fixture.store.progressOf("ana")
        val gate = CompletableDeferred<UnenrollOutcome>()
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> gate.await() })
        val useCase = UnenrollFromCourse(fixture.sessions, gateway, fixture.store)

        val job = async { useCase.unenroll("grafos") }
        yield()
        job.cancel()
        gate.complete(UnenrollOutcome.Unenrolled)
        yield()

        assertTrue(job.isCancelled)
        assertFailsWith<CancellationException> { job.await() }
        assertEquals(before, fixture.store.progressOf("ana"))
        assertEquals(ANA, fixture.sessions.current())
    }

    @Test
    fun `CancellationException from enrollments unenroll propagates`() = runTest {
        val fixture = anaWithGraphs()
        val cancellingStore = object : EnrollmentStore by fixture.store {
            override suspend fun unenroll(userId: String, courseId: String) {
                throw CancellationException("cancelled during store write")
            }
        }
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> UnenrollOutcome.Unenrolled })
        val useCase = UnenrollFromCourse(fixture.sessions, gateway, cancellingStore)

        val error = assertFailsWith<CancellationException> {
            useCase.unenroll("grafos")
        }

        assertEquals("cancelled during store write", error.message)
        assertEquals(ANA, fixture.sessions.current())
    }
}
