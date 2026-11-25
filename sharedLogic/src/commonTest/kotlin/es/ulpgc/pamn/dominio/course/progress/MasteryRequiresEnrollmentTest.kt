package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// No hay módulo dominado sin inscripción: dominar en un curso sin inscribirse es un fallo de
// programación (lanza IllegalStateException) y no deja nada escrito.
class MasteryRequiresEnrollmentTest {

    @Test
    fun `mastering without enrollment fails`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        assertFailsWith<IllegalStateException> { store.markMastered("grafos", "bfs") }
    }

    @Test
    fun `a failed mastery leaves no rows`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        runCatching { store.markMastered("grafos", "bfs") }
        assertEquals(UserProgress(), store.progressOf("ana"))
        assertTrue(store.mastered("grafos").first().isEmpty())
        assertFalse(store.enrolled("grafos").first())
    }

    @Test
    fun `a failed mastery does not touch the courses already enrolled`() = runTest {
        val store = ProgressFixture().enrolledIn("grafos").store
        store.markMastered("grafos", "bfs")
        val before = store.progressOf("ana")
        runCatching { store.markMastered("arboles", "raiz") }
        assertEquals(before, store.progressOf("ana"))
    }

    // El mensaje ayuda a encontrar el fallo: dice el curso, y nunca el token de la sesión.
    @Test
    fun `the failure names the course and never the token`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        val failure = assertFailsWith<IllegalStateException> { store.markMastered("grafos", "bfs") }
        val message = failure.message.orEmpty()
        assertTrue("grafos" in message)
        assertFalse(sessionOf("ana").token in message)
    }

    @Test
    fun `the enrollment of another user does not count`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.logInAs("beto")
        assertFailsWith<IllegalStateException> { fixture.store.markMastered("grafos", "bfs") }
    }

    @Test
    fun `enrolling in another course does not allow mastering this one`() = runTest {
        val store = ProgressFixture().enrolledIn("arboles").store
        assertFailsWith<IllegalStateException> { store.markMastered("grafos", "bfs") }
    }

    @Test
    fun `mastering after enrolling works`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        runCatching { store.markMastered("grafos", "bfs") }
        store.enroll("grafos")
        store.markMastered("grafos", "bfs")
        assertEquals(setOf("bfs"), store.mastered("grafos").first())
    }
}
