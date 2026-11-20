package es.ulpgc.pamn.dominio.course.exercise

import kotlin.random.Random

// Una práctica: los ejercicios de un módulo, uno detrás de otro. `index` es el actual; cuando
// pasa del último, la práctica ha terminado y se enseña el resumen. Inmutable, como ExerciseState.
// Más adelante, cada ejercicio comprobado se guardará en el registro de respuestas (Room, §4.4).
data class PracticeSession(
    val exercises: List<ExerciseState>,
    val index: Int = 0,
) {
    companion object {
        fun start(questions: List<Question>, random: Random) =
            PracticeSession(questions.map { it.startExercise(random) })
    }

    val current: ExerciseState? get() = exercises.getOrNull(index)
    val total: Int get() = exercises.size
    val position: Int get() = minOf(index + 1, total) // «pregunta 2 de 5»
    val isFinished: Boolean get() = index >= total
    val completedCount: Int get() = exercises.count { it.isChecked }
    val correctCount: Int get() = exercises.count { it.correct == true }
    val canContinue: Boolean get() = current?.isChecked == true

    // Cambia el ejercicio actual, por ejemplo: session.update { it.choose(1) }.
    fun update(change: (ExerciseState) -> ExerciseState): PracticeSession =
        if (isFinished) this
        else copy(exercises = exercises.mapIndexed { i, exercise -> if (i == index) change(exercise) else exercise })

    fun next(): PracticeSession = if (canContinue) copy(index = index + 1) else this
}
