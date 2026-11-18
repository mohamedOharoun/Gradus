package es.ulpgc.pamn.dominio.account

import kotlinx.coroutines.flow.MutableStateFlow

// SessionStore en memoria: la sesión dura mientras la app está abierta.
// MutableStateFlow guarda un único valor y se puede leer y cambiar desde varias corrutinas a la vez
// sin que se mezclen (el valor se sustituye entero, nunca a medias).
class InMemorySessionStore : SessionStore {

    private val session = MutableStateFlow<Session?>(null)

    override suspend fun save(session: Session) {
        this.session.value = session
    }

    override suspend fun current(): Session? = session.value

    override suspend fun clear() {
        session.value = null
    }
}
