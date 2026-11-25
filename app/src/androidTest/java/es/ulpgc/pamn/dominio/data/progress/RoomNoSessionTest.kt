package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Sin sesión (como NoSessionProgressTest): leer da vacío y el Flow termina; escribir es un fallo
// de programación, lanza IllegalStateException y no deja filas.
@RunWith(AndroidJUnit4::class)
class RoomNoSessionTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    // toList() solo vuelve si el Flow termina: no se queda esperando a una sesión futura.
    @Test
    fun who_listens_without_session_gets_empty_and_the_flow_ends() = runTest {
        assertEquals(listOf(emptySet<String>()), store.mastered(GRAPHS).toList())
        assertEquals(listOf(false), store.enrolled(GRAPHS).toList())
    }

    @Test
    fun without_session_enrolling_fails() = runTest {
        assertIsThePortFailure(failureOf { store.enroll(GRAPHS) })
    }

    @Test
    fun without_session_mastering_fails() = runTest {
        assertIsThePortFailure(failureOf { store.markMastered(GRAPHS, "bfs") })
    }

    @Test
    fun failed_writes_without_session_leave_nothing_for_the_next_user() = runTest {
        failureOf { store.enroll(GRAPHS) }
        failureOf { store.markMastered(GRAPHS, "bfs") }

        assertEquals(0, progress.rowCount("enrollments"))
        assertEquals(0, progress.rowCount("mastered_modules"))
        progress.logInAs(ANA)
        assertFalse(store.enrolled(GRAPHS).first())
    }

    // Tras cerrar sesión, dominar lanza aunque el último usuario estuviera inscrito.
    @Test
    fun after_logging_out_mastering_fails_and_writes_nothing() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.sessions.clear()

        assertIsThePortFailure(failureOf { store.markMastered(GRAPHS, "bfs") })
        assertEquals(0, progress.rowCount("mastered_modules"))
    }
}
