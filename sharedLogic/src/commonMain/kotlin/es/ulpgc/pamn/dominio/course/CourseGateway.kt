package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.search.CourseQuery

// Puerto (arquitectura hexagonal): lo que la app necesita del catálogo de cursos del servidor, sin
// decir cómo se habla con él. Lo implementan HttpCourseGateway (Ktor) y FakeCourseGateway.
// Nunca lanza excepciones: cualquier fallo llega como un desenlace.
interface CourseGateway {

    // Busca por texto en el título o la descripción y por etiquetas (el curso debe tener todas).
    // `page` empieza en 0. Sin texto ni etiquetas (CourseQuery.EMPTY) = todos.
    suspend fun search(query: CourseQuery, page: Int = 0): SearchOutcome

    // Descarga la última versión publicada de un curso, con su contenido.
    suspend fun course(id: String): CourseOutcome
}

// Atajo para buscar solo por texto.
suspend fun CourseGateway.search(text: String, page: Int = 0): SearchOutcome = search(CourseQuery(text = text), page)

sealed interface SearchOutcome {
    // 200: una página de resultados (puede estar vacía).
    data class Found(val page: CoursePage) : SearchOutcome

    // No se ha podido hablar con el servidor (sin red, apagado o tiempo agotado). Se puede reintentar.
    data object Unreachable : SearchOutcome

    // El servidor ha respondido algo inesperado (4xx, 5xx o un cuerpo que no se entiende).
    data object ServerFailure : SearchOutcome
}

sealed interface CourseOutcome {
    // 200: el curso, ya traducido al modelo de la app.
    data class Found(val course: Course) : CourseOutcome

    // 404 COURSE_NOT_FOUND: no existe (o se ha borrado).
    data object NotFound : CourseOutcome

    // El curso usa un formato más nuevo que esta versión de la app: hay que actualizarla.
    data object UnsupportedFormat : CourseOutcome

    data object Unreachable : CourseOutcome

    // Otra respuesta, o un curso que no se puede leer.
    data object ServerFailure : CourseOutcome
}
