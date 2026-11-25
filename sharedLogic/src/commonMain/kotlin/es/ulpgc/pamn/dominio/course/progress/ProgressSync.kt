package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session

// Caso de uso: sube lo pendiente del usuario de la sesión y fusiona todo su progreso del servidor.
// Recibe los dos puertos (almacén y gateway): no sabe si son memoria, Room, HTTP o un Fake.
// - Llama SIEMPRE al gateway, también sin nada pendiente (B-10): así llega lo dominado en otro móvil.
// - Un solo intento, sin reintentos (tampoco tras un 401: con la misma sesión no serviría).
// - Solo con Synced se llama a settle, con el `sent` que de verdad se envió: lo marcado mientras la
//   petición viaja no está en él y sigue pendiente. Con SessionExpired, Unreachable o ServerFailure no
//   se toca el almacén. Devuelve el desenlace del gateway tal cual; la interfaz decide qué decir.
// - No lanza por la red ni por el servidor (eso llega como SyncOutcome). Si falla el almacén
//   (p. ej. Room), su excepción sí sale. La cancelación también se propaga.
// Concurrencia (B-11, sin Mutex): dos sync a la vez no pierden ni deshacen nada. Cada una solo saca
// de la cola SU `sent` (por clave), y la fusión es unión con la fecha mínima (una respuesta antigua no
// borra lo nuevo). El coste es una llamada repetida. Una cancelación antes de settle deja todo pendiente
// (settle es todo o nada): la siguiente sync lo reenvía, cosa inofensiva porque el servidor fusiona.
class ProgressSync(private val store: ProgressSyncStore, private val gateway: ProgressGateway) {

    suspend fun sync(session: Session): SyncOutcome {
        val userId = session.account.id // decisión 11: el usuario sale de la sesión del parámetro
        val sent = store.pending(userId)
        return gateway.sync(session, sent).also { outcome ->
            if (outcome is SyncOutcome.Synced) store.settle(userId, sent, outcome.response.remote)
        }
    }
}
