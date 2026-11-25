package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// ProgressSync sin 200: no se toca el almacén, se devuelve el desenlace tal cual y no se reintenta.
class ProgressSyncFailureTest {

    // Algo ya subido («grafos» con «bfs») y algo pendiente («dfs» y la inscripción en «redes»).
    // El servidor, si contestara, traería un curso nuevo: un fallo no debe traerlo.
    private suspend fun failing(outcome: SyncOutcome): Triple<ProgressFixture, FakeProgressGateway, SyncOutcome> {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        fixture.uploadAll()
        fixture.store.markMastered("grafos", "dfs")
        fixture.store.enroll("redes")
        val before = fixture.store.progressOf("ana")
        val gateway = FakeProgressGateway(answer = { _, _ -> outcome })

        val result = fixture.syncWith(gateway).sync(ANA)

        assertEquals(before, fixture.store.progressOf("ana")) // intacto: cola, fechas y nada nuevo
        return Triple(fixture, gateway, result)
    }

    @Test
    fun `unreachable leaves what is pending intact and is returned as is`() = runTest {
        assertEquals(SyncOutcome.Unreachable, failing(SyncOutcome.Unreachable).third)
    }

    @Test
    fun `server failure leaves what is pending intact and is returned as is`() = runTest {
        assertEquals(SyncOutcome.ServerFailure, failing(SyncOutcome.ServerFailure).third)
    }

    @Test
    fun `session expired leaves what is pending intact and is returned as is`() = runTest {
        assertEquals(SyncOutcome.SessionExpired, failing(SyncOutcome.SessionExpired).third)
    }

    @Test
    fun `session expired does not retry and makes one call`() = runTest {
        assertEquals(1, failing(SyncOutcome.SessionExpired).second.calls.size)
    }

    @Test
    fun `unreachable does not retry and makes one call`() = runTest {
        assertEquals(1, failing(SyncOutcome.Unreachable).second.calls.size)
    }

    @Test
    fun `server failure does not retry and makes one call`() = runTest {
        assertEquals(1, failing(SyncOutcome.ServerFailure).second.calls.size)
    }
}
