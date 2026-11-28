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
// Desde la tarea 4b hace falta sesión e inscripción antes de dominar: cada test las prepara con
// storeEnrolledIn. Lo que exige la inscripción tiene sus propios tests (MasteryRequiresEnrollmentTest).
class InMemoryProgressStoreTest {

    private suspend fun storeEnrolledIn(vararg courseIds: String) =
        ProgressFixture().enrolledIn(*courseIds).store

    @Test
    fun `at first nothing is mastered`() = runTest {
        val store = storeEnrolledIn("grafos")
        assertTrue(store.mastered("grafos").first().isEmpty())
    }

    @Test
    fun `a mastered module is remembered`() = runTest {
        val store = storeEnrolledIn("grafos")
        store.markMastered("grafos", "bfs")
        store.markMastered("grafos", "dfs")
        assertEquals(setOf("bfs", "dfs"), store.mastered("grafos").first())
    }

    @Test
    fun `mastering a module twice keeps it once`() = runTest {
        val store = storeEnrolledIn("grafos")
        store.markMastered("grafos", "bfs")
        store.markMastered("grafos", "bfs")
        assertEquals(setOf("bfs"), store.mastered("grafos").first())
    }

    @Test
    fun `each course has its own progress`() = runTest {
        val store = storeEnrolledIn("grafos")
        store.markMastered("grafos", "bfs")
        assertTrue(store.mastered("arboles").first().isEmpty())
    }

    // El camino lo escucha: al dominar un módulo, se entera sin volver a preguntar.
    @Test
    fun `who listens gets the new progress`() = runTest {
        val store = storeEnrolledIn("grafos")
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
        val store = storeEnrolledIn("grafos", "arboles")
        val seen = mutableListOf<Set<String>>()
        val listening = launch { store.mastered("grafos").toList(seen) }
        testScheduler.runCurrent()
        store.markMastered("arboles", "raiz")
        testScheduler.runCurrent()
        listening.cancel()
        assertEquals(listOf(emptySet<String>()), seen)
    }
}
