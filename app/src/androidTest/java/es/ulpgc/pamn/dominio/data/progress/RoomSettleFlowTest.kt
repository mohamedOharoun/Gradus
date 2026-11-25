package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Quien escucha mastered o enrolled se entera de lo que llega del servidor: settle escribe en las
// mismas tablas que leen los Flow, y Room avisa al terminar la transacción. Sin tiempos: receive().
@RunWith(AndroidJUnit4::class)
class RoomSettleFlowTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun whoever_listens_to_mastered_hears_what_was_merged() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val seen = listenTo(store.mastered(GRAPHS))
        assertEquals(emptySet<String>(), seen.receive())

        store.settle(ANA, snapshot(), remote = snapshot(enrollment(GRAPHS, START, "bfs" to START)))

        assertEquals(setOf("bfs"), seen.receive())
    }

    @Test
    fun whoever_listens_to_enrolled_hears_what_was_merged() = runTest {
        progress.logInAs(ANA)
        val seen = listenTo(store.enrolled(TREES))
        assertEquals(false, seen.receive())

        store.settle(ANA, snapshot(), remote = snapshot(enrollment(TREES, START)))

        assertEquals(true, seen.receive())
    }
}
