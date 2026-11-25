package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

// El gateway falso para los tests de ProgressSync: devuelve lo configurado y recuerda lo que recibió.
class FakeProgressGatewayTest {

    private val session = sessionOf("u1")
    private val pending = ProgressSnapshot(
        listOf(EnrollmentSnapshot(Enrollment("c1", Instant.parse("2026-11-24T10:00:00Z")), emptyList())),
    )

    private fun sessionOf(userId: String, token: String = "tok-secreto-123") = Session(
        token = token,
        expiresAt = "2026-12-01T00:00:00Z",
        account = Account(userId, "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"),
    )

    @Test
    fun `by default it answers synced with nothing`() = runTest {
        val outcome = FakeProgressGateway().sync(session, pending)
        assertEquals(SyncOutcome.Synced(SyncResponse(ProgressSnapshot(emptyList()), emptyList())), outcome)
    }

    @Test
    fun `it answers what it is told for every outcome`() = runTest {
        listOf(SyncOutcome.SessionExpired, SyncOutcome.Unreachable, SyncOutcome.ServerFailure).forEach { wanted ->
            assertEquals(wanted, FakeProgressGateway(answer = { _, _ -> wanted }).sync(session, pending))
        }
    }

    @Test
    fun `it can answer a synced response with content`() = runTest {
        val response = SyncResponse(pending, listOf(SkippedProgress.WholeEnrollment("c9", "COURSE_NOT_FOUND")))
        val gateway = FakeProgressGateway(answer = { _, _ -> SyncOutcome.Synced(response) })
        assertEquals(SyncOutcome.Synced(response), gateway.sync(session, pending))
    }

    @Test
    fun `the answer can depend on the session and on what is sent`() = runTest {
        val gateway = FakeProgressGateway(answer = { s, sent ->
            if (s.account.id == "u1" && !sent.isEmpty) SyncOutcome.Unreachable else SyncOutcome.SessionExpired
        })
        assertEquals(SyncOutcome.Unreachable, gateway.sync(session, pending))
        assertEquals(SyncOutcome.SessionExpired, gateway.sync(sessionOf("u2"), pending))
        assertEquals(SyncOutcome.SessionExpired, gateway.sync(session, ProgressSnapshot(emptyList())))
    }

    @Test
    fun `it remembers the user and what was sent in order`() = runTest {
        val gateway = FakeProgressGateway()
        val empty = ProgressSnapshot(emptyList())
        gateway.sync(session, pending)
        gateway.sync(sessionOf("u2"), empty)
        assertEquals(listOf(SyncCall("u1", pending), SyncCall("u2", empty)), gateway.calls)
    }

    @Test
    fun `it has no calls before being used`() {
        assertEquals(emptyList(), FakeProgressGateway().calls)
    }

    // Lo que recuerda no guarda el token: ni en el objeto ni en su texto.
    @Test
    fun `what it remembers never shows the token`() = runTest {
        val gateway = FakeProgressGateway()
        gateway.sync(session, pending)
        assertFalse("tok-secreto-123" in gateway.calls.toString())
        assertFalse("tok-secreto-123" in gateway.toString())
    }
}
