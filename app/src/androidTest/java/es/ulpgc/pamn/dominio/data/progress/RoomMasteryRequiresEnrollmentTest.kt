package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// No hay módulo dominado sin inscripción (como MasteryRequiresEnrollmentTest): RoomProgressStore lo
// comprueba antes de escribir y lanza la IllegalStateException del puerto, no la
// SQLiteConstraintException de la clave foránea, y no deja filas.
@RunWith(AndroidJUnit4::class)
class RoomMasteryRequiresEnrollmentTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun mastering_without_enrollment_fails_with_the_port_exception() = runTest {
        progress.logInAs(ANA)

        assertIsThePortFailure(failureOf { store.markMastered(GRAPHS, "bfs") })
    }

    @Test
    fun a_failed_mastery_leaves_no_rows() = runTest {
        progress.logInAs(ANA)

        failureOf { store.markMastered(GRAPHS, "bfs") }

        assertEquals(0, progress.rowCount("mastered_modules"))
        assertEquals(0, progress.rowCount("enrollments"))
        assertTrue(store.mastered(GRAPHS).first().isEmpty())
        assertFalse(store.enrolled(GRAPHS).first())
    }

    @Test
    fun a_failed_mastery_does_not_touch_the_courses_already_enrolled() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val enrollmentBefore = progress.dao.enrollment(ANA, GRAPHS)
        val masteredBefore = progress.dao.masteredModules(ANA, GRAPHS)

        failureOf { store.markMastered(TREES, "root") }

        assertEquals(enrollmentBefore, progress.dao.enrollment(ANA, GRAPHS))
        assertEquals(masteredBefore, progress.dao.masteredModules(ANA, GRAPHS))
        assertEquals(1, progress.rowCount("enrollments"))
        assertEquals(1, progress.rowCount("mastered_modules"))
    }

    // El mensaje ayuda a encontrar el fallo: dice el curso, y nunca el token de la sesión.
    @Test
    fun the_failure_names_the_course_and_never_the_token() = runTest {
        progress.logInAs(ANA)

        val message = failureOf { store.markMastered(GRAPHS, "bfs") }?.message.orEmpty()

        assertTrue(message, GRAPHS in message)
        assertFalse(message, sessionOf(ANA).token in message)
    }

    @Test
    fun the_enrollment_of_another_user_does_not_count() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.logInAs(BETO)

        assertIsThePortFailure(failureOf { store.markMastered(GRAPHS, "bfs") })
        assertEquals(0, progress.rowCount("mastered_modules"))
    }

    @Test
    fun enrolling_in_another_course_does_not_allow_mastering_this_one() = runTest {
        progress.enrolledAsAna(TREES)

        assertIsThePortFailure(failureOf { store.markMastered(GRAPHS, "bfs") })
        assertEquals(0, progress.rowCount("mastered_modules"))
    }

    @Test
    fun mastering_after_enrolling_works() = runTest {
        progress.logInAs(ANA)
        failureOf { store.markMastered(GRAPHS, "bfs") }

        store.enroll(GRAPHS)
        store.markMastered(GRAPHS, "bfs")

        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
    }
}
