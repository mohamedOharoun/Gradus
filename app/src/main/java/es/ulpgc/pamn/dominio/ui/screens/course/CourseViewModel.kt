package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.masteredCount
import es.ulpgc.pamn.dominio.course.missingFor
import es.ulpgc.pamn.dominio.course.stateOf
import es.ulpgc.pamn.dominio.course.statesFor

// Lo que pinta la pantalla del camino. Inmutable: cada cambio es una copia nueva (copy).
data class CourseUiState(
    val course: Course,
    val states: Map<String, ModuleState>,
    val masteredCount: Int,
    val notice: CourseNotice? = null,
    // Módulo cuya práctica hay que abrir (evento: el NavHost navega y lo borra con onPracticeOpened).
    val practiceToOpen: String? = null,
)

// El aviso que sale abajo (Snackbar) al tocar un módulo bloqueado.
sealed interface CourseNotice {
    data class Locked(val module: Module, val missing: List<Module>) : CourseNotice
}

// Recibe el curso y lo dominado por constructor. Con el backend (y Room) recibirá un gateway y un
// repositorio, como RegisterViewModel recibe el AccountGateway: la pantalla no cambiará.
class CourseViewModel(private val course: Course, private val mastered: Set<String>) : ViewModel() {

    companion object {
        fun factory(course: Course, mastered: Set<String>) = viewModelFactory {
            initializer { CourseViewModel(course, mastered) }
        }
    }

    var state by mutableStateOf(
        CourseUiState(course, course.statesFor(mastered), course.masteredCount(mastered)),
    )
        private set

    fun onModuleClick(moduleId: String) {
        val module = course.module(moduleId) ?: return
        state = when (course.stateOf(moduleId, mastered)) {
            ModuleState.LOCKED -> state.copy(notice = CourseNotice.Locked(module, course.missingFor(moduleId, mastered)))
            // Frontera: practicar. Dominado: repasar. Las dos abren la práctica del módulo.
            ModuleState.FRONTIER, ModuleState.MASTERED -> state.copy(practiceToOpen = moduleId)
        }
    }

    fun onPracticeOpened() {
        state = state.copy(practiceToOpen = null)
    }

    // La pantalla avisa cuando el Snackbar ya se ha ido, para no volver a enseñarlo al girar.
    fun onNoticeShown() {
        state = state.copy(notice = null)
    }
}
