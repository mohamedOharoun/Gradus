package es.ulpgc.pamn.dominio.ui.screens.detail

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
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.ui.screens.course.problem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// La ficha de un curso: descarga el curso de la ruta y escucha el progreso, como CourseViewModel.
// Con CachingCourseGateway, al pulsar «Empezar el curso» el camino ya no lo vuelve a descargar.
// Recibe los puertos por constructor: no sabe si hay HTTP ni dónde se guarda el progreso.
class CourseDetailViewModel(
    private val courseId: String,
    private val gateway: CourseGateway,
    private val progress: ProgressStore,
) : ViewModel() {

    companion object {
        fun factory(courseId: String, gateway: CourseGateway, progress: ProgressStore) = viewModelFactory {
            initializer { CourseDetailViewModel(courseId, gateway, progress) }
        }
    }

    var state by mutableStateOf<CourseDetailState>(CourseDetailState.Loading)
        private set

    private var loading: Job? = null

    init {
        load()
    }

    // «Reintentar»: solo si falló y reintentar puede arreglarlo (sin conexión o fallo del servidor).
    fun onRetry() {
        val current = state
        if (current is CourseDetailState.Problem && current.problem.canRetry) load()
    }

    private fun load() {
        loading?.cancel()
        state = CourseDetailState.Loading
        loading = viewModelScope.launch {
            when (val outcome = gateway.course(courseId)) {
                is CourseOutcome.Found -> showWithProgress(outcome.course)
                else -> state = CourseDetailState.Problem(outcome.problem())
            }
        }
    }

    // Se queda escuchando mientras viva el ViewModel: al volver del camino tras dominar un módulo,
    // la ficha ya enseña el progreso nuevo.
    private suspend fun showWithProgress(course: Course) {
        progress.mastered(course.id).collect { mastered -> state = CourseDetailState.Ready(course, mastered) }
    }
}
