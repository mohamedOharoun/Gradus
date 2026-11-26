package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.learningOrder
import es.ulpgc.pamn.dominio.course.masteredCount
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem

// Lo que pinta la ficha de un curso: descargando, con un problema o lista. Los problemas son los
// mismos que los del camino (CourseProblem): la ficha descarga el mismo curso.
sealed interface CourseDetailState {
    data object Loading : CourseDetailState

    data class Problem(val problem: CourseProblem) : CourseDetailState

    // `mastered`: ids de los módulos dominados (del ProgressStore), para el botón y la lista.
    data class Ready(val course: Course, val mastered: Set<String> = emptySet()) : CourseDetailState {
        // «Qué vas a aprender», en el orden del camino. Se calcula una vez, al crear el estado.
        val modules: List<Module> = course.learningOrder()
        val masteredCount: Int get() = course.masteredCount(mastered)

        fun isMastered(module: Module): Boolean = module.id in mastered
    }
}

// Lo que la ficha avisa hacia fuera (objeto de parámetros, como CourseActions).
data class CourseDetailActions(
    val onStart: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onBack: () -> Unit = {},
)
