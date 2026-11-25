package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

// ProgressSync, lo que sube: qué se envía, con qué sesión y qué devuelve.
class ProgressSyncUploadTest {

    @Test
    fun `with nothing pending it sends an empty snapshot and brings the server progress`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        val remote = snapshot(enrollment("grafos", START, "bfs" to START + 1.minutes))
        val gateway = FakeProgressGateway(answer = { _, _ -> synced(remote) })

        fixture.syncWith(gateway).sync(ANA)

        assertEquals(listOf(SyncCall("ana", snapshot())), gateway.calls)
        assertTrue(fixture.store.enrolled("grafos").first())
        assertEquals(setOf("bfs"), fixture.store.mastered("grafos").first())
    }

    @Test
    fun `what is pending is sent with the session of the parameter`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        val pending = fixture.store.pending("ana")
        var token: String? = null
        val gateway = FakeProgressGateway(answer = { session, _ -> token = session.token; synced() })

        fixture.syncWith(gateway).sync(ANA)

        assertEquals(listOf(SyncCall("ana", pending)), gateway.calls)
        assertEquals("t-ana", token)
    }

    // Decisión 11: el usuario sale de la sesión que llega por parámetro, no del almacén de sesiones.
    @Test
    fun `the user comes from the session of the parameter and not from the session store`() = runTest {
        val fixture = ProgressFixture()
        fixture.logInAs("bea")
        fixture.store.enroll("redes")
        fixture.enrolledIn("grafos") // ahora la sesión guardada es la de «ana»
        val anaPending = fixture.store.pending("ana")
        val gateway = FakeProgressGateway(answer = { _, sent -> synced(remote = sent) })

        fixture.syncWith(gateway).sync(sessionOf("bea"))

        assertEquals(listOf(SyncCall("bea", snapshot(enrollment("redes", START)))), gateway.calls)
        assertTrue(fixture.store.pending("bea").isEmpty)
        assertEquals(anaPending, fixture.store.pending("ana"))
    }

    @Test
    fun `it returns the synced outcome the gateway gives`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        val outcome = synced(snapshot(enrollment("grafos", START)), SkippedProgress.WholeEnrollment("x", "COURSE_NOT_FOUND"))
        val gateway = FakeProgressGateway(answer = { _, _ -> outcome })

        assertSame(outcome, fixture.syncWith(gateway).sync(ANA))
    }
}
