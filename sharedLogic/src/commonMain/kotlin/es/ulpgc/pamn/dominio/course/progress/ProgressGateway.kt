package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session

// Puerto del progreso con el servidor (backend/docs/api-progreso.md).
// La implementación real es HttpProgressGateway; FakeProgressGateway sirve a los tests.
// Nunca lanza por un fallo de red o del servidor: lo devuelve como SyncOutcome o UnenrollOutcome
// (sin textos: la interfaz decide qué decir).
interface ProgressGateway {

    // POST /api/progress/sync: sube `pending` y devuelve todo el progreso del usuario en el servidor.
    // La sesión va por parámetro (decisión 11): su token viaja en Authorization: Bearer.
    suspend fun sync(session: Session, pending: ProgressSnapshot): SyncOutcome

    // DELETE /api/progress/enrollments/{courseId}: da de baja al usuario de la sesión en ese curso.
    // Es idempotente en el servidor (204 aunque ya no estuviera inscrito).
    suspend fun unenroll(session: Session, courseId: String): UnenrollOutcome
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

sealed interface UnenrollOutcome {
    // 204: la inscripción y sus módulos dominados han quedado eliminados en el servidor.
    data object Unenrolled : UnenrollOutcome

    // 401: la sesión ya no vale (caducada o cerrada).
    data object SessionExpired : UnenrollOutcome

    // Sin conexión, servidor apagado o tiempo agotado.
    data object Unreachable : UnenrollOutcome

    // Cualquier otra cosa: 404, 400, 5xx o un estado inesperado (p. ej. 200).
    data object ServerFailure : UnenrollOutcome
}
