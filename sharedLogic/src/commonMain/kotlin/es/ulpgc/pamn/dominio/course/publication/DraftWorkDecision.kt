package es.ulpgc.pamn.dominio.course.publication

// Número máximo de intentos ante ServerFailure (0-based, igual que BackgroundSyncDecision):
// reintenta en intentos 0..3 y se rinde en el 5.º intento (attempt >= 4).
const val MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS: Int = 5

// Qué debe hacer el trabajo en segundo plano tras procesar un borrador.
// Independiente de Android/WorkManager: el Worker traducirá cada variante a su Result.
sealed interface DraftWorkDecision {
    data object Done : DraftWorkDecision
    data object Retry : DraftWorkDecision
    data object GiveUp : DraftWorkDecision
}
