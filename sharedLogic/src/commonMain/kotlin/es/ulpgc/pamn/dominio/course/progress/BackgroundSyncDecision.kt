package es.ulpgc.pamn.dominio.course.progress

// Número máximo de intentos para ServerFailure. Como runAttemptCount empieza en 0, reintenta en los
// intentos 0..3 y se rinde en el 5.º intento (attempt == 4).
const val MAX_SERVER_FAILURE_ATTEMPTS: Int = 5

// Qué debe hacer el trabajo en segundo plano tras intentar sincronizar el progreso.
// Sin acoplar a WorkManager ni a Android: el Worker traduce cada variante a su Result.
sealed interface BackgroundSyncDecision {
    data object Done : BackgroundSyncDecision
    data object Retry : BackgroundSyncDecision
    data object GiveUp : BackgroundSyncDecision
}

// Función pura: decide si terminar, reintentar o rendirse según el desenlace y el intento (0-based).
fun backgroundSyncDecision(
    result: SessionSyncResult,
    attempt: Int,
): BackgroundSyncDecision {
    require(attempt >= 0) { "attempt must be non-negative: $attempt" }
    return when (result) {
        SessionSyncResult.NoSession -> BackgroundSyncDecision.Done
        is SessionSyncResult.Finished -> when (result.outcome) {
            is SyncOutcome.Synced -> BackgroundSyncDecision.Done
            SyncOutcome.Unreachable -> BackgroundSyncDecision.Retry
            SyncOutcome.ServerFailure ->
                if (attempt < MAX_SERVER_FAILURE_ATTEMPTS - 1) {
                    BackgroundSyncDecision.Retry
                } else {
                    BackgroundSyncDecision.GiveUp
                }
            SyncOutcome.SessionExpired -> BackgroundSyncDecision.GiveUp
        }
    }
}
