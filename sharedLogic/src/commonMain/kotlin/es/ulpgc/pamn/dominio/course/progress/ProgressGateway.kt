package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session

// Puerto de la sincronización del progreso con el servidor (backend/docs/api-progreso.md).
// La implementación real es HttpProgressGateway; FakeProgressGateway sirve a los tests.
// Nunca lanza por un fallo de red o del servidor: lo devuelve como SyncOutcome (sin textos: la
// interfaz decide qué decir).
interface ProgressGateway {

    // POST /api/progress/sync: sube `pending` y devuelve todo el progreso del usuario en el servidor.
    // La sesión va por parámetro (decisión 11): su token viaja en Authorization: Bearer.
    suspend fun sync(session: Session, pending: ProgressSnapshot): SyncOutcome
}

sealed interface SyncOutcome {
    // 200: el progreso completo del servidor y lo que no guardó, ya en el modelo de la app.
    data class Synced(val response: SyncResponse) : SyncOutcome

    // 401: la sesión ya no vale (caducada o cerrada). Reintentar con la misma no sirve.
    data object SessionExpired : SyncOutcome

    // Sin conexión, servidor apagado o tiempo agotado. Lo pendiente se puede reintentar.
    data object Unreachable : SyncOutcome

    // Cualquier otra cosa: 400, 413, 5xx, un estado desconocido o un 200 que no se puede leer.
    data object ServerFailure : SyncOutcome
}
