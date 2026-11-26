package es.ulpgc.pamn.dominio.course.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Decisión de reintento para la sincronización en segundo plano según el desenlace y el intento.
class BackgroundSyncDecisionTest {

    private val emptySynced = SyncOutcome.Synced(
        SyncResponse(remote = ProgressSnapshot(emptyList()), skipped = emptyList()),
    )

    @Test
    fun `no session is done`() {
        assertEquals(BackgroundSyncDecision.Done, backgroundSyncDecision(SessionSyncResult.NoSession, attempt = 0))
        assertEquals(BackgroundSyncDecision.Done, backgroundSyncDecision(SessionSyncResult.NoSession, attempt = 10))
    }

    @Test
    fun `synced is done`() {
        val result = SessionSyncResult.Finished(emptySynced)
        assertEquals(BackgroundSyncDecision.Done, backgroundSyncDecision(result, attempt = 0))
        assertEquals(BackgroundSyncDecision.Done, backgroundSyncDecision(result, attempt = 10))
    }

    @Test
    fun `session expired gives up`() {
        val result = SessionSyncResult.Finished(SyncOutcome.SessionExpired)
        assertEquals(BackgroundSyncDecision.GiveUp, backgroundSyncDecision(result, attempt = 0))
        assertEquals(BackgroundSyncDecision.GiveUp, backgroundSyncDecision(result, attempt = 10))
    }

    @Test
    fun `unreachable retries on first attempt`() {
        val result = SessionSyncResult.Finished(SyncOutcome.Unreachable)
        assertEquals(BackgroundSyncDecision.Retry, backgroundSyncDecision(result, attempt = 0))
    }

    @Test
    fun `unreachable still retries on attempt 100`() {
        val result = SessionSyncResult.Finished(SyncOutcome.Unreachable)
        assertEquals(BackgroundSyncDecision.Retry, backgroundSyncDecision(result, attempt = 100))
    }

    @Test
    fun `server failure retries on attempts 0 through 3`() {
        val result = SessionSyncResult.Finished(SyncOutcome.ServerFailure)
        for (attempt in 0 until MAX_SERVER_FAILURE_ATTEMPTS - 1) {
            assertEquals(BackgroundSyncDecision.Retry, backgroundSyncDecision(result, attempt = attempt))
        }
    }

    @Test
    fun `server failure gives up on fifth attempt`() {
        val result = SessionSyncResult.Finished(SyncOutcome.ServerFailure)
        assertEquals(
            BackgroundSyncDecision.GiveUp,
            backgroundSyncDecision(result, attempt = MAX_SERVER_FAILURE_ATTEMPTS - 1),
        )
    }

    @Test
    fun `server failure gives up beyond fifth attempt`() {
        val result = SessionSyncResult.Finished(SyncOutcome.ServerFailure)
        assertEquals(BackgroundSyncDecision.GiveUp, backgroundSyncDecision(result, attempt = 5))
        assertEquals(BackgroundSyncDecision.GiveUp, backgroundSyncDecision(result, attempt = 100))
    }

    @Test
    fun `negative attempt throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            backgroundSyncDecision(SessionSyncResult.NoSession, attempt = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            backgroundSyncDecision(SessionSyncResult.Finished(SyncOutcome.ServerFailure), attempt = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            backgroundSyncDecision(SessionSyncResult.Finished(SyncOutcome.Unreachable), attempt = -1)
        }
    }

    @Test
    fun `every decision can be handled without else`() {
        // Si se añade una variante a BackgroundSyncDecision, este when deja de compilar.
        fun label(decision: BackgroundSyncDecision): String = when (decision) {
            BackgroundSyncDecision.Done -> "done"
            BackgroundSyncDecision.Retry -> "retry"
            BackgroundSyncDecision.GiveUp -> "give up"
        }
        assertEquals("done", label(backgroundSyncDecision(SessionSyncResult.NoSession, attempt = 0)))
        assertEquals("retry", label(backgroundSyncDecision(SessionSyncResult.Finished(SyncOutcome.Unreachable), attempt = 0)))
        assertEquals("give up", label(backgroundSyncDecision(SessionSyncResult.Finished(SyncOutcome.SessionExpired), attempt = 0)))
    }
}
