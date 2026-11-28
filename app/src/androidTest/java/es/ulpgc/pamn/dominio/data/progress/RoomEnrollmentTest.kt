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
import kotlin.time.Duration.Companion.minutes

// Inscribirse con RoomProgressStore: igual que EnrollmentStoreTest y ProgressDatesTest, más la
// marca pending (solo al insertar).
@RunWith(AndroidJUnit4::class)
class RoomEnrollmentTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun at_first_the_student_is_not_enrolled() = runTest {
        progress.logInAs(ANA)

        assertFalse(store.enrolled(GRAPHS).first())
    }

    @Test
    fun enrolling_is_remembered() = runTest {
        progress.enrolledAsAna(GRAPHS)

        assertTrue(store.enrolled(GRAPHS).first())
    }

    @Test
    fun each_course_has_its_own_enrollment() = runTest {
        progress.enrolledAsAna(GRAPHS)

        assertFalse(store.enrolled(TREES).first())
    }

    @Test
    fun enrolling_twice_gives_no_error_and_keeps_a_single_row() = runTest {
        progress.enrolledAsAna(GRAPHS)

        store.enroll(GRAPHS)

        assertTrue(store.enrolled(GRAPHS).first())
        assertEquals(1, progress.rowCount("enrollments"))
    }

    @Test
    fun the_enrollment_date_comes_from_the_clock() = runTest {
        progress.clock.time = START + 5.minutes

        progress.enrolledAsAna(GRAPHS)

        assertEquals(START + 5.minutes, progress.dao.enrollment(ANA, GRAPHS)?.enrolledAt)
    }

    @Test
    fun enrolling_twice_keeps_the_first_date() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 30.minutes

        store.enroll(GRAPHS)

        assertEquals(START, progress.dao.enrollment(ANA, GRAPHS)?.enrolledAt)
    }

    @Test
    fun enrolling_twice_keeps_what_was_already_mastered() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")

        store.enroll(GRAPHS)

        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
    }

    @Test
    fun a_new_enrollment_is_pending() = runTest {
        progress.enrolledAsAna(GRAPHS)

        assertEquals(true, progress.dao.enrollment(ANA, GRAPHS)?.pending)
    }

    // Una inscripción ya subida (pending = false) no vuelve a la cola por inscribirse otra vez.
    @Test
    fun enrolling_again_keeps_a_settled_enrollment_as_it_was() = runTest {
        progress.logInAs(ANA)
        val settled = EnrollmentEntity(ANA, GRAPHS, enrolledAt = START, pending = false)
        progress.dao.insertEnrollment(settled)
        progress.clock.time = START + 1.minutes

        store.enroll(GRAPHS)

        assertEquals(settled, progress.dao.enrollment(ANA, GRAPHS))
    }
}
