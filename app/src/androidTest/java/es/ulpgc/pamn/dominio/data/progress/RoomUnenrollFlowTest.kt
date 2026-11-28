package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Los Flow de RoomProgressStore tras una baja: emiten false y emptySet() para el curso dado de
// baja, sin avisar a oyentes de otros cursos ni de otros usuarios (Channel + barrera, sin delay).
@RunWith(AndroidJUnit4::class)
class RoomUnenrollFlowTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun watch_enrolled_and_watch_mastered_emit_false_and_empty_set_after_unenroll() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val seenEnrolled = listenTo(store.enrolled(GRAPHS))
        val seenMastered = listenTo(store.mastered(GRAPHS))
        assertEquals(true, seenEnrolled.receive())
        assertEquals(setOf("bfs"), seenMastered.receive())

        store.unenroll(ANA, GRAPHS)

        assertEquals(false, seenEnrolled.receive())
        assertEquals(emptySet<String>(), seenMastered.receive())
    }

    @Test
    fun unenrolling_does_not_notify_listeners_of_another_course() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        store.markMastered(GRAPHS, "bfs")
        store.markMastered(TREES, "root")
        val seenTreesEnrolled = listenTo(store.enrolled(TREES))
        val seenTreesMastered = listenTo(store.mastered(TREES))
        val barrier = listenTo(store.enrolled(GRAPHS))
        assertEquals(true, seenTreesEnrolled.receive())
        assertEquals(setOf("root"), seenTreesMastered.receive())
        assertEquals(true, barrier.receive())

        store.unenroll(ANA, GRAPHS)
        assertEquals(false, barrier.receive())
        store.markMastered(TREES, "leaf")
        assertEquals(setOf("root", "leaf"), seenTreesMastered.receive())

        store.unenroll(ANA, TREES)

        assertEquals(emptySet<String>(), seenTreesMastered.receive())
        assertEquals(false, seenTreesEnrolled.receive())
    }

    @Test
    fun unenrolling_does_not_notify_listeners_of_another_user_in_the_same_course() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val barrier = listenTo(store.enrolled(GRAPHS))
        assertEquals(true, barrier.receive())

        progress.logInAs(BETO)
        store.enroll(GRAPHS)
        store.markMastered(GRAPHS, "dfs")
        val betoEnrolled = listenTo(store.enrolled(GRAPHS))
        val betoMastered = listenTo(store.mastered(GRAPHS))
        assertEquals(true, betoEnrolled.receive())
        assertEquals(setOf("dfs"), betoMastered.receive())

        store.unenroll(ANA, GRAPHS)
        assertEquals(false, barrier.receive())
        store.markMastered(GRAPHS, "dijkstra")
        assertEquals(setOf("dfs", "dijkstra"), betoMastered.receive())

        store.unenroll(BETO, GRAPHS)

        assertEquals(emptySet<String>(), betoMastered.receive())
        assertEquals(false, betoEnrolled.receive())
    }
}
