package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.learningOrder
import es.ulpgc.pamn.dominio.course.masteredCount
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem
import kotlin.math.roundToInt

// Estado del diálogo de baja en línea (tipo suma: no se puede estar enviando y con error a la vez).
sealed interface UnenrollDialogState {
    data object Closed : UnenrollDialogState
    data object Confirming : UnenrollDialogState
    data object Sending : UnenrollDialogState
    data class Failed(val reason: FailureReason) : UnenrollDialogState
}

// Lo que pinta la ficha de un curso: descargando, con un problema o lista. Los problemas son los
// mismos que los del camino (CourseProblem): la ficha descarga el mismo curso.
sealed interface CourseDetailState {
    data object Loading : CourseDetailState

    data class Problem(val problem: CourseProblem) : CourseDetailState

    // `mastered`: ids de los módulos dominados (del ProgressStore); `enrolled`: del EnrollmentStore.
    data class Ready(
        val course: Course,
        val mastered: Set<String> = emptySet(),
        val enrolled: Boolean = false,
        val enrolling: Boolean = false,
        val unenrollDialog: UnenrollDialogState = UnenrollDialogState.Closed,
    ) : CourseDetailState {
        // «Qué vas a aprender», en el orden del camino. Se calcula una vez, al crear el estado.
        val modules: List<Module> = course.learningOrder()
        val totalModules: Int get() = course.modules.size
        val masteredCount: Int get() = course.masteredCount(mastered)
        val progressFraction: Float
            get() = if (totalModules == 0) 0f else masteredCount.toFloat() / totalModules
        val progressPercent: Int get() = (progressFraction * 100f).roundToInt()

        fun isMastered(module: Module): Boolean = module.id in mastered
    }
}

// Lo que la ficha avisa hacia fuera (objeto de parámetros, como CourseActions).
data class CourseDetailActions(
    val onEnroll: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onOpenUnenrollDialog: () -> Unit = {},
    val onConfirmUnenroll: () -> Unit = {},
    val onDismissUnenrollDialog: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onBack: () -> Unit = {},
)
