package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.course.masteredCount
import es.ulpgc.pamn.dominio.course.missingFor
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.stateOf
import es.ulpgc.pamn.dominio.course.statesFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Lo que pinta la pantalla del camino. Inmutable: cada cambio es una copia nueva (copy).
data class CourseUiState(
    val course: Course,
    val mastered: Set<String>,
    val notice: CourseNotice? = null,
    // Módulo cuya práctica hay que abrir (evento: el NavHost navega y lo borra con onPracticeOpened).
    val practiceToOpen: String? = null,
) {
    val states: Map<String, ModuleState> get() = course.statesFor(mastered)
    val masteredCount: Int get() = course.masteredCount(mastered)
}

// La pantalla entera: descargando, con un problema o con el camino listo.
sealed interface CourseScreenState {
    data object Loading : CourseScreenState
    data class Problem(val problem: CourseProblem) : CourseScreenState
    data class Ready(val ui: CourseUiState) : CourseScreenState
}

// Por qué no se puede enseñar el curso. Solo algunos se arreglan reintentando.
enum class CourseProblem(val canRetry: Boolean) {
    NOT_FOUND(canRetry = false),
    UNREACHABLE(canRetry = true),
    UNSUPPORTED_FORMAT(canRetry = false),
    SERVER_FAILURE(canRetry = true),
}

// Por qué no se ha podido descargar el curso. La usan el camino y la ficha del curso, que
// descargan lo mismo. Solo para fallos: un curso encontrado no es un problema.
fun CourseOutcome.problem(): CourseProblem = when (this) {
    is CourseOutcome.Found -> error("Un curso encontrado no es un problema")
    CourseOutcome.NotFound -> CourseProblem.NOT_FOUND
    CourseOutcome.Unreachable -> CourseProblem.UNREACHABLE
    CourseOutcome.UnsupportedFormat -> CourseProblem.UNSUPPORTED_FORMAT
    CourseOutcome.ServerFailure -> CourseProblem.SERVER_FAILURE
}

// El aviso que sale abajo (Snackbar) al tocar un módulo bloqueado.
sealed interface CourseNotice {
    data class Locked(val module: Module, val missing: List<Module>) : CourseNotice
}

// Descarga el curso de la ruta y escucha el progreso: al dominar un módulo, el camino se
// actualiza solo. Recibe los puertos por constructor (no sabe si hay HTTP ni dónde se guarda el progreso).
class CourseViewModel(
    private val courseId: String,
    private val gateway: CourseGateway,
    private val progress: ProgressStore,
) : ViewModel() {

    companion object {
        fun factory(courseId: String, gateway: CourseGateway, progress: ProgressStore) = viewModelFactory {
            initializer { CourseViewModel(courseId, gateway, progress) }
        }
    }

    var state by mutableStateOf<CourseScreenState>(CourseScreenState.Loading)
        private set

    private var loading: Job? = null

    init {
        load()
    }

    fun onRetry() {
        val current = state
        if (current is CourseScreenState.Problem && current.problem.canRetry) load()
    }

    fun onModuleClick(moduleId: String) = updateReady { ui ->
        val module = ui.course.module(moduleId) ?: return@updateReady ui
        when (ui.course.stateOf(moduleId, ui.mastered)) {
            ModuleState.LOCKED -> ui.copy(notice = CourseNotice.Locked(module, ui.course.missingFor(moduleId, ui.mastered)))
            // Frontera: practicar. Dominado: repasar. Las dos abren la práctica del módulo.
            ModuleState.FRONTIER, ModuleState.MASTERED -> ui.copy(practiceToOpen = moduleId)
        }
    }

    fun onPracticeOpened() = updateReady { it.copy(practiceToOpen = null) }

    // La pantalla avisa cuando el Snackbar ya se ha ido, para no volver a enseñarlo al girar.
    fun onNoticeShown() = updateReady { it.copy(notice = null) }

    private fun load() {
        loading?.cancel()
        state = CourseScreenState.Loading
        loading = viewModelScope.launch {
            when (val outcome = gateway.course(courseId)) {
                is CourseOutcome.Found -> showWithProgress(outcome.course)
                else -> state = CourseScreenState.Problem(outcome.problem())
            }
        }
    }

    // collect se queda escuchando mientras viva el ViewModel: cada cambio de progreso repinta el
    // camino, conservando el aviso o la práctica pendiente que hubiera.
    private suspend fun showWithProgress(course: Course) {
        progress.mastered(course.id).collect { mastered ->
            val previous = (state as? CourseScreenState.Ready)?.ui
            state = CourseScreenState.Ready(previous?.copy(mastered = mastered) ?: CourseUiState(course, mastered))
        }
    }

    private inline fun updateReady(change: (CourseUiState) -> CourseUiState) {
        val current = state as? CourseScreenState.Ready ?: return
        state = CourseScreenState.Ready(change(current.ui))
    }
}
