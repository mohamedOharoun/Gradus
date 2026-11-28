package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.clearIfCurrent

// Caso de uso: da de baja en línea al estudiante de la sesión actual en un curso.
// - Sin sesión guardada no llama al servidor ni al almacén local (NoSession).
// - Pregunta una sola vez al servidor antes de borrar nada en local (decisión B-3).
// - Con Unenrolled borra el curso del usuario original (session.account.id), incluso si la sesión
//   cambió o se cerró mientras la petición viajaba (decisión B-2).
// - Con SessionExpired borra la sesión solo si sigue siendo la misma (clearIfCurrent), sin tocar
//   el progreso local.
// - Con Unreachable o ServerFailure no borra progreso local ni toca la sesión.
// - Excepciones del almacén y CancellationException no se capturan: se propagan limpiamente.
class UnenrollFromCourse(
    private val sessions: SessionStore,
    private val gateway: ProgressGateway,
    private val enrollments: EnrollmentStore,
) {

    suspend fun unenroll(courseId: String): UnenrollResult {
        val session = sessions.current() ?: return UnenrollResult.NoSession
        val outcome = gateway.unenroll(session, courseId)
        when (outcome) {
            UnenrollOutcome.Unenrolled -> enrollments.unenroll(session.account.id, courseId)
            UnenrollOutcome.SessionExpired -> sessions.clearIfCurrent(session)
            UnenrollOutcome.Unreachable, UnenrollOutcome.ServerFailure -> Unit
        }
        return UnenrollResult.Finished(outcome)
    }
}
