package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes

// Dominar módulos con RoomProgressStore: igual que InMemoryProgressStoreTest y ProgressDatesTest,
// más la marca pending (solo al insertar). Cada test inscribe antes (lo que exige la inscripción
// está en RoomMasteryRequiresEnrollmentTest).
@RunWith(AndroidJUnit4::class)
class RoomMasteryTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    private suspend fun masteredRow(moduleId: String) =
        progress.dao.masteredModules(ANA, GRAPHS).single { it.moduleId == moduleId }

    @Test
    fun at_first_nothing_is_mastered() = runTest {
        progress.enrolledAsAna(GRAPHS)

        assertTrue(store.mastered(GRAPHS).first().isEmpty())
    }

    @Test
    fun a_mastered_module_is_remembered() = runTest {
        progress.enrolledAsAna(GRAPHS)

        store.markMastered(GRAPHS, "bfs")
        store.markMastered(GRAPHS, "dfs")

        assertEquals(setOf("bfs", "dfs"), store.mastered(GRAPHS).first())
    }

    @Test
    fun mastering_a_module_twice_keeps_it_once() = runTest {
        progress.enrolledAsAna(GRAPHS)

        store.markMastered(GRAPHS, "bfs")
        store.markMastered(GRAPHS, "bfs")

        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
        assertEquals(1, progress.rowCount("mastered_modules"))
    }

    @Test
    fun each_course_has_its_own_progress() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)

        store.markMastered(GRAPHS, "bfs")

        assertTrue(store.mastered(TREES).first().isEmpty())
    }

    @Test
    fun the_mastered_date_comes_from_the_clock() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 10.minutes

        store.markMastered(GRAPHS, "bfs")

        assertEquals(START + 10.minutes, masteredRow("bfs").masteredAt)
    }

    @Test
    fun each_mastered_module_has_its_own_date() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 1.minutes
        store.markMastered(GRAPHS, "bfs")
        progress.clock.time = START + 2.minutes
        store.markMastered(GRAPHS, "dfs")

        assertEquals(START + 1.minutes, masteredRow("bfs").masteredAt)
        assertEquals(START + 2.minutes, masteredRow("dfs").masteredAt)
    }

    @Test
    fun mastering_twice_keeps_the_first_date() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 1.minutes
        store.markMastered(GRAPHS, "bfs")
        progress.clock.time = START + 30.minutes

        store.markMastered(GRAPHS, "bfs")

        assertEquals(START + 1.minutes, masteredRow("bfs").masteredAt)
    }

    @Test
    fun a_new_mastered_module_is_pending() = runTest {
        progress.enrolledAsAna(GRAPHS)

        store.markMastered(GRAPHS, "bfs")

        assertTrue(masteredRow("bfs").pending)
    }

    // Un módulo ya subido (pending = false) no vuelve a la cola por dominarlo otra vez.
    @Test
    fun mastering_again_keeps_a_settled_module_as_it_was() = runTest {
        progress.logInAs(ANA)
        progress.dao.insertEnrollment(EnrollmentEntity(ANA, GRAPHS, enrolledAt = START, pending = false))
        val settled = MasteredModuleEntity(ANA, GRAPHS, "bfs", masteredAt = START, pending = false)
        progress.dao.insertMasteredModule(settled)
        progress.clock.time = START + 1.minutes

        store.markMastered(GRAPHS, "bfs")

        assertEquals(listOf(settled), progress.dao.masteredModules(ANA, GRAPHS))
    }

    // Dominar solo escribe en mastered_modules: la inscripción (ya subida) no vuelve a la cola.
    @Test
    fun mastering_does_not_change_the_enrollment_row() = runTest {
        progress.logInAs(ANA)
        val settled = EnrollmentEntity(ANA, GRAPHS, enrolledAt = START, pending = false)
        progress.dao.insertEnrollment(settled)
        progress.clock.time = START + 1.minutes

        store.markMastered(GRAPHS, "bfs")

        assertEquals(settled, progress.dao.enrollment(ANA, GRAPHS))
    }
}
