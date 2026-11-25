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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

// settle con Room, primera mitad: lo enviado sale de la cola por clave (curso; curso + módulo), y nada más.
@RunWith(AndroidJUnit4::class)
class RoomSettleQueueTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun what_was_sent_leaves_the_queue() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")

        progress.uploadAll()

        assertTrue(store.pending(ANA).isEmpty)
        assertFalse(progress.dao.enrollment(ANA, GRAPHS)!!.pending)
        assertFalse(progress.module(ANA, GRAPHS, "bfs")!!.pending)
    }

    // El servidor redondea a microsegundos: las fechas de sent no coinciden con las del móvil.
    @Test
    fun the_queue_is_cleared_by_key_not_by_date() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 123_456_789.nanoseconds
        store.markMastered(GRAPHS, "bfs")
        val sent = snapshot(enrollment(GRAPHS, START + 1.minutes, "bfs" to START + 123_457_000.nanoseconds))

        store.settle(ANA, sent, remote = snapshot())

        assertTrue(store.pending(ANA).isEmpty)
    }

    // De sent solo se usan las claves: sus fechas no se escriben (ni aunque sean más antiguas).
    @Test
    fun the_dates_of_sent_are_not_stored() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val sent = snapshot(enrollment(GRAPHS, START - 10.minutes, "bfs" to START - 10.minutes))

        store.settle(ANA, sent, remote = snapshot())

        assertEquals(START, progress.dao.enrollment(ANA, GRAPHS)!!.enrolledAt)
        assertEquals(START, progress.module(ANA, GRAPHS, "bfs")!!.masteredAt)
    }

    @Test
    fun an_enrollment_made_during_the_request_stays_pending() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val sent = store.pending(ANA)
        store.enroll(TREES)

        store.settle(ANA, sent, remote = sent)

        assertSameSnapshot(snapshot(enrollment(TREES, START)), store.pending(ANA))
    }

    @Test
    fun a_module_mastered_during_the_request_stays_pending_even_in_a_course_that_was_sent() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val sent = store.pending(ANA)
        progress.clock.time = START + 2.minutes
        store.markMastered(GRAPHS, "dfs")

        store.settle(ANA, sent, remote = sent)

        assertSameSnapshot(snapshot(enrollment(GRAPHS, START, "dfs" to START + 2.minutes)), store.pending(ANA))
    }

    // B-8: lo omitido por el servidor (no está en remote) sale de la cola y se conserva en el móvil.
    @Test
    fun a_skipped_enrollment_leaves_the_queue_and_stays_on_the_phone() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val sent = store.pending(ANA)

        store.settle(ANA, sent, remote = snapshot())

        assertTrue(store.pending(ANA).isEmpty)
        assertTrue(store.enrolled(GRAPHS).first())
        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
    }

    @Test
    fun a_skipped_module_leaves_the_queue_and_stays_on_the_phone() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val sent = store.pending(ANA)

        store.settle(ANA, sent, remote = snapshot(enrollment(GRAPHS, START)))

        assertTrue(store.pending(ANA).isEmpty)
        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
    }

    @Test
    fun a_course_that_was_sent_but_is_not_on_the_phone_is_ignored() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val sent = snapshot(enrollment(TREES, START, "avl" to START))

        store.settle(ANA, sent, remote = snapshot())

        assertNull(progress.dao.enrollment(ANA, TREES))
        assertEquals(0, progress.rowCount("mastered_modules"))
        assertSameSnapshot(snapshot(enrollment(GRAPHS, START)), store.pending(ANA))
    }

    @Test
    fun a_module_that_was_sent_but_is_not_on_the_phone_is_ignored() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val sent = snapshot(enrollment(GRAPHS, START, "dfs" to START))

        store.settle(ANA, sent, remote = snapshot())

        assertNull(progress.module(ANA, GRAPHS, "dfs"))
        // La inscripción sí iba en sent (sale de la cola); «bfs» no iba: sigue pendiente.
        assertSameSnapshot(snapshot(enrollment(GRAPHS, START, "bfs" to START)), store.pending(ANA))
    }

    // La clave incluye el usuario: el mismo curso de otro usuario no sale de su cola.
    @Test
    fun settling_one_user_does_not_touch_another_in_the_same_course() = runTest {
        progress.logInAs(BETO)
        store.enroll(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val betoBefore = progress.allRows(BETO)

        progress.uploadAll(ANA)

        assertEquals(betoBefore, progress.allRows(BETO))
        assertSameSnapshot(snapshot(enrollment(GRAPHS, START, "bfs" to START)), store.pending(BETO))
        assertTrue(store.pending(ANA).isEmpty)
    }
}
