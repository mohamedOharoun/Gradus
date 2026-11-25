package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

// ProgressSync tras un 200 (Synced): lo enviado sale de la cola y lo del servidor se fusiona.
class ProgressSyncSettleTest {

    // El servidor guarda lo que recibe y no tiene nada más.
    private val echo = FakeProgressGateway(answer = { _, sent -> synced(remote = sent) })

    @Test
    fun `after synced what was sent leaves the queue`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos", "redes")
        fixture.store.markMastered("grafos", "bfs")

        fixture.syncWith(echo).sync(ANA)

        assertTrue(fixture.store.pending("ana").isEmpty)
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `what was mastered on another phone appears`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.uploadAll()
        val remote = snapshot(enrollment("grafos", START, "bfs" to START + 5.minutes))
        val gateway = FakeProgressGateway(answer = { _, _ -> synced(remote) })

        fixture.syncWith(gateway).sync(ANA)

        val grafos = fixture.store.progressOf("ana").courses.getValue("grafos")
        assertEquals(START + 5.minutes, grafos.mastered.getValue("bfs").masteredAt)
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `whoever listens to mastered hears what was mastered on another phone`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val heard = mutableListOf<Set<String>>()
        backgroundScope.launch { fixture.store.mastered("grafos").toList(heard) }
        yield() // quien escucha recibe el valor actual
        val gateway = FakeProgressGateway(answer = { _, _ -> synced(snapshot(enrollment("grafos", START, "bfs" to START))) })

        fixture.syncWith(gateway).sync(ANA)
        yield() // y ahora el cambio

        assertEquals(listOf(emptySet(), setOf("bfs")), heard)
    }

    // B-8: lo que el servidor omite sale de la cola y el móvil lo conserva.
    @Test
    fun `a whole enrollment skipped by the server leaves the queue and is kept`() = runTest {
        val fixture = ProgressFixture().enrolledIn("fantasma")
        fixture.store.markMastered("fantasma", "m1")
        val gateway = FakeProgressGateway(answer = { _, _ ->
            synced(snapshot(), SkippedProgress.WholeEnrollment("fantasma", "COURSE_NOT_FOUND"))
        })

        fixture.syncWith(gateway).sync(ANA)

        assertTrue(fixture.store.pending("ana").isEmpty)
        assertTrue(fixture.store.enrolled("fantasma").first())
        assertEquals(setOf("m1"), fixture.store.mastered("fantasma").first())
    }

    @Test
    fun `a single module skipped by the server leaves the queue and is kept`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        fixture.store.markMastered("grafos", "raro")
        val gateway = FakeProgressGateway(answer = { _, _ ->
            synced(snapshot(enrollment("grafos", START, "bfs" to START)), SkippedProgress.SingleModule("grafos", "raro", "MODULE_NOT_FOUND"))
        })

        fixture.syncWith(gateway).sync(ANA)

        assertTrue(fixture.store.pending("ana").isEmpty)
        assertEquals(setOf("bfs", "raro"), fixture.store.mastered("grafos").first())
    }

    // settle recibe el sent que de verdad se envió, no lo pendiente al volver la respuesta.
    @Test
    fun `what is marked while the request travels stays pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val gateway = FakeProgressGateway(answer = { _, sent ->
            fixture.clock.time = START + 1.minutes
            fixture.store.markMastered("grafos", "dfs") // el estudiante sigue estudiando
            synced(remote = sent)
        })

        fixture.syncWith(gateway).sync(ANA)

        assertSameSnapshot(snapshot(enrollment("grafos", START, "dfs" to START + 1.minutes)), fixture.store.pending("ana"))
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `a 200 without changes leaves the state the same`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        fixture.uploadAll()
        val before = fixture.store.progressOf("ana")
        val gateway = FakeProgressGateway(answer = { _, _ -> synced(snapshot(enrollment("grafos", START, "bfs" to START))) })

        fixture.syncWith(gateway).sync(ANA)

        assertEquals(before, fixture.store.progressOf("ana"))
    }
}
