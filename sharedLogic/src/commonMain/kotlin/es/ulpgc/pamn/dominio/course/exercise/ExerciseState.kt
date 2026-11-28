package es.ulpgc.pamn.dominio.course.exercise

import kotlin.random.Random

// Una pregunta mientras se responde. Máquina de estados pequeña, como Submission en el registro:
// eligiendo (correct = null) → comprobada (correct = true o false). Inmutable: cada acción
// devuelve un estado nuevo, y lo que no tiene sentido (cambiar la respuesta ya comprobada, unir
// en una pregunta de opción única…) devuelve el mismo estado.
data class ExerciseState(
    val question: Question,
    val draft: Draft,
    val correct: Boolean? = null,
) {
    val isChecked: Boolean get() = correct != null
    val canCheck: Boolean get() = !isChecked && draft.answer != null

    // Opción única: elige esa. Selección múltiple: la marca o la desmarca.
    fun choose(index: Int) = edit {
        when (it) {
            is SingleDraft -> it.copy(choice = index)
            is MultipleDraft -> it.toggle(index)
            is MatchingDraft -> it
        }
    }

    fun tapLeft(index: Int) = edit { (it as? MatchingDraft)?.tapLeft(index) ?: it }
    fun tapRight(index: Int) = edit { (it as? MatchingDraft)?.tapRight(index) ?: it }

    fun check(): ExerciseState {
        val answer = draft.answer
        return if (isChecked || answer == null) this else copy(correct = question.isCorrect(answer))
    }

    private inline fun edit(change: (Draft) -> Draft) = if (isChecked) this else copy(draft = change(draft))
}

// Factory method: cada tipo de pregunta empieza con su borrador vacío. El Random solo se usa para
// barajar la columna derecha de emparejar.
fun Question.startExercise(random: Random): ExerciseState = ExerciseState(
    question = this,
    draft = when (this) {
        is SingleChoice -> SingleDraft()
        is MultipleChoice -> MultipleDraft()
        is Matching -> MatchingDraft(rightOrder = rightOrder(random))
    },
)
