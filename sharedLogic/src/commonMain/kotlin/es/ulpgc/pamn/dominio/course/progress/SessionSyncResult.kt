package es.ulpgc.pamn.dominio.course.progress

// Qué pasó al sincronizar con la sesión guardada: no había ninguna, o se intentó y hubo un desenlace.
sealed interface SessionSyncResult {
    // No hay sesión guardada: no se llamó al servidor.
    data object NoSession : SessionSyncResult

    // Se sincronizó con la sesión guardada; `outcome` es el desenlace de ProgressSync, tal cual.
    data class Finished(val outcome: SyncOutcome) : SessionSyncResult
}
