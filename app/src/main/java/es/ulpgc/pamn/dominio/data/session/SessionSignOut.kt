package es.ulpgc.pamn.dominio.data.session

import es.ulpgc.pamn.dominio.account.Session

// Puerto ligero en app/ (patrón Command, como ProgressSyncRequester): cerrar la sesión por decisión
// del usuario («Cerrar sesión» o cuenta eliminada), para distinguirlo de un 401. Vive en app/ porque
// elegir entre Bienvenida o Login es una regla de navegación, no del puerto SessionStore de sharedLogic.
fun interface SessionSignOut {
    suspend fun signOut()
}

// Lo que observa StartViewModel sobre la sesión guardada: o hay sesión, o no la hay indicando si la
// ausencia viene de una salida voluntaria del usuario o de otra causa (401, arranque, lectura fallida).
sealed interface SessionUpdate {
    data class Active(val session: Session) : SessionUpdate
    data class Missing(val voluntary: Boolean) : SessionUpdate
}
