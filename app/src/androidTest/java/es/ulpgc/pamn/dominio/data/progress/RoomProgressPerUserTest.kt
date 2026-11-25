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

// El progreso es de cada usuario (como ProgressPerUserTest): la clave es el id de la cuenta de la
// sesión, y la sesión se lee al empezar a escuchar.
@RunWith(AndroidJUnit4::class)
class RoomProgressPerUserTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    // Ana inscrita en grafos y con «bfs» dominado.
    private suspend fun anaWithProgress() {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
    }

    @Test
    fun another_user_does_not_see_the_enrollment() = runTest {
        anaWithProgress()
        progress.logInAs(BETO)

        assertFalse(store.enrolled(GRAPHS).first())
    }

    @Test
    fun another_user_does_not_see_what_was_mastered() = runTest {
        anaWithProgress()
        progress.logInAs(BETO)

        assertTrue(store.mastered(GRAPHS).first().isEmpty())
    }

    @Test
    fun the_first_user_finds_the_progress_again() = runTest {
        anaWithProgress()
        progress.logInAs(BETO)
        progress.logInAs(ANA)

        assertTrue(store.enrolled(GRAPHS).first())
        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
    }

    @Test
    fun after_logging_out_nothing_is_visible() = runTest {
        anaWithProgress()
        progress.sessions.clear()

        assertFalse(store.enrolled(GRAPHS).first())
        assertTrue(store.mastered(GRAPHS).first().isEmpty())
    }

    @Test
    fun what_one_user_writes_does_not_touch_another() = runTest {
        anaWithProgress()
        val anaEnrollment = progress.dao.enrollment(ANA, GRAPHS)
        val anaMastered = progress.dao.masteredModules(ANA, GRAPHS)
        progress.logInAs(BETO)

        store.enroll(GRAPHS)
        store.markMastered(GRAPHS, "dfs")

        assertEquals(anaEnrollment, progress.dao.enrollment(ANA, GRAPHS))
        assertEquals(anaMastered, progress.dao.masteredModules(ANA, GRAPHS))
        assertEquals(listOf("dfs"), progress.dao.masteredModules(BETO, GRAPHS).map { it.moduleId })
    }

    // Quien ya escucha sigue con el usuario con el que empezó. Lo de Ana se escribe con el DAO
    // mientras Beto sigue dentro: si el Flow hubiera cambiado de usuario, el segundo valor sería
    // {dijkstra} (lo de Beto) y nunca {bfs, dfs}.
    @Test
    fun who_listens_keeps_the_user_it_started_with() = runTest {
        anaWithProgress()
        val seen = listenTo(store.mastered(GRAPHS))
        assertEquals(setOf("bfs"), seen.receive())
        progress.logInAs(BETO)
        store.enroll(GRAPHS)
        store.markMastered(GRAPHS, "dijkstra")

        progress.dao.insertMasteredModule(MasteredModuleEntity(ANA, GRAPHS, "dfs", START, pending = true))

        assertEquals(setOf("bfs", "dfs"), seen.receive())
    }

    // El Flow se puede crear antes y escuchar después: manda el usuario de cuando empieza a escuchar.
    @Test
    fun the_session_is_read_when_listening_starts_not_when_the_flow_is_created() = runTest {
        anaWithProgress()
        val mastered = store.mastered(GRAPHS)
        val enrolled = store.enrolled(GRAPHS)

        progress.logInAs(BETO)

        assertTrue(mastered.first().isEmpty())
        assertFalse(enrolled.first())
    }
}
