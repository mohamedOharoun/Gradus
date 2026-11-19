package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.course.exercise.PracticeSession
import kotlin.random.Random

// Lo que pinta la pantalla de práctica.
data class ExerciseUiState(val conceptTitle: String, val session: PracticeSession)

// Guarda la práctica mientras dura la pantalla (sobrevive al giro). Toda la lógica está en
// PracticeSession/ExerciseState (sharedLogic): aquí solo se le pasan los toques.
// Más adelante, al comprobar, guardará cada respuesta en el registro (Room, §4.4).
class ExerciseViewModel(course: Course, conceptId: String, random: Random) : ViewModel() {

    companion object {
        fun factory(course: Course, conceptId: String, random: Random = Random.Default) = viewModelFactory {
            initializer { ExerciseViewModel(course, conceptId, random) }
        }
    }

    var state by mutableStateOf(
        ExerciseUiState(
            conceptTitle = course.concept(conceptId)?.title.orEmpty(),
            session = PracticeSession.start(course.questionsFor(conceptId), random),
        ),
    )
        private set

    fun onChoose(option: Int) = update { it.choose(option) }
    fun onLeftClick(index: Int) = update { it.tapLeft(index) }
    fun onRightClick(index: Int) = update { it.tapRight(index) }
    fun onCheck() = update { it.check() }

    fun onContinue() {
        state = state.copy(session = state.session.next())
    }

    private fun update(change: (ExerciseState) -> ExerciseState) {
        state = state.copy(session = state.session.update(change))
    }
}
