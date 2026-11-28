package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.ProgressGateway
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ProgressModule (Facade): un solo RoomProgressStore con tres caras. Se prueba con la base en
// memoria de RoomProgressRule, nunca con la real.
@RunWith(AndroidJUnit4::class)
class ProgressModuleTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val module get() = moduleWith(FakeProgressGateway())

    private fun moduleWith(gateway: ProgressGateway) = ProgressModule(progress.database, progress.sessions, gateway)

    @Test
    fun the_three_ports_are_the_same_object() {
        val module = module

        assertSame(module.progressStore, module.enrollmentStore)
        assertSame(module.enrollmentStore, module.syncStore)
    }

    // Inicio (docs/tareas/inicio.md, paso 4d): el lector de inscripciones es el mismo almacén.
    @Test
    fun the_enrollments_reader_is_the_same_object() {
        val module = module

        assertSame(module.enrollmentStore, module.enrollmentsReader)
    }

    @Test
    fun what_is_enrolled_and_mastered_is_seen_by_the_enrollments_reader() = runTest {
        val module = module
        progress.logInAs(ANA)
        module.enrollmentStore.enroll(GRAPHS)
        module.progressStore.markMastered(GRAPHS, "bfs")

        val enrollment = module.enrollmentsReader.enrollments().first().single()

        assertEquals(GRAPHS, enrollment.enrollment.courseId)
        assertEquals(listOf("bfs"), enrollment.mastered.map { it.moduleId })
    }

    @Test
    fun what_is_enrolled_through_the_enrollment_port_is_seen_by_the_progress_port() = runTest {
        val module = module
        progress.logInAs(ANA)

        module.enrollmentStore.enroll(GRAPHS)

        // markMastered no lanza: ve la inscripción hecha por el otro puerto.
        module.progressStore.markMastered(GRAPHS, "bfs")
        assertEquals(setOf("bfs"), module.progressStore.mastered(GRAPHS).first())
    }

    @Test
    fun what_is_mastered_appears_pending_in_the_sync_port() = runTest {
        val module = module
        progress.logInAs(ANA)
        module.enrollmentStore.enroll(GRAPHS)
        module.progressStore.markMastered(GRAPHS, "bfs")

        val pending = module.syncStore.pending(ANA)

        val enrollment = pending.enrollments.single()
        assertEquals(GRAPHS, enrollment.enrollment.courseId)
        assertEquals(listOf("bfs"), enrollment.mastered.map { it.moduleId })
    }

    // Comprueba que el puerto de progreso es de verdad el almacén Room y no uno vacío.
    @Test
    fun mastering_without_enrolling_fails_with_the_port_failure() = runTest {
        val module = module
        progress.logInAs(ANA)

        val failure = failureOf { module.progressStore.markMastered(GRAPHS, "bfs") }

        assertIsThePortFailure(failure)
    }

    // La sesión inyectada llega al almacén: lo de ana no lo ve beto.
    @Test
    fun the_injected_session_decides_whose_progress_is_seen() = runTest {
        val module = module
        progress.logInAs(ANA)
        module.enrollmentStore.enroll(GRAPHS)
        assertTrue(module.enrollmentStore.enrolled(GRAPHS).first())

        progress.logInAs(BETO)

        assertFalse(module.enrollmentStore.enrolled(GRAPHS).first())
    }

    // 8a: el ProgressSync del módulo trabaja sobre el MISMO almacén Room. Lo que llega del servidor
    // se ve por los otros puertos, y lo enviado sale de la cola.
    @Test
    fun the_sync_of_the_module_merges_into_the_same_room_store() = runTest {
        val remote = synced(snapshot(enrollment(TREES, START, "root" to START)))
        val module = moduleWith(FakeProgressGateway { _, _ -> remote })
        progress.logInAs(ANA)
        module.enrollmentStore.enroll(GRAPHS)
        module.progressStore.markMastered(GRAPHS, "bfs")

        val outcome = module.sync.sync(sessionOf(ANA))

        assertEquals(remote, outcome)
        assertTrue(module.enrollmentStore.enrolled(TREES).first())
        assertEquals(setOf("root"), module.progressStore.mastered(TREES).first())
        assertTrue(module.syncStore.pending(ANA).isEmpty)
    }
}
