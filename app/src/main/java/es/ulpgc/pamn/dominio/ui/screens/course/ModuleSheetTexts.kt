package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.ui.text.UiText

// Textos de la hoja de un módulo (docs/tareas/notas-camara.md, tarea 3). Funciones puras, probadas en el
// ordenador (ModuleSheetTextsTest). Solo se abre la hoja de módulos desbloqueados: frontera o dominados.

fun studyButtonText(state: ModuleState): UiText = when (state) {
    ModuleState.MASTERED -> UiText.of(R.string.sheet_review)
    else -> UiText.of(R.string.sheet_study)
}

fun moduleStateText(state: ModuleState): UiText = when (state) {
    ModuleState.MASTERED -> UiText.of(R.string.sheet_state_mastered)
    else -> UiText.of(R.string.sheet_state_to_study)
}

// Boceto 1: «Mis notas · 3»; sin notas, sin número.
fun notesButtonText(count: Int): UiText =
    if (count == 0) UiText.of(R.string.sheet_notes) else UiText.of(R.string.sheet_notes_count, count)

// Lo que lee TalkBack: con palabras, porque «·» no se lee bien. Plural de strings.xml («1 foto» / «3 fotos»).
fun notesButtonDescription(count: Int): UiText =
    if (count == 0) UiText.of(R.string.sheet_notes_none) else UiText.plural(R.plurals.sheet_notes_photos, count)
