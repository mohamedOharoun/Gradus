package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.ProgressSync
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// settle es todo o nada (una sola transacción). Para que falle a mitad sin trucos, el servidor manda,
// en el último módulo, una fecha que no cabe en nanosegundos desde 1970 (InstantConverter lanza
// IllegalArgumentException al guardarla). Para entonces ya se ha sacado sent de la cola, se ha
// cambiado una fecha (MIN) y se ha insertado una inscripción nueva: todo eso se deshace.
@RunWith(AndroidJUnit4::class)
class RoomSettleAtomicityTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    private val outOfRange = Instant.parse("2300-01-01T00:00:00Z")

    // Lo que ya se habría aplicado antes del fallo: fecha más antigua para GRAPHS e inscripción en TREES.
    private val badRemote = snapshot(
        enrollment(GRAPHS, START - 1.minutes, "bfs" to START - 1.minutes),
        enrollment(TREES, START, "avl" to outOfRange),
    )

    @Test
    fun a_settle_that_fails_halfway_leaves_nothing_half_done() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val sent = store.pending(ANA)
        val before = progress.allRows()

        val failure = failureOf { store.settle(ANA, sent, badRemote) }

        assertEquals("llegó $failure", IllegalArgumentException::class.java, failure?.javaClass)
        assertEquals(before, progress.allRows())
        assertSameSnapshot(sent, store.pending(ANA))
    }

    @Test
    fun after_a_failed_settle_the_next_one_works() = runTest {
        progress.enrolledAsAna(GRAPHS)
        val sent = store.pending(ANA)
        failureOf { store.settle(ANA, sent, badRemote) }

        progress.uploadAll()

        assertTrue(store.pending(ANA).isEmpty)
    }

    // ProgressSync no captura los fallos del almacén: la excepción sale y todo sigue pendiente.
    @Test
    fun through_progress_sync_the_store_failure_comes_out_and_nothing_changes() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val pendingBefore = store.pending(ANA)
        val before = progress.allRows()
        val gateway = FakeProgressGateway(answer = { _, _ -> synced(badRemote) })

        val failure = failureOf { ProgressSync(store, gateway).sync(sessionOf(ANA)) }

        assertEquals("llegó $failure", IllegalArgumentException::class.java, failure?.javaClass)
        assertEquals(before, progress.allRows())
        assertSameSnapshot(pendingBefore, store.pending(ANA))
    }
}
