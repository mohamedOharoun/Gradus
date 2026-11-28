package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// settle, segunda mitad: lo que llega del servidor se fusiona (unión, gana la fecha más antigua,
// no queda pendiente, nada local se borra).
class SettleMergeTest {

    private fun ProgressFixture.course(courseId: String) = store.progressOf("ana").courses.getValue(courseId)

    @Test
    fun `a course the phone did not have appears and is not pending`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        val remote = snapshot(enrollment("redes", START, "tcp" to START + 1.minutes))
        fixture.store.settle("ana", sent = snapshot(), remote = remote)
        assertTrue(fixture.store.enrolled("redes").first())
        assertEquals(setOf("tcp"), fixture.store.mastered("redes").first())
        assertEquals(START + 1.minutes, fixture.course("redes").mastered.getValue("tcp").masteredAt)
        assertTrue(fixture.store.pending("ana").isEmpty)
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `a new module of a known course appears and is not pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val sent = fixture.store.pending("ana")
        fixture.store.settle("ana", sent, remote = snapshot(enrollment("grafos", START, "bfs" to START)))
        assertEquals(setOf("bfs"), fixture.store.mastered("grafos").first())
        assertTrue(fixture.store.pending("ana").isEmpty)
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `the older enrollment date wins when it comes from the server`() = runTest {
        val fixture = ProgressFixture()
        fixture.clock.time = START + 10.minutes
        fixture.enrolledIn("grafos")
        fixture.store.settle("ana", fixture.store.pending("ana"), remote = snapshot(enrollment("grafos", START)))
        assertEquals(START, fixture.course("grafos").enrollment.enrolledAt)
    }

    @Test
    fun `the older enrollment date wins when it is local`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.settle("ana", snapshot(), remote = snapshot(enrollment("grafos", START + 10.minutes)))
        assertEquals(START, fixture.course("grafos").enrollment.enrolledAt)
    }

    @Test
    fun `the older mastered date wins when it comes from the server`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 10.minutes
        fixture.store.markMastered("grafos", "bfs")
        val remote = snapshot(enrollment("grafos", START, "bfs" to START + 1.minutes))
        fixture.store.settle("ana", fixture.store.pending("ana"), remote)
        assertEquals(START + 1.minutes, fixture.course("grafos").mastered.getValue("bfs").masteredAt)
    }

    @Test
    fun `the older mastered date wins when it is local`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 1.minutes
        fixture.store.markMastered("grafos", "bfs")
        val remote = snapshot(enrollment("grafos", START, "bfs" to START + 10.minutes))
        fixture.store.settle("ana", snapshot(), remote)
        assertEquals(START + 1.minutes, fixture.course("grafos").mastered.getValue("bfs").masteredAt)
    }

    @Test
    fun `a future local date is replaced by the clipped server date`() = runTest {
        // El reloj del móvil va una hora adelantado; el servidor recorta las fechas a su hora (START).
        val fixture = ProgressFixture()
        fixture.clock.time = START + 1.hours
        fixture.enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val sent = fixture.store.pending("ana")
        fixture.store.settle("ana", sent, remote = snapshot(enrollment("grafos", START, "bfs" to START)))
        assertEquals(START, fixture.course("grafos").enrollment.enrolledAt)
        assertEquals(START, fixture.course("grafos").mastered.getValue("bfs").masteredAt)
    }

    @Test
    fun `a pending item that also comes from the server stays pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        // Nada se envió (sent vacío), pero otro móvil ya lo había subido con fechas anteriores.
        val earlier = START - 5.minutes
        fixture.store.settle("ana", snapshot(), remote = snapshot(enrollment("grafos", earlier, "bfs" to earlier)))
        assertSameSnapshot(snapshot(enrollment("grafos", earlier, "bfs" to earlier)), fixture.store.pending("ana"))
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `nothing local is deleted`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos", "redes")
        fixture.store.markMastered("grafos", "bfs")
        fixture.uploadAll()
        val before = fixture.store.progressOf("ana")
        fixture.store.settle("ana", sent = snapshot(), remote = snapshot(enrollment("grafos", START)))
        assertEquals(before, fixture.store.progressOf("ana"))
    }

    @Test
    fun `whoever listens to mastered hears what was merged`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val heard = mutableListOf<Set<String>>()
        backgroundScope.launch { fixture.store.mastered("grafos").toList(heard) }
        yield() // deja que quien escucha reciba el valor actual
        fixture.store.settle("ana", snapshot(), remote = snapshot(enrollment("grafos", START, "bfs" to START)))
        yield() // y ahora el cambio
        assertEquals(listOf(emptySet(), setOf("bfs")), heard)
    }

    @Test
    fun `whoever listens to enrolled hears what was merged`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        val heard = mutableListOf<Boolean>()
        backgroundScope.launch { fixture.store.enrolled("redes").toList(heard) }
        yield()
        fixture.store.settle("ana", snapshot(), remote = snapshot(enrollment("redes", START)))
        yield()
        assertEquals(listOf(false, true), heard)
    }

    @Test
    fun `settling twice with the same leaves the same state`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val sent = fixture.store.pending("ana")
        val remote = snapshot(
            enrollment("grafos", START - 1.minutes, "bfs" to START, "dfs" to START),
            enrollment("redes", START),
        )
        fixture.store.settle("ana", sent, remote)
        val once = fixture.store.progressOf("ana")
        fixture.store.settle("ana", sent, remote)
        assertEquals(once, fixture.store.progressOf("ana"))
    }
}
