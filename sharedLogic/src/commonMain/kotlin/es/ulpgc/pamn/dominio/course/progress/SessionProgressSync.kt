package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.clearIfCurrent

// Caso de uso: sincroniza el progreso con la sesión guardada y trata el 401.
// - Sin sesión guardada no llama al servidor (NoSession).
// - Con SessionExpired borra la sesión, pero solo si sigue siendo la que recibió el 401
//   (clearIfCurrent): no echa a quien acaba de iniciar sesión mientras la petición viajaba.
// - Con Synced, Unreachable o ServerFailure no toca la sesión. Devuelve el desenlace tal cual.
// Es el único sitio que trata el 401; la interfaz solo reacciona a «la sesión se ha borrado».
class SessionProgressSync(private val sessions: SessionStore, private val sync: ProgressSync) {

    suspend fun run(): SessionSyncResult {
        val session = sessions.current() ?: return SessionSyncResult.NoSession
        val outcome = sync.sync(session)
        if (outcome is SyncOutcome.SessionExpired) sessions.clearIfCurrent(session)
        return SessionSyncResult.Finished(outcome)
    }
}
