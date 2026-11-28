package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

// ProgressSync con corrutinas: dos sincronizaciones a la vez (B-11: sin Mutex) y cancelación.
// El gateway espera a que el test complete un CompletableDeferred: así el test decide cuándo
// «llega» cada respuesta.
class ProgressSyncConcurrencyTest {

    private val grafosAll = snapshot(enrollment("grafos", START, "bfs" to START, "dfs" to START + 1.minutes))
    private val grafosOld = snapshot(enrollment("grafos", START, "bfs" to START))
    private val redes = enrollment("redes", START - 1.minutes)

    // Dos sync a la vez: la primera envía «grafos» con «bfs»; mientras viaja se domina «dfs» y la
    // segunda envía los dos. Devuelve las dos respuestas pendientes para soltarlas en el orden que se quiera.
    private suspend fun twoSyncsAtOnce(
        fixture: ProgressFixture,
        scope: CoroutineScope,
    ): Pair<List<CompletableDeferred<SyncOutcome>>, FakeProgressGateway> {
        val gates = listOf(CompletableDeferred<SyncOutcome>(), CompletableDeferred<SyncOutcome>())
        var next = 0
        val gateway = FakeProgressGateway(answer = { _, _ -> gates[next++].await() })
        val sync = fixture.syncWith(gateway)
        scope.launch { sync.sync(ANA) }
        yield() // la primera envía y espera
        fixture.clock.time = START + 1.minutes
        fixture.store.markMastered("grafos", "dfs")
        scope.launch { sync.sync(ANA) }
        yield() // la segunda envía y espera
        return gates to gateway
    }

    @Test
    fun `two syncs at once leave the queue empty and the state is the union`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val (gates, gateway) = twoSyncsAtOnce(fixture, this)
        assertEquals(2, gateway.calls.size)

        gates[0].complete(synced(grafosOld))
        gates[1].complete(synced(snapshot(*grafosAll.enrollments.toTypedArray(), redes)))
        yield()

        assertTrue(fixture.store.pending("ana").isEmpty)
        assertSameSnapshot(snapshot(*grafosAll.enrollments.toTypedArray(), redes), allOf(fixture))
    }

    @Test
    fun `an old response that arrives late does not undo the new one`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val (gates, _) = twoSyncsAtOnce(fixture, this)

        gates[1].complete(synced(snapshot(*grafosAll.enrollments.toTypedArray(), redes)))
        yield()
        gates[0].complete(synced(grafosOld)) // la antigua no trae ni «dfs» ni «redes»
        yield()

        assertTrue(fixture.store.pending("ana").isEmpty)
        assertSameSnapshot(snapshot(*grafosAll.enrollments.toTypedArray(), redes), allOf(fixture))
    }

    @Test
    fun `cancelling while the request travels does not touch the store`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val before = fixture.store.progressOf("ana")
        val gate = CompletableDeferred<SyncOutcome>()
        val job = async { fixture.syncWith(FakeProgressGateway(answer = { _, _ -> gate.await() })).sync(ANA) }
        yield()

        job.cancel()
        gate.complete(synced(grafosOld)) // aunque la respuesta llegue, ya nadie la fusiona
        yield()

        assertTrue(job.isCancelled)
        assertEquals(before, fixture.store.progressOf("ana"))
    }

    // Lo mismo que pasaría si la cancelación llegara entre la respuesta y settle (Room): se reenvía.
    @Test
    fun `after a cancellation the next sync resends what is pending and uploads it`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val pending = fixture.store.pending("ana")
        val stuck = launch { fixture.syncWith(FakeProgressGateway(answer = { _, _ -> CompletableDeferred<SyncOutcome>().await() })).sync(ANA) }
        yield()
        stuck.cancel()

        val gateway = FakeProgressGateway(answer = { _, sent -> synced(remote = sent) })
        fixture.syncWith(gateway).sync(ANA)

        assertEquals(listOf(SyncCall("ana", pending)), gateway.calls)
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    // Todo el progreso de «ana» como snapshot (con lo ya subido), para compararlo sin orden.
    private fun allOf(fixture: ProgressFixture) = ProgressSnapshot(
        fixture.store.progressOf("ana").courses.values.map { EnrollmentSnapshot(it.enrollment, it.mastered.values.toList()) },
    )
}
