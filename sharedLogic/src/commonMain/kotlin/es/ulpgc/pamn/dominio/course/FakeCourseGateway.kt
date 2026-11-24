package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.searchKey

// Gateway falso, sin red: para previews, tests de los ViewModel y probar la app sin backend.
// Por defecto busca y devuelve los cursos que recibe; también se le puede decir qué responder.
class FakeCourseGateway(
    private val courses: List<Course> = emptyList(),
    private val searchAnswer: (query: CourseQuery, page: Int) -> SearchOutcome = { query, page -> searchIn(courses, query, page) },
    private val courseAnswer: (id: String) -> CourseOutcome = { id ->
        courses.firstOrNull { it.id == id }?.let(CourseOutcome::Found) ?: CourseOutcome.NotFound
    },
) : CourseGateway {

    // Lo que le han pedido, en orden (para comprobarlo en los tests).
    val searches: List<Pair<CourseQuery, Int>> get() = searchRequests.toList()
    val requestedCourses: List<String> get() = courseRequests.toList()
    private val searchRequests = mutableListOf<Pair<CourseQuery, Int>>()
    private val courseRequests = mutableListOf<String>()

    override suspend fun search(query: CourseQuery, page: Int): SearchOutcome {
        searchRequests += query to page
        return searchAnswer(query, page)
    }

    override suspend fun course(id: String): CourseOutcome {
        courseRequests += id
        return courseAnswer(id)
    }
}

private const val FAKE_PAGE_SIZE = 20

// Todos los cursos del gateway falso son de este autor.
private const val FAKE_AUTHOR_NAME = "Autor de prueba"

// Filtra por texto, etiquetas y creador. La fecha (`query.published`) se ignora (decisión G): sin reloj
// en commonMain, filtrarla exigiría kotlinx-datetime, una dependencia nueva.
private fun searchIn(courses: List<Course>, query: CourseQuery, page: Int): SearchOutcome {
    val matches = courses
        .filter { it.matchesText(query) && it.hasAllTagsOf(query) && matchesAuthor(query) }
        .map { it.toFakeSummary() }
    val items = matches.drop(page * FAKE_PAGE_SIZE).take(FAKE_PAGE_SIZE)
    return SearchOutcome.Found(CoursePage(items = items, page = page, size = FAKE_PAGE_SIZE, total = matches.size))
}

private fun Course.matchesText(query: CourseQuery): Boolean {
    if (!query.hasText) return true
    val text = query.text.trim()
    return title.contains(text, ignoreCase = true) || description.contains(text, ignoreCase = true)
}

// Las etiquetas se comparan con la misma regla que CourseQuery (sin copiarla).
private fun Course.hasAllTagsOf(query: CourseQuery): Boolean {
    val own = tags.map { it.searchKey() }.toSet()
    return query.tags.all { it.searchKey() in own }
}

// El creador es una subcadena del nombre, con la misma regla que CourseQuery (sin mayúsculas ni espacios
// de sobra, sin quitar tildes). En blanco no filtra.
private fun matchesAuthor(query: CourseQuery): Boolean =
    !query.hasAuthor || FAKE_AUTHOR_NAME.searchKey().contains(query.author.searchKey())

private fun Course.toFakeSummary() = CourseSummary(
    id = id,
    version = 1,
    author = Author(id = "fake-author", name = FAKE_AUTHOR_NAME),
    publishedAt = "2026-01-01T00:00:00Z",
    title = title,
    description = description,
    language = language,
    tags = tags,
    moduleCount = modules.size,
)
