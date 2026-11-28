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
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.UnenrollFromCourse
import es.ulpgc.pamn.dominio.course.progress.UnenrollOutcome
import es.ulpgc.pamn.dominio.course.progress.UnenrollResult
import es.ulpgc.pamn.dominio.data.course.CourseCopyRequester
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.ui.screens.course.problem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// La ficha de un curso: descarga el curso, escucha inscripción y progreso, y coordina inscripción
// y baja en línea. Recibe todos los puertos por constructor: no sabe si hay HTTP ni Room.
class CourseDetailViewModel(
    private val courseId: String,
    private val gateway: CourseGateway,
    private val enrollment: EnrollmentStore,
    private val progress: ProgressStore,
    private val progressSync: ProgressSyncRequester,
    private val courseCopy: CourseCopyRequester,
    private val unenroll: UnenrollFromCourse,
) : ViewModel() {

    companion object {
        fun factory(
            courseId: String,
            gateway: CourseGateway,
            enrollment: EnrollmentStore,
            progress: ProgressStore,
            progressSync: ProgressSyncRequester,
            courseCopy: CourseCopyRequester,
            unenroll: UnenrollFromCourse,
        ) = viewModelFactory {
            initializer {
                CourseDetailViewModel(
                    courseId = courseId,
                    gateway = gateway,
                    enrollment = enrollment,
                    progress = progress,
                    progressSync = progressSync,
                    courseCopy = courseCopy,
                    unenroll = unenroll,
                )
            }
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

    // Marca «inscribiéndose» antes de lanzar la corrutina (guardia contra doble toque en el hilo
    // principal). enrolling se apaga cuando el Flow emite enrolled = true (sin parpadeo).
    // enroll sin sesión lanza IllegalStateException (fallo de programación): no se captura.
    fun onEnroll() {
        val current = state as? CourseDetailState.Ready ?: return
        if (current.enrolled || current.enrolling) return
        state = current.copy(enrolling = true)
        viewModelScope.launch {
            enrollment.enroll(current.course.id)
            progressSync.requestSync()
            courseCopy.requestCopy(current.course.id)
        }
    }

    fun onOpenUnenrollDialog() {
        val current = state as? CourseDetailState.Ready ?: return
        if (!current.enrolled || current.unenrollDialog !is UnenrollDialogState.Closed) return
        state = current.copy(unenrollDialog = UnenrollDialogState.Confirming)
    }

    // Cancelar el diálogo. Mientras se envía no se puede: la petición ya ha salido.
    fun onDismissUnenrollDialog() {
        val current = state as? CourseDetailState.Ready ?: return
        if (current.unenrollDialog is UnenrollDialogState.Sending ||
            current.unenrollDialog is UnenrollDialogState.Closed
        ) return
        state = current.copy(unenrollDialog = UnenrollDialogState.Closed)
    }

    // Confirmar (o reintentar) la baja. Guardia contra doble envío en el hilo principal.
    fun onConfirmUnenroll() {
        val current = state as? CourseDetailState.Ready ?: return
        if (!current.enrolled) return
        if (current.unenrollDialog !is UnenrollDialogState.Confirming &&
            current.unenrollDialog !is UnenrollDialogState.Failed
        ) return
        state = current.copy(unenrollDialog = UnenrollDialogState.Sending)
        viewModelScope.launch {
            val nextDialog = dialogAfterUnenroll(unenroll.unenroll(current.course.id))
            updateReady { ready ->
                // Si dejó de estar inscrito mientras viajaba la petición, cerramos aunque fallase.
                val dialog = if (ready.enrolled) nextDialog else UnenrollDialogState.Closed
                ready.copy(unenrollDialog = dialog)
            }
        }
    }

    private fun load() {
        loading?.cancel()
        state = CourseDetailState.Loading
        loading = viewModelScope.launch {
            when (val outcome = gateway.course(courseId)) {
                is CourseOutcome.Found -> observeCourse(outcome.course)
                else -> state = CourseDetailState.Problem(outcome.problem())
            }
        }
    }

    // Espera a tener tanto `enrolled` como `mastered` antes de salir de Loading (sin parpadeo) y
    // conserva el estado local (`enrolling`, `unenrollDialog`) frente a emisiones posteriores de Room.
    private suspend fun observeCourse(course: Course) {
        combine(
            enrollment.enrolled(course.id),
            progress.mastered(course.id),
        ) { enrolled, mastered -> enrolled to mastered }
            .collect { (enrolled, mastered) ->
                val previous = state as? CourseDetailState.Ready
                state = CourseDetailState.Ready(
                    course = course,
                    mastered = mastered,
                    enrolled = enrolled,
                    enrolling = !enrolled && previous?.enrolling == true,
                    unenrollDialog = dialogAfterEmission(enrolled, previous?.unenrollDialog),
                )
            }
    }

    private fun updateReady(change: (CourseDetailState.Ready) -> CourseDetailState.Ready) {
        val current = state as? CourseDetailState.Ready ?: return
        state = change(current)
    }
}

private fun dialogAfterEmission(
    enrolled: Boolean,
    previous: UnenrollDialogState?,
): UnenrollDialogState = when {
    previous == null -> UnenrollDialogState.Closed
    enrolled -> previous
    previous is UnenrollDialogState.Sending -> previous
    else -> UnenrollDialogState.Closed
}

private fun dialogAfterUnenroll(result: UnenrollResult): UnenrollDialogState = when (result) {
    UnenrollResult.NoSession -> UnenrollDialogState.Closed
    is UnenrollResult.Finished -> when (result.outcome) {
        // SessionExpired: UnenrollFromCourse ya borró la sesión; GradusNavHost lleva a Login.
        UnenrollOutcome.Unenrolled,
        UnenrollOutcome.SessionExpired,
        -> UnenrollDialogState.Closed
        UnenrollOutcome.Unreachable -> UnenrollDialogState.Failed(FailureReason.UNREACHABLE)
        UnenrollOutcome.ServerFailure -> UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE)
    }
}
