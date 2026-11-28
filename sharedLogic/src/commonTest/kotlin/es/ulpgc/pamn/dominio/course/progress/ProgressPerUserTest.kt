package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// El progreso es de cada usuario: la clave es el id de la cuenta de la sesión.
class ProgressPerUserTest {

    private suspend fun anaWithProgress(): ProgressFixture =
        ProgressFixture().enrolledIn("grafos").also { it.store.markMastered("grafos", "bfs") }

    @Test
    fun `another user does not see the enrollment`() = runTest {
        val fixture = anaWithProgress()
        fixture.logInAs("beto")
        assertFalse(fixture.store.enrolled("grafos").first())
    }

    @Test
    fun `another user does not see what was mastered`() = runTest {
        val fixture = anaWithProgress()
        fixture.logInAs("beto")
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
    }

    @Test
    fun `the first user finds the progress again`() = runTest {
        val fixture = anaWithProgress()
        fixture.logInAs("beto")
        fixture.logInAs("ana")
        assertTrue(fixture.store.enrolled("grafos").first())
        assertEquals(setOf("bfs"), fixture.store.mastered("grafos").first())
    }

    @Test
    fun `after logging out nothing is visible`() = runTest {
        val fixture = anaWithProgress()
        fixture.sessions.clear()
        assertFalse(fixture.store.enrolled("grafos").first())
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
    }

    @Test
    fun `what one user writes does not touch another`() = runTest {
        val fixture = anaWithProgress()
        val before = fixture.store.progressOf("ana")
        fixture.logInAs("beto")
        fixture.store.enroll("grafos")
        fixture.store.markMastered("grafos", "dfs")
        assertEquals(before, fixture.store.progressOf("ana"))
        assertEquals(setOf("dfs"), fixture.store.progressOf("beto").courses.getValue("grafos").mastered.keys)
    }

    // La sesión se lee al empezar a escuchar: si luego entra otro usuario, quien ya escuchaba
    // sigue con el suyo y no recibe lo del nuevo.
    @Test
    fun `who listens keeps the user it started with`() = runTest {
        val fixture = anaWithProgress()
        val seen = mutableListOf<Set<String>>()
        val listening = launch { fixture.store.mastered("grafos").toList(seen) }
        testScheduler.runCurrent()
        fixture.logInAs("beto")
        fixture.store.enroll("grafos")
        fixture.store.markMastered("grafos", "dfs")
        testScheduler.runCurrent()
        listening.cancel()
        assertEquals(listOf(setOf("bfs")), seen)
    }
}
