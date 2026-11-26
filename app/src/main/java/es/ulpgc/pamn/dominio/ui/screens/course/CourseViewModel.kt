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
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ModuleTap
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.studyAccess
import es.ulpgc.pamn.dominio.course.progress.tapModule
import es.ulpgc.pamn.dominio.course.statesFor
import es.ulpgc.pamn.dominio.notes.InMemoryNotesStore
import es.ulpgc.pamn.dominio.notes.NotesStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// Lo que pinta la pantalla del camino. Inmutable: cada cambio es una copia nueva (copy).
data class CourseUiState(
    val course: Course,
    val mastered: Set<String>,
    val notice: CourseNotice? = null,
    // Por defecto, sin inscripción: si alguien lo olvida, la puerta queda cerrada.
    val enrolled: Boolean = false,
    // Módulo cuya práctica hay que abrir (evento: el NavHost navega y lo borra con onPracticeOpened).
    val practiceToOpen: String? = null,
    // Módulo con la hoja abierta (Estudiar / Repasar y «Mis notas»; notas-camara.md, decisión S).
    val sheet: String? = null,
    // Cuántas notas con cámara tiene cada módulo («Mis notas · 3»). Sin notas, no está.
    val noteCounts: Map<String, Int> = emptyMap(),
    // Módulo cuyas notas hay que abrir (evento, como practiceToOpen: se borra con onNotesOpened).
    val notesToOpen: String? = null,
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

// El aviso que sale abajo (Snackbar) al tocar un módulo que no se puede abrir.
sealed interface CourseNotice {
    data class Locked(val module: Module, val missing: List<Module>) : CourseNotice
    data object NotEnrolled : CourseNotice
}

// Descarga el curso de la ruta y escucha el progreso, la inscripción y las notas: al dominar un
// módulo, inscribirse o guardar una nota, el camino se actualiza solo. Recibe los puertos por
// constructor (no sabe si hay HTTP ni dónde se guarda nada).
class CourseViewModel(
    private val courseId: String,
    private val gateway: CourseGateway,
    private val progress: ProgressStore,
    private val enrollment: EnrollmentStore,
    // Por defecto, sin notas (previews y pruebas que no las usan).
    private val notes: NotesStore = InMemoryNotesStore(),
) : ViewModel() {

    companion object {
        fun factory(
            courseId: String,
            gateway: CourseGateway,
            progress: ProgressStore,
            enrollment: EnrollmentStore,
            notes: NotesStore = InMemoryNotesStore(),
        ) = viewModelFactory {
            initializer { CourseViewModel(courseId, gateway, progress, enrollment, notes) }
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

    // Sin reglas propias: lo decide tapModule (sharedLogic) con lo dominado y la inscripción.
    // Inscrito y desbloqueado (frontera o dominado): abre la hoja del módulo (Estudiar / Mis notas).
    fun onModuleClick(moduleId: String) = updateReady { ui ->
        val module = ui.course.module(moduleId) ?: return@updateReady ui
        when (val tap = ui.course.tapModule(moduleId, ui.mastered, studyAccess(ui.enrolled))) {
            ModuleTap.OpenPractice -> ui.copy(sheet = moduleId)
            is ModuleTap.Locked -> ui.copy(notice = CourseNotice.Locked(module, tap.missing))
            ModuleTap.NotEnrolled -> ui.copy(notice = CourseNotice.NotEnrolled)
            null -> ui
        }
    }

    fun onPracticeOpened() = updateReady { it.copy(practiceToOpen = null) }

    // Cerrar la hoja sin elegir nada (deslizarla hacia abajo o tocar fuera).
    fun onSheetDismiss() = updateReady { it.copy(sheet = null) }

    // «Estudiar» o «Repasar» en la hoja: se cierra y se abre la práctica de su módulo.
    fun onStudy() = updateReady { ui -> ui.sheet?.let { ui.copy(sheet = null, practiceToOpen = it) } ?: ui }

    // «Mis notas» en la hoja: se cierra y se abren las notas de su módulo.
    fun onOpenNotes() = updateReady { ui -> ui.sheet?.let { ui.copy(sheet = null, notesToOpen = it) } ?: ui }

    fun onNotesOpened() = updateReady { it.copy(notesToOpen = null) }

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

    // combine emite cuando cambia cualquiera de los tres, con el último valor de todos (y no emite
    // hasta tener los tres: mientras, sigue Loading). collect se queda escuchando mientras viva el
    // ViewModel, conservando el aviso, la hoja o el evento pendiente que hubiera.
    private suspend fun showWithProgress(course: Course) {
        combine(
            progress.mastered(course.id),
            enrollment.enrolled(course.id),
            notes.counts(course.id),
        ) { mastered, enrolled, counts -> Triple(mastered, enrolled, counts) }
            .collect { (mastered, enrolled, counts) ->
                val previous = (state as? CourseScreenState.Ready)?.ui
                val ui = previous?.copy(mastered = mastered, enrolled = enrolled, noteCounts = counts)
                    ?: CourseUiState(course, mastered, enrolled = enrolled, noteCounts = counts)
                state = CourseScreenState.Ready(ui)
            }
    }

    private inline fun updateReady(change: (CourseUiState) -> CourseUiState) {
        val current = state as? CourseScreenState.Ready ?: return
        state = CourseScreenState.Ready(change(current.ui))
    }
}
