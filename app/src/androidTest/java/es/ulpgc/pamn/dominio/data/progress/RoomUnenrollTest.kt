package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.data.course.CourseCopyEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes

// Baja local con RoomProgressStore (B4): borra inscripción y módulos dominados en @Transaction,
// aísla otros cursos/usuarios/copias, es idempotente, actúa sobre userId explícito (sin leer la
// sesión), limpia pending, permite reinscripción limpia y bloquea markMastered tras la baja.
@RunWith(AndroidJUnit4::class)
class RoomUnenrollTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun unenrolling_deletes_enrollment_and_all_mastered_modules_pending_and_synced() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        progress.uploadAll(ANA)
        progress.clock.time = START + 5.minutes
        store.markMastered(GRAPHS, "dfs")

        store.unenroll(ANA, GRAPHS)

        assertNull(progress.dao.enrollment(ANA, GRAPHS))
        assertTrue(progress.dao.masteredModules(ANA, GRAPHS).isEmpty())
        assertEquals(0, progress.rowCount("enrollments"))
        assertEquals(0, progress.rowCount("mastered_modules"))
        assertFalse(store.enrolled(GRAPHS).first())
        assertTrue(store.mastered(GRAPHS).first().isEmpty())
    }

    @Test
    fun unenrolling_does_not_affect_other_courses_of_the_same_user() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        store.markMastered(GRAPHS, "bfs")
        store.markMastered(TREES, "root")
        val treesEnrollment = progress.dao.enrollment(ANA, TREES)
        val treesMastered = progress.dao.masteredModules(ANA, TREES)

        store.unenroll(ANA, GRAPHS)

        assertEquals(treesEnrollment, progress.dao.enrollment(ANA, TREES))
        assertEquals(treesMastered, progress.dao.masteredModules(ANA, TREES))
        assertTrue(store.enrolled(TREES).first())
        assertEquals(setOf("root"), store.mastered(TREES).first())
    }

    @Test
    fun unenrolling_does_not_affect_the_same_course_of_another_user() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        progress.logInAs(BETO)
        store.enroll(GRAPHS)
        store.markMastered(GRAPHS, "dfs")
        val betoBefore = progress.allRows(BETO)

        store.unenroll(ANA, GRAPHS)

        assertEquals(betoBefore, progress.allRows(BETO))
        assertTrue(store.enrolled(GRAPHS).first())
        assertEquals(setOf("dfs"), store.mastered(GRAPHS).first())
    }

    // Las copias locales de cursos son globales al dispositivo (Decisión 5): la baja no las borra.
    @Test
    fun unenrolling_does_not_delete_course_copies() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val copyDao = progress.database.courseCopyDao()
        copyDao.save(CourseCopyEntity(GRAPHS, """{"id":"$GRAPHS"}"""))

        store.unenroll(ANA, GRAPHS)

        assertEquals("""{"id":"$GRAPHS"}""", copyDao.loadJson(GRAPHS))
    }

    @Test
    fun unenrolling_when_not_enrolled_is_idempotent_and_changes_nothing() = runTest {
        progress.enrolledAsAna(TREES)
        store.markMastered(TREES, "root")
        val before = progress.allRows()

        store.unenroll("desconocido", GRAPHS)
        store.unenroll(ANA, GRAPHS)

        assertEquals(before, progress.allRows())
    }

    @Test
    fun unenrolling_twice_is_idempotent() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")

        store.unenroll(ANA, GRAPHS)
        store.unenroll(ANA, GRAPHS)

        assertEquals(0, progress.rowCount("enrollments"))
        assertEquals(0, progress.rowCount("mastered_modules"))
    }

    @Test
    fun unenrolling_works_without_active_session() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        progress.sessions.clear()

        store.unenroll(ANA, GRAPHS)

        assertEquals(0, progress.rowCount("enrollments"))
        assertEquals(0, progress.rowCount("mastered_modules"))
        assertTrue(store.pending(ANA).isEmpty)
    }

    @Test
    fun unenrolling_acts_on_given_userId_even_when_another_user_is_logged_in() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        progress.logInAs(BETO)
        store.enroll(GRAPHS)
        store.markMastered(GRAPHS, "dfs")

        store.unenroll(ANA, GRAPHS)

        assertNull(progress.dao.enrollment(ANA, GRAPHS))
        assertTrue(progress.dao.masteredModules(ANA, GRAPHS).isEmpty())
        assertTrue(store.enrolled(GRAPHS).first())
        assertEquals(setOf("dfs"), store.mastered(GRAPHS).first())
    }

    @Test
    fun pending_excludes_unenrolled_course_and_its_pending_modules() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        store.markMastered(GRAPHS, "bfs")
        store.markMastered(TREES, "root")

        store.unenroll(ANA, GRAPHS)

        assertSameSnapshot(
            snapshot(enrollment(TREES, START, "root" to START)),
            store.pending(ANA),
        )
    }

    @Test
    fun re_enrolling_after_unenroll_starts_clean_with_new_date_pending_and_no_mastered() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        progress.uploadAll(ANA)
        store.unenroll(ANA, GRAPHS)

        val later = START + 10.minutes
        progress.clock.time = later
        store.enroll(GRAPHS)

        assertEquals(EnrollmentEntity(ANA, GRAPHS, enrolledAt = later, pending = true), progress.dao.enrollment(ANA, GRAPHS))
        assertTrue(progress.dao.masteredModules(ANA, GRAPHS).isEmpty())
        assertTrue(store.enrolled(GRAPHS).first())
        assertTrue(store.mastered(GRAPHS).first().isEmpty())
        assertSameSnapshot(snapshot(enrollment(GRAPHS, later)), store.pending(ANA))
    }

    @Test
    fun mastering_after_unenroll_throws_port_exception_and_leaves_no_rows() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        store.unenroll(ANA, GRAPHS)

        val failure = failureOf { store.markMastered(GRAPHS, "dfs") }

        assertIsThePortFailure(failure)
        val message = failure?.message.orEmpty()
        assertTrue(message, GRAPHS in message)
        assertFalse(message, sessionOf(ANA).token in message)
        assertEquals(0, progress.rowCount("enrollments"))
        assertEquals(0, progress.rowCount("mastered_modules"))
    }

    @Test
    fun markMasteredIfEnrolled_returns_false_without_enrollment_and_true_when_enrolled() = runTest {
        val firstModule = MasteredModuleEntity(ANA, GRAPHS, "bfs", START, pending = true)

        assertFalse(progress.dao.markMasteredIfEnrolled(firstModule))
        assertEquals(0, progress.rowCount("mastered_modules"))

        progress.dao.insertEnrollment(EnrollmentEntity(ANA, GRAPHS, START, pending = true))

        assertTrue(progress.dao.markMasteredIfEnrolled(firstModule))
        assertTrue(progress.dao.markMasteredIfEnrolled(firstModule.copy(masteredAt = START + 5.minutes)))
        assertEquals(listOf(firstModule), progress.dao.masteredModules(ANA, GRAPHS))
    }
}
