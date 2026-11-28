package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.search.CourseQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Patrón Decorator: es un CourseGateway que envuelve a otro y le añade una caché en memoria de los
// cursos descargados. Quien lo usa no nota la diferencia, salvo que el segundo course(id) no va a
// la red. Así el camino y la práctica comparten el curso, y cached(id) lo da sin esperar.
// Dura mientras la app está abierta: una versión nueva del curso se verá al volver a abrirla
// (guardar cursos en el móvil, con Room, es otro documento).
class CachingCourseGateway(private val delegate: CourseGateway) : CourseGateway {

    private val courses = MutableStateFlow<Map<String, Course>>(emptyMap())

    // El curso ya descargado, o null si todavía no se ha descargado.
    fun cached(id: String): Course? = courses.value[id]

    override suspend fun search(query: CourseQuery, page: Int): SearchOutcome = delegate.search(query, page)

    override suspend fun course(id: String): CourseOutcome {
        cached(id)?.let { return CourseOutcome.Found(it) }
        val outcome = delegate.course(id)
        if (outcome is CourseOutcome.Found) courses.update { it + (id to outcome.course) }
        return outcome
    }
}
