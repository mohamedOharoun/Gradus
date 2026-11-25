package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.StudyAccess
import es.ulpgc.pamn.dominio.course.progress.studyAccess
import es.ulpgc.pamn.dominio.course.study.ModuleStudy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

// Guarda el estudio de un módulo mientras dura la pantalla (sobrevive al giro). Toda la lógica
// está en ModuleStudy (sharedLogic): aquí solo se le pasan los toques y, al aprobar la prueba de
// dominio, se apunta en el progreso (el camino lo escucha y desbloquea lo siguiente).
// Más adelante, al comprobar, guardará cada respuesta en el registro (Room, §4.4).
class ExerciseViewModel(
    course: Course,
    moduleId: String,
    private val progress: ProgressStore,
    enrollment: EnrollmentStore,
    private val random: Random,
) : ViewModel() {

    companion object {
        fun factory(
            course: Course,
            moduleId: String,
            progress: ProgressStore,
            enrollment: EnrollmentStore,
            random: Random = Random.Default,
        ) = viewModelFactory { initializer { ExerciseViewModel(course, moduleId, progress, enrollment, random) } }
    }

    // La puerta: null mientras se lee la inscripción (no se enseña nada); NotEnrolled, la pantalla
    // vuelve atrás sin componer la práctica; Allowed, se enseña. Basta con el primer valor: no hay
    // «darse de baja» (decisión 7), así que quien está inscrito al abrir lo sigue estando.
    var access by mutableStateOf<StudyAccess?>(null)
        private set

    // null: el módulo no existe en el curso (la pantalla vuelve al camino).
    var study by mutableStateOf(ModuleStudy.start(course, moduleId, random))
        private set

    init {
        viewModelScope.launch { access = studyAccess(enrollment.enrolled(course.id).first()) }
    }

    fun onChoose(option: Int) = update { it.choose(option) }
    fun onLeftClick(index: Int) = update { it.tapLeft(index) }
    fun onRightClick(index: Int) = update { it.tapRight(index) }
    fun onCheck() = update { it.check() }

    fun onContinue() {
        val current = study ?: return
        val next = current.next(random)
        study = next
        // Solo en el momento de aprobar (no en cada «Continuar» posterior).
        if (next.isPassed && !current.isPassed) {
            viewModelScope.launch { progress.markMastered(next.course.id, next.module.id) }
        }
    }

    fun onStartTest() {
        study = study?.startTest(random)
    }

    fun onRetry() {
        study = study?.retry(random)
    }

    private fun update(change: (ExerciseState) -> ExerciseState) {
        study = study?.update(change)
    }
}
