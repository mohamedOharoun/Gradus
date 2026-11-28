package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Sin sesión (B-3): leer da vacío; escribir es un fallo de programación y lanza.
class NoSessionProgressTest {

    private val fixture = ProgressFixture()
    private val store = fixture.store

    @Test
    fun `without session nothing is mastered`() = runTest {
        assertTrue(store.mastered("grafos").first().isEmpty())
    }

    @Test
    fun `without session the student is not enrolled`() = runTest {
        assertFalse(store.enrolled("grafos").first())
    }

    @Test
    fun `without session enrolling fails`() = runTest {
        assertFailsWith<IllegalStateException> { store.enroll("grafos") }
    }

    @Test
    fun `without session mastering fails`() = runTest {
        assertFailsWith<IllegalStateException> { store.markMastered("grafos", "bfs") }
    }

    @Test
    fun `failed writes without session leave nothing for the next user`() = runTest {
        runCatching { store.enroll("grafos") }
        runCatching { store.markMastered("grafos", "bfs") }
        fixture.logInAs("ana")
        assertEquals(UserProgress(), store.progressOf("ana"))
        assertFalse(store.enrolled("grafos").first())
    }

    // Sin sesión, quien escucha recibe el valor vacío y el Flow termina (no espera a una sesión futura).
    @Test
    fun `who listens without session gets empty and the flow ends`() = runTest {
        assertEquals(listOf(emptySet<String>()), store.mastered("grafos").toList())
        assertEquals(listOf(false), store.enrolled("grafos").toList())
    }
}
