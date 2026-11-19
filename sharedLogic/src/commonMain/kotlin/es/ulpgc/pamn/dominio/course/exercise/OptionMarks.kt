package es.ulpgc.pamn.dominio.course.exercise

// Qué enseña una opción (de opción única o selección múltiple) después de comprobar.
// NONE: aún no se ha comprobado, o la opción ni era buena ni se eligió.
enum class OptionMark { NONE, CORRECT, WRONG }

fun ExerciseState.isSelected(option: Int): Boolean = when (val draft = draft) {
    is SingleDraft -> draft.choice == option
    is MultipleDraft -> option in draft.choices
    is MatchingDraft -> false
}

// Las buenas salen como correctas (también las que no se eligieron: así se ve la respuesta) y las
// elegidas que no eran buenas, como incorrectas.
fun ExerciseState.markFor(option: Int): OptionMark {
    if (!isChecked) return OptionMark.NONE
    val isRight = when (val question = question) {
        is SingleChoice -> option == question.correct
        is MultipleChoice -> option in question.correct
        is Matching -> return OptionMark.NONE
    }
    return when {
        isRight -> OptionMark.CORRECT
        isSelected(option) -> OptionMark.WRONG
        else -> OptionMark.NONE
    }
}
