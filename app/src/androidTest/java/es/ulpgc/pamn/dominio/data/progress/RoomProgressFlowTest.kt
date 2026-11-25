package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Los Flow de RoomProgressStore avisan de lo suyo y de nada más. Room vuelve a consultar con
// cualquier cambio en la tabla; distinctUntilChanged no deja pasar un valor repetido.
// Sin tiempos: cada valor se espera con receive(). Para «no avisa», se cambia otra cosa, después lo
// propio, y se comprueba que lo siguiente que llega es lo propio (no un valor repetido). La barrera
// (un segundo oyente de lo otro) asegura que Room ya ha procesado el primer cambio antes del segundo.
@RunWith(AndroidJUnit4::class)
class RoomProgressFlowTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun who_listens_gets_the_new_enrollment() = runTest {
        progress.logInAs(ANA)
        val seen = listenTo(store.enrolled(GRAPHS))
        assertEquals(false, seen.receive())

        store.enroll(GRAPHS)

        assertEquals(true, seen.receive())
    }

    @Test
    fun who_listens_gets_the_new_mastered_module() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val seen = listenTo(store.mastered(GRAPHS))
        assertEquals(emptySet<String>(), seen.receive())

        store.markMastered(GRAPHS, "bfs")

        assertEquals(setOf("bfs"), seen.receive())
    }

    @Test
    fun progress_in_another_course_is_not_announced() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        val seen = listenTo(store.mastered(GRAPHS))
        val barrier = listenTo(store.mastered(TREES))
        assertEquals(emptySet<String>(), seen.receive())
        assertEquals(emptySet<String>(), barrier.receive())

        store.markMastered(TREES, "root")
        assertEquals(setOf("root"), barrier.receive())
        store.markMastered(GRAPHS, "bfs")

        assertEquals(setOf("bfs"), seen.receive())
    }

    @Test
    fun enrolling_in_another_course_is_not_announced() = runTest {
        progress.logInAs(ANA)
        val seen = listenTo(store.enrolled(GRAPHS))
        val barrier = listenTo(store.enrolled(TREES))
        assertEquals(false, seen.receive())
        assertEquals(false, barrier.receive())

        store.enroll(TREES)
        assertEquals(true, barrier.receive())
        store.enroll(GRAPHS)

        assertEquals(true, seen.receive())
    }

    // Lo de otro usuario en el mismo curso tampoco llega. La barrera escucha a Beto (se abre con su
    // sesión) y sus filas se escriben con el DAO, con Ana otra vez dentro.
    @Test
    fun progress_of_another_user_in_the_same_course_is_not_announced() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val seen = listenTo(store.mastered(GRAPHS))
        assertEquals(emptySet<String>(), seen.receive())
        progress.logInAs(BETO)
        val barrier = listenTo(store.mastered(GRAPHS))
        assertEquals(emptySet<String>(), barrier.receive())
        progress.logInAs(ANA)

        progress.dao.insertEnrollment(EnrollmentEntity(BETO, GRAPHS, START, pending = true))
        progress.dao.insertMasteredModule(MasteredModuleEntity(BETO, GRAPHS, "dijkstra", START, pending = true))
        assertEquals(setOf("dijkstra"), barrier.receive())
        store.markMastered(GRAPHS, "bfs")

        assertEquals(setOf("bfs"), seen.receive())
    }
}
