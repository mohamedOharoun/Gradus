package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Inscribirse en un curso y saber si se está inscrito.
class EnrollmentStoreTest {

    @Test
    fun `at first the student is not enrolled`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        assertFalse(store.enrolled("grafos").first())
    }

    @Test
    fun `enrolling is remembered`() = runTest {
        val store = ProgressFixture().enrolledIn("grafos").store
        assertTrue(store.enrolled("grafos").first())
    }

    @Test
    fun `each course has its own enrollment`() = runTest {
        val store = ProgressFixture().enrolledIn("grafos").store
        assertFalse(store.enrolled("arboles").first())
    }

    @Test
    fun `enrolling twice gives no error`() = runTest {
        val store = ProgressFixture().enrolledIn("grafos").store
        store.enroll("grafos")
        assertTrue(store.enrolled("grafos").first())
    }

    // Quien escucha (el botón de inscribirse) se entera sin volver a preguntar.
    @Test
    fun `who listens gets the new enrollment`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        val seen = mutableListOf<Boolean>()
        val listening = launch { store.enrolled("grafos").take(2).toList(seen) }
        testScheduler.runCurrent()
        store.enroll("grafos")
        listening.join()
        assertEquals(listOf(false, true), seen)
    }

    // Lo que se inscriba en otro curso no le llega a quien escucha este.
    @Test
    fun `enrolling in another course is not announced`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        val seen = mutableListOf<Boolean>()
        val listening = launch { store.enrolled("grafos").toList(seen) }
        testScheduler.runCurrent()
        store.enroll("arboles")
        testScheduler.runCurrent()
        listening.cancel()
        assertEquals(listOf(false), seen)
    }
}
