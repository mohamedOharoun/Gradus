package es.ulpgc.pamn.dominio.course

// Gateway falso, sin red: para previews, tests de los ViewModel y probar la app sin backend.
// Por defecto busca y devuelve los cursos que recibe; también se le puede decir qué responder.
class FakeCourseGateway(
    private val courses: List<Course> = emptyList(),
    private val searchAnswer: (query: String, page: Int) -> SearchOutcome = { query, page -> searchIn(courses, query, page) },
    private val courseAnswer: (id: String) -> CourseOutcome = { id ->
        courses.firstOrNull { it.id == id }?.let(CourseOutcome::Found) ?: CourseOutcome.NotFound
    },
) : CourseGateway {

    // Lo que le han pedido, en orden (para comprobarlo en los tests).
    val searches: List<Pair<String, Int>> get() = searchRequests.toList()
    val requestedCourses: List<String> get() = courseRequests.toList()
    private val searchRequests = mutableListOf<Pair<String, Int>>()
    private val courseRequests = mutableListOf<String>()

    override suspend fun search(query: String, page: Int): SearchOutcome {
        searchRequests += query to page
        return searchAnswer(query, page)
    }

    override suspend fun course(id: String): CourseOutcome {
        courseRequests += id
        return courseAnswer(id)
    }
}

private const val FAKE_PAGE_SIZE = 20

private fun searchIn(courses: List<Course>, query: String, page: Int): SearchOutcome {
    val matches = courses
        .filter { query.isBlank() || query.trim().let { q -> it.title.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) } }
        .map { it.toFakeSummary() }
    val items = matches.drop(page * FAKE_PAGE_SIZE).take(FAKE_PAGE_SIZE)
    return SearchOutcome.Found(CoursePage(items = items, page = page, size = FAKE_PAGE_SIZE, total = matches.size))
}

private fun Course.toFakeSummary() = CourseSummary(
    id = id,
    version = 1,
    author = Author(id = "fake-author", name = "Autor de prueba"),
    publishedAt = "2026-01-01T00:00:00Z",
    title = title,
    description = description,
    language = language,
    tags = tags,
    moduleCount = modules.size,
)
