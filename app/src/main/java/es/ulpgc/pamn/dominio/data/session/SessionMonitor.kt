package es.ulpgc.pamn.dominio.data.session

import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

// Envuelve un SessionStore y su Flow de cambios sin tocar el puerto SessionStore de sharedLogic
// (patrones Decorator + Interface Segregation):
// - como SessionStore: save() y clear() dejan la salida como no voluntaria (401, nueva sesión);
// - como SessionSignOut: signOut() marca la salida como voluntaria antes de borrar y la desmarca si falla;
// - updates: traduce cada emisión del Flow en SessionUpdate (Active | Missing(voluntary)).
class SessionMonitor(
    private val store: SessionStore,
    sessionFlow: Flow<Session?>,
) : SessionStore, SessionSignOut {

    private val voluntarySignOut = AtomicBoolean(false)

    val updates: Flow<SessionUpdate> = sessionFlow.map { session ->
        if (session != null) SessionUpdate.Active(session)
        else SessionUpdate.Missing(voluntary = voluntarySignOut.get())
    }

    override suspend fun save(session: Session) {
        voluntarySignOut.set(false)
        store.save(session)
    }

    override suspend fun current(): Session? = store.current()

    override suspend fun clear() {
        voluntarySignOut.set(false)
        store.clear()
    }

    override suspend fun signOut() {
        voluntarySignOut.set(true)
        try {
            store.clear()
        } catch (failure: Throwable) {
            voluntarySignOut.set(false)
            throw failure
        }
    }
}
