package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Qué módulos ha dominado el estudiante en cada curso, mientras la app está abierta.
class InMemoryProgressStoreTest {

    private val store = InMemoryProgressStore()

    @Test
    fun `at first nothing is mastered`() = runTest {
        assertTrue(store.mastered("grafos").first().isEmpty())
    }

    @Test
    fun `a mastered module is remembered`() = runTest {
        store.markMastered("grafos", "bfs")
        store.markMastered("grafos", "dfs")
        assertEquals(setOf("bfs", "dfs"), store.mastered("grafos").first())
    }

    @Test
    fun `mastering a module twice keeps it once`() = runTest {
        store.markMastered("grafos", "bfs")
        store.markMastered("grafos", "bfs")
        assertEquals(setOf("bfs"), store.mastered("grafos").first())
    }

    @Test
    fun `each course has its own progress`() = runTest {
        store.markMastered("grafos", "bfs")
        assertTrue(store.mastered("arboles").first().isEmpty())
    }

    // El camino lo escucha: al dominar un módulo, se entera sin volver a preguntar.
    @Test
    fun `who listens gets the new progress`() = runTest {
        val seen = mutableListOf<Set<String>>()
        val listening = launch { store.mastered("grafos").take(2).toList(seen) }
        testScheduler.runCurrent()
        store.markMastered("grafos", "bfs")
        listening.join()
        assertEquals(listOf(emptySet(), setOf("bfs")), seen)
    }

    // Lo que domine en otro curso no le llega a quien escucha este.
    @Test
    fun `progress in another course is not announced`() = runTest {
        val seen = mutableListOf<Set<String>>()
        val listening = launch { store.mastered("grafos").toList(seen) }
        testScheduler.runCurrent()
        store.markMastered("arboles", "raiz")
        testScheduler.runCurrent()
        listening.cancel()
        assertEquals(listOf(emptySet<String>()), seen)
    }
}
