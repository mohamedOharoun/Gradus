package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.http.CourseDecoding
import es.ulpgc.pamn.dominio.course.http.decodeCourseVersion
import es.ulpgc.pamn.dominio.course.http.encodeCourseVersion
import es.ulpgc.pamn.dominio.course.search.CourseQuery

// Patrón Decorator: envuelve un CourseGateway y añade copia local para abrir cursos sin red.
// Estrategia «red primero»: siempre consulta al delegate; con Found guarda la copia bajo el `id`
// pedido (sobrescribiendo la anterior) y con Unreachable intenta leerla. El resto de desenlaces y
// search pasan tal cual sin tocar lo guardado. Los fallos del almacén se propagan sin tragarse.
class OfflineCourseGateway(
    private val delegate: CourseGateway,
    private val copies: CourseCopyStore,
) : CourseGateway {

    override suspend fun search(query: CourseQuery, page: Int): SearchOutcome =
        delegate.search(query, page)

    override suspend fun course(id: String): CourseOutcome = when (val outcome = delegate.course(id)) {
        is CourseOutcome.Found -> {
            copies.save(id, encodeCourseVersion(outcome.course))
            outcome
        }
        CourseOutcome.Unreachable -> loadCopy(id) ?: CourseOutcome.Unreachable
        CourseOutcome.NotFound,
        CourseOutcome.UnsupportedFormat,
        CourseOutcome.ServerFailure,
        -> outcome
    }

    private suspend fun loadCopy(id: String): CourseOutcome.Found? {
        val json = copies.load(id) ?: return null
        return when (val decoded = decodeCourseVersion(json)) {
            is CourseDecoding.Decoded -> CourseOutcome.Found(decoded.course)
            is CourseDecoding.Malformed,
            is CourseDecoding.UnsupportedFormat,
            -> null
        }
    }
}
