package es.ulpgc.pamn.dominio.course.study

import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.course.exercise.startExercise
import kotlin.random.Random

// Un paso de la parte formativa mientras se estudia: una lección que se lee o un ejercicio que se responde.
sealed interface StepState {
    // ¿Se puede pasar al siguiente? Una lección, siempre; un ejercicio, una vez comprobado.
    val isDone: Boolean

    data class Reading(val lesson: Step.Lesson) : StepState {
        override val isDone: Boolean get() = true
    }

    data class Answering(val exercise: ExerciseState) : StepState {
        override val isDone: Boolean get() = exercise.isChecked
    }
}

// La parte formativa de un módulo: sus pasos en orden. Inmutable, como PracticeSession, pero con
// lecciones además de ejercicios. Aquí fallar no importa: se enseña la explicación y se sigue.
data class StepSession(val steps: List<StepState>, val index: Int = 0) {

    companion object {
        fun start(steps: List<Step>, random: Random) = StepSession(
            steps.map { step ->
                when (step) {
                    is Step.Lesson -> StepState.Reading(step)
                    is Step.Exercise -> StepState.Answering(step.question.startExercise(random))
                }
            },
        )
    }

    val current: StepState? get() = steps.getOrNull(index)
    val total: Int get() = steps.size
    val position: Int get() = minOf(index + 1, total) // «paso 2 de 5»
    val isFinished: Boolean get() = index >= total
    val canContinue: Boolean get() = current?.isDone == true

    // Cambia el ejercicio actual; si el paso actual es una lección, no hace nada.
    fun update(change: (ExerciseState) -> ExerciseState): StepSession {
        val answering = current as? StepState.Answering ?: return this
        return copy(steps = steps.mapIndexed { i, step -> if (i == index) StepState.Answering(change(answering.exercise)) else step })
    }

    fun next(): StepSession = if (canContinue) copy(index = index + 1) else this
}
