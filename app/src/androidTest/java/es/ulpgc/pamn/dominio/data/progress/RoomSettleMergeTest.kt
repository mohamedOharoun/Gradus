package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

// settle con Room, segunda mitad: lo que llega del servidor se fusiona (unión, gana la fecha más
// antigua, lo nuevo no queda pendiente, lo que ya estaba conserva su pending, nada local se borra).
@RunWith(AndroidJUnit4::class)
class RoomSettleMergeTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    private suspend fun enrolledAt(courseId: String) = progress.dao.enrollment(ANA, courseId)!!.enrolledAt

    private suspend fun masteredAt(courseId: String, moduleId: String) =
        progress.module(ANA, courseId, moduleId)!!.masteredAt

    @Test
    fun a_course_the_phone_did_not_have_appears_and_is_not_pending() = runTest {
        progress.logInAs(ANA)
        val remote = snapshot(enrollment(TREES, START, "avl" to START + 1.minutes))

        store.settle(ANA, sent = snapshot(), remote = remote)

        assertTrue(store.enrolled(TREES).first())
        assertEquals(setOf("avl"), store.mastered(TREES).first())
        assertEquals(START, enrolledAt(TREES))
        assertEquals(START + 1.minutes, masteredAt(TREES, "avl"))
        assertFalse(progress.dao.enrollment(ANA, TREES)!!.pending)
        assertFalse(progress.module(ANA, TREES, "avl")!!.pending)
        assertTrue(store.pending(ANA).isEmpty)
    }

    @Test
    fun a_new_module_of_a_known_course_appears_and_is_not_pending() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val sent = store.pending(ANA)

        store.settle(ANA, sent, remote = snapshot(enrollment(GRAPHS, START, "bfs" to START)))

        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
        assertFalse(progress.module(ANA, GRAPHS, "bfs")!!.pending)
        assertTrue(store.pending(ANA).isEmpty)
    }

    @Test
    fun the_older_enrollment_date_wins_when_it_comes_from_the_server() = runTest {
        progress.clock.time = START + 10.minutes
        progress.enrolledAsAna(GRAPHS)

        store.settle(ANA, store.pending(ANA), remote = snapshot(enrollment(GRAPHS, START)))

        assertEquals(START, enrolledAt(GRAPHS))
    }

    @Test
    fun the_older_enrollment_date_wins_when_it_is_local() = runTest {
        progress.enrolledAsAna(GRAPHS)

        store.settle(ANA, snapshot(), remote = snapshot(enrollment(GRAPHS, START + 10.minutes)))

        assertEquals(START, enrolledAt(GRAPHS))
    }

    @Test
    fun the_older_mastered_date_wins_when_it_comes_from_the_server() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 10.minutes
        store.markMastered(GRAPHS, "bfs")

        store.settle(ANA, store.pending(ANA), remote = snapshot(enrollment(GRAPHS, START, "bfs" to START + 1.minutes)))

        assertEquals(START + 1.minutes, masteredAt(GRAPHS, "bfs"))
    }

    @Test
    fun the_older_mastered_date_wins_when_it_is_local() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 1.minutes
        store.markMastered(GRAPHS, "bfs")

        store.settle(ANA, snapshot(), remote = snapshot(enrollment(GRAPHS, START, "bfs" to START + 10.minutes)))

        assertEquals(START + 1.minutes, masteredAt(GRAPHS, "bfs"))
    }

    // El reloj del móvil va una hora adelantado; el servidor recorta las fechas a su hora (START).
    @Test
    fun a_future_local_date_is_replaced_by_the_clipped_server_date() = runTest {
        progress.clock.time = START + 1.hours
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")

        store.settle(ANA, store.pending(ANA), remote = snapshot(enrollment(GRAPHS, START, "bfs" to START)))

        assertEquals(START, enrolledAt(GRAPHS))
        assertEquals(START, masteredAt(GRAPHS, "bfs"))
    }

    // Nada se envió (sent vacío), pero otro móvil ya lo había subido con fechas anteriores: se queda
    // la fecha más antigua y sigue pendiente (reenviarlo es inofensivo).
    @Test
    fun a_pending_item_that_also_comes_from_the_server_stays_pending() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val earlier = START - 5.minutes

        store.settle(ANA, snapshot(), remote = snapshot(enrollment(GRAPHS, earlier, "bfs" to earlier)))

        assertSameSnapshot(snapshot(enrollment(GRAPHS, earlier, "bfs" to earlier)), store.pending(ANA))
    }

    @Test
    fun nothing_local_is_deleted() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        store.markMastered(GRAPHS, "bfs")
        progress.uploadAll()
        val before = progress.allRows()

        store.settle(ANA, sent = snapshot(), remote = snapshot(enrollment(GRAPHS, START)))

        assertEquals(before, progress.allRows())
    }

    @Test
    fun what_comes_from_the_server_goes_only_to_its_user() = runTest {
        progress.logInAs(BETO)
        progress.clock.time = START + 1.minutes
        store.enroll(GRAPHS)
        val betoBefore = progress.allRows(BETO)

        val remote = snapshot(enrollment(GRAPHS, START - 10.minutes, "bfs" to START), enrollment(TREES, START))
        store.settle(ANA, sent = snapshot(), remote = remote)

        assertEquals(betoBefore, progress.allRows(BETO))
        assertNull(progress.dao.enrollment(BETO, TREES))
        assertEquals(START - 10.minutes, enrolledAt(GRAPHS))
    }

    @Test
    fun settling_twice_with_the_same_leaves_the_same_state() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val sent = store.pending(ANA)
        val remote = snapshot(
            enrollment(GRAPHS, START - 1.minutes, "bfs" to START, "dfs" to START),
            enrollment(TREES, START),
        )

        store.settle(ANA, sent, remote)
        val once = progress.allRows()
        store.settle(ANA, sent, remote)

        assertEquals(once, progress.allRows())
    }

    // B-11 (dos sync a la vez): la respuesta de una petición anterior llega tarde, cuando ya se había
    // fusionado y el estudiante había dominado algo nuevo. El servidor redondea hacia arriba a
    // microsegundos, así que sus fechas son algo más recientes que las del móvil.
    @Test
    fun a_late_old_response_does_not_undo_what_is_new() = runTest {
        progress.clock.time = START + 123_456_789.nanoseconds
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 1.minutes + 987_654_321.nanoseconds
        store.markMastered(GRAPHS, "bfs")
        val oldSent = store.pending(ANA)
        val oldRemote = snapshot(
            enrollment(GRAPHS, START + 123_457_000.nanoseconds, "bfs" to START + 1.minutes + 987_655_000.nanoseconds),
        )
        store.settle(ANA, oldSent, oldRemote) // la primera respuesta
        progress.clock.time = START + 5.minutes
        store.markMastered(GRAPHS, "dfs")
        val before = progress.allRows()

        store.settle(ANA, oldSent, oldRemote) // la respuesta antigua que llega tarde

        assertSameSnapshot(snapshot(enrollment(GRAPHS, START + 123_456_789.nanoseconds, "dfs" to START + 5.minutes)), store.pending(ANA))
        assertEquals(START + 123_456_789.nanoseconds, enrolledAt(GRAPHS))
        assertEquals(START + 1.minutes + 987_654_321.nanoseconds, masteredAt(GRAPHS, "bfs"))
        assertEquals(START + 5.minutes, masteredAt(GRAPHS, "dfs"))
        assertEquals(before, progress.allRows())
    }
}
