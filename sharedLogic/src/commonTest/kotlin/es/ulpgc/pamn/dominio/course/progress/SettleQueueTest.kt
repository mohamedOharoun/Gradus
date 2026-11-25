package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

// settle, primera mitad: lo enviado sale de la cola (por clave), y nada más.
class SettleQueueTest {

    @Test
    fun `what was sent leaves the queue`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        fixture.uploadAll()
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `the queue is cleared by key not by date`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 123_456_789.nanoseconds
        fixture.store.markMastered("grafos", "bfs")
        // Las fechas de sent no coinciden con las del móvil (como las redondeadas a microsegundos).
        val sent = snapshot(enrollment("grafos", START + 1.minutes, "bfs" to START + 123_457_000.nanoseconds))
        fixture.store.settle("ana", sent, remote = sent)
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `an enrollment made during the request stays pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val sent = fixture.store.pending("ana")
        fixture.store.enroll("redes")
        fixture.store.settle("ana", sent, remote = sent)
        assertSameSnapshot(snapshot(enrollment("redes", START)), fixture.store.pending("ana"))
    }

    @Test
    fun `a module mastered during the request stays pending even in a course that was sent`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val sent = fixture.store.pending("ana")
        fixture.clock.time = START + 2.minutes
        fixture.store.markMastered("grafos", "dfs")
        fixture.store.settle("ana", sent, remote = sent)
        assertSameSnapshot(
            snapshot(enrollment("grafos", START, "dfs" to START + 2.minutes)),
            fixture.store.pending("ana"),
        )
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `a skipped enrollment leaves the queue and stays on the phone`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val sent = fixture.store.pending("ana")
        fixture.store.settle("ana", sent, remote = snapshot())
        assertTrue(fixture.store.pending("ana").isEmpty)
        assertTrue(fixture.store.enrolled("grafos").first())
        assertEquals(setOf("bfs"), fixture.store.mastered("grafos").first())
    }

    @Test
    fun `a skipped module leaves the queue and stays on the phone`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val sent = fixture.store.pending("ana")
        fixture.store.settle("ana", sent, remote = snapshot(enrollment("grafos", START)))
        assertTrue(fixture.store.pending("ana").isEmpty)
        assertEquals(setOf("bfs"), fixture.store.mastered("grafos").first())
    }

    @Test
    fun `what was sent but is not on the phone is ignored`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val sent = snapshot(enrollment("redes", START, "x" to START))
        fixture.store.settle("ana", sent, remote = snapshot())
        assertEquals(setOf("grafos"), fixture.store.progressOf("ana").courses.keys)
        assertSameSnapshot(snapshot(enrollment("grafos", START)), fixture.store.pending("ana"))
    }

    @Test
    fun `settling one user does not touch another`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.logInAs("bea")
        fixture.store.enroll("redes")
        val beaBefore = fixture.store.progressOf("bea")
        fixture.uploadAll("ana")
        assertEquals(beaBefore, fixture.store.progressOf("bea"))
        assertSameSnapshot(snapshot(enrollment("redes", START)), fixture.store.pending("bea"))
    }
}
