package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// ProgressStore en memoria: el progreso dura mientras la app está abierta (como InMemorySessionStore).
// Un solo MutableStateFlow con todos los cursos: id del curso → módulos dominados.
class InMemoryProgressStore : ProgressStore {

    private val progress = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    // map saca el curso pedido; distinctUntilChanged evita avisar cuando cambia otro curso.
    override fun mastered(courseId: String): Flow<Set<String>> =
        progress.map { it[courseId].orEmpty() }.distinctUntilChanged()

    // update cambia el valor de forma atómica: dos llamadas a la vez no se pisan.
    override suspend fun markMastered(courseId: String, moduleId: String) {
        progress.update { all -> all + (courseId to all[courseId].orEmpty() + moduleId) }
    }
}
