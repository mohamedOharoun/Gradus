package es.ulpgc.pamn.dominio.data.course

import es.ulpgc.pamn.dominio.course.CourseGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Patrón Command: implementa CourseCopyRequester lanzando gateway.course(courseId) en un ámbito
// de aplicación (appScope), así la descarga y el guardado en Room no se cancelan al salir de
// ScreenCourseOverview.
// Precarga oportunista (best effort): el gateway devuelve los fallos de red/servidor como
// CourseOutcome sin lanzar y se descartan; una Exception del almacén (p. ej. Room/disco) se
// captura para no cerrar la app tras inscribirse bien. CancellationException se relanza siempre.
class CourseCopyLauncher(
    private val gateway: CourseGateway,
    private val scope: CoroutineScope,
) : CourseCopyRequester {

    override fun requestCopy(courseId: String) {
        scope.launch {
            try {
                gateway.course(courseId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Si falla la precarga, al abrir el camino con red se volverá a intentar.
            }
        }
    }
}
