package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.SearchOutcome
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import kotlinx.coroutines.delay
import kotlin.time.Duration

// Solo para tests. Decorator: pasa la petición al gateway de dentro al momento (que la apunta en
// `searches`), pero devuelve la respuesta con retraso, como una red lenta. Así se puede teclear o
// pulsar algo mientras una petición está en camino. `answered` apunta las búsquedas que llegaron a
// responderse: una petición cancelada a medio camino no aparece.
class DelayedCourseGateway(
    private val inner: CourseGateway,
    private val latency: Duration,
) : CourseGateway {

    val answered: List<Pair<CourseQuery, Int>> get() = answeredSearches.toList()
    private val answeredSearches = mutableListOf<Pair<CourseQuery, Int>>()

    override suspend fun search(query: CourseQuery, page: Int): SearchOutcome {
        val outcome = inner.search(query, page)
        delay(latency)
        answeredSearches += query to page
        return outcome
    }

    override suspend fun course(id: String): CourseOutcome {
        val outcome = inner.course(id)
        delay(latency)
        return outcome
    }
}
