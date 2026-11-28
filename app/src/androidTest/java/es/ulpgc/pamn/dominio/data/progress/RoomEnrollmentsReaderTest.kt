package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours

// RoomProgressStore como EnrollmentsReader (docs/tareas/inicio.md, paso 2): lo mismo que
// EnrollmentsReaderTest (en sharedLogic) pide al almacén en memoria, ahora con las tablas de Room.
// Sin tiempos: cada valor del Flow se espera con receive().
@RunWith(AndroidJUnit4::class)
class RoomEnrollmentsReaderTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    private fun List<EnrollmentSnapshot>.byCourse() = sortedBy { it.enrollment.courseId }

    @Test
    fun with_no_enrollments_the_list_is_empty() = runTest {
        progress.logInAs(ANA)
        assertTrue(store.enrollments().first().isEmpty())
    }

    @Test
    fun every_enrollment_comes_with_its_dates_and_mastered_modules() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        progress.clock.time = START + 2.hours
        store.markMastered(GRAPHS, "bfs")

        assertEquals(
            listOf(enrollment(GRAPHS, START, "bfs" to START + 2.hours), enrollment(TREES, START)),
            store.enrollments().first().byCourse(),
        )
    }

    @Test
    fun only_the_enrollments_of_the_session_user_are_read() = runTest {
        progress.logInAs(BETO)
        store.enroll(TREES)
        progress.logInAs(ANA)
        store.enroll(GRAPHS)

        assertEquals(listOf(GRAPHS), store.enrollments().first().map { it.enrollment.courseId })
    }

    // Lo que trae la sincronización del servidor (sin pendiente) también se lee.
    @Test
    fun what_the_server_sent_is_read_too() = runTest {
        progress.logInAs(ANA)
        val fromServer = enrollment(GRAPHS, START, "bfs" to START + 1.hours)
        store.settle(ANA, snapshot(), remote = snapshot(fromServer))

        assertEquals(listOf(fromServer), store.enrollments().first())
    }

    @Test
    fun who_listens_gets_each_newly_mastered_module() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val seen = listenTo(store.enrollments())
        assertEquals(listOf(enrollment(GRAPHS, START)), seen.receive())

        store.markMastered(GRAPHS, "bfs")

        assertEquals(listOf(enrollment(GRAPHS, START, "bfs" to START)), seen.receive())
    }

    @Test
    fun who_listens_gets_a_new_enrollment() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val seen = listenTo(store.enrollments())
        assertEquals(1, seen.receive().size)

        store.enroll(TREES)

        assertEquals(listOf(GRAPHS, TREES), seen.receive().byCourse().map { it.enrollment.courseId })
    }

    @Test
    fun without_a_session_the_list_is_empty_and_the_flow_ends() = runTest {
        assertEquals(listOf(emptyList<EnrollmentSnapshot>()), store.enrollments().toList())
    }
}
