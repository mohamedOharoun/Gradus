package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.progress.FakeProgressGateway
import es.ulpgc.pamn.dominio.course.progress.ProgressSync
import es.ulpgc.pamn.dominio.course.progress.SyncCall
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes

// El caso de uso entero con el almacén Room: ProgressSync(RoomProgressStore, FakeProgressGateway).
// ProgressSync no cambia; solo cambia el almacén que recibe.
@RunWith(AndroidJUnit4::class)
class RoomProgressSyncTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    private fun syncWith(gateway: FakeProgressGateway) = ProgressSync(store, gateway)

    @Test
    fun sync_uploads_what_is_pending_and_empties_the_queue() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        store.markMastered(GRAPHS, "bfs")
        val pendingBefore = store.pending(ANA)
        val echo = FakeProgressGateway(answer = { _, sent -> synced(remote = sent) })

        val outcome = syncWith(echo).sync(sessionOf(ANA))

        assertEquals(synced(remote = pendingBefore), outcome)
        assertEquals(listOf(SyncCall(ANA, pendingBefore)), echo.calls)
        assertTrue(store.pending(ANA).isEmpty)
    }

    // B-10: también sin nada pendiente se llama (envía vacío) y así llega lo de otro móvil.
    @Test
    fun what_was_mastered_on_another_phone_appears() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.uploadAll()
        val remote = snapshot(enrollment(GRAPHS, START, "bfs" to START + 5.minutes))
        val gateway = FakeProgressGateway(answer = { _, _ -> synced(remote) })

        syncWith(gateway).sync(sessionOf(ANA))

        assertTrue(gateway.calls.single().sent.isEmpty)
        assertEquals(setOf("bfs"), store.mastered(GRAPHS).first())
        assertEquals(START + 5.minutes, progress.module(ANA, GRAPHS, "bfs")!!.masteredAt)
        assertTrue(store.pending(ANA).isEmpty)
    }

    // settle recibe el sent que de verdad se envió, no lo pendiente al volver la respuesta.
    @Test
    fun what_is_marked_while_the_request_travels_stays_pending() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val gateway = FakeProgressGateway(answer = { _, sent ->
            progress.clock.time = START + 1.minutes
            store.markMastered(GRAPHS, "dfs") // el estudiante sigue estudiando
            synced(remote = sent)
        })

        syncWith(gateway).sync(sessionOf(ANA))

        assertSameSnapshot(snapshot(enrollment(GRAPHS, START, "dfs" to START + 1.minutes)), store.pending(ANA))
    }

    @Test
    fun unreachable_changes_nothing() = runTest { assertFailedSyncChangesNothing(SyncOutcome.Unreachable) }

    @Test
    fun server_failure_changes_nothing() = runTest { assertFailedSyncChangesNothing(SyncOutcome.ServerFailure) }

    @Test
    fun session_expired_changes_nothing() = runTest { assertFailedSyncChangesNothing(SyncOutcome.SessionExpired) }

    // Sin 200 no se llama a settle: las filas (cola, fechas y nada nuevo) quedan igual.
    private suspend fun assertFailedSyncChangesNothing(failed: SyncOutcome) {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        val pendingBefore = store.pending(ANA)
        val before = progress.allRows()

        val outcome = syncWith(FakeProgressGateway(answer = { _, _ -> failed })).sync(sessionOf(ANA))

        assertEquals(failed, outcome)
        assertEquals(before, progress.allRows())
        assertSameSnapshot(pendingBefore, store.pending(ANA))
    }
}
