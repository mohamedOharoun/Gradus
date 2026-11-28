package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// Comportamiento de baja en FakeProgressGateway: desenlace configurable, suspensión y registro sin token.
class FakeProgressGatewayUnenrollTest {

    private val session = sessionOf("u1")

    private fun sessionOf(userId: String, token: String = "tok-secreto-123") = Session(
        token = token,
        expiresAt = "2026-12-01T00:00:00Z",
        account = Account(userId, "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"),
    )

    @Test
    fun `by default it answers unenrolled`() = runTest {
        assertEquals(UnenrollOutcome.Unenrolled, FakeProgressGateway().unenroll(session, "c1"))
    }

    @Test
    fun `it answers what it is told for every outcome`() = runTest {
        val outcomes = listOf(
            UnenrollOutcome.Unenrolled,
            UnenrollOutcome.SessionExpired,
            UnenrollOutcome.Unreachable,
            UnenrollOutcome.ServerFailure,
        )
        outcomes.forEach { wanted ->
            val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> wanted })
            assertEquals(wanted, gateway.unenroll(session, "c1"))
        }
    }

    @Test
    fun `the answer can depend on the session and on the courseId`() = runTest {
        val gateway = FakeProgressGateway(unenrollAnswer = { s, courseId ->
            if (s.account.id == "u1" && courseId == "c1") UnenrollOutcome.Unenrolled else UnenrollOutcome.ServerFailure
        })
        assertEquals(UnenrollOutcome.Unenrolled, gateway.unenroll(session, "c1"))
        assertEquals(UnenrollOutcome.ServerFailure, gateway.unenroll(sessionOf("u2"), "c1"))
        assertEquals(UnenrollOutcome.ServerFailure, gateway.unenroll(session, "c2"))
    }

    @Test
    fun `the answer can suspend and the call is remembered while it waits`() = runTest {
        val gate = CompletableDeferred<UnenrollOutcome>()
        val gateway = FakeProgressGateway(unenrollAnswer = { _, _ -> gate.await() })
        val outcome = async { gateway.unenroll(session, "c1") }
        yield()
        assertEquals(listOf(UnenrollCall("u1", "c1")), gateway.unenrollCalls)
        gate.complete(UnenrollOutcome.Unreachable)
        assertEquals(UnenrollOutcome.Unreachable, outcome.await())
    }

    @Test
    fun `it remembers unenroll calls in order without mixing them with sync calls`() = runTest {
        val gateway = FakeProgressGateway()
        gateway.unenroll(session, "c1")
        gateway.sync(session, ProgressSnapshot(emptyList()))
        gateway.unenroll(sessionOf("u2"), "c2")

        assertEquals(listOf(UnenrollCall("u1", "c1"), UnenrollCall("u2", "c2")), gateway.unenrollCalls)
        assertEquals(listOf(SyncCall("u1", ProgressSnapshot(emptyList()))), gateway.calls)
    }

    @Test
    fun `it has no unenroll calls before being used`() {
        assertEquals(emptyList(), FakeProgressGateway().unenrollCalls)
    }

    @Test
    fun `what it remembers never shows the token`() = runTest {
        val gateway = FakeProgressGateway()
        gateway.unenroll(session, "c1")
        assertFalse("tok-secreto-123" in gateway.unenrollCalls.toString())
        assertFalse("tok-secreto-123" in gateway.toString())
    }
}
