package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.course.ModuleState

// Textos de la hoja de un módulo (docs/tareas/notas-camara.md, tarea 3). Funciones puras, probadas en el
// ordenador (ModuleSheetTextsTest). Solo se abre la hoja de módulos desbloqueados: frontera o dominados.

fun studyButtonText(state: ModuleState): String = when (state) {
    ModuleState.MASTERED -> "Repasar"
    else -> "Estudiar"
}

fun moduleStateText(state: ModuleState): String = when (state) {
    ModuleState.MASTERED -> "Dominado"
    else -> "Por estudiar"
}

// Boceto 1: «Mis notas · 3»; sin notas, sin número.
fun notesButtonText(count: Int): String = if (count == 0) "Mis notas" else "Mis notas · $count"

// Lo que lee TalkBack: con palabras, porque «·» no se lee bien.
fun notesButtonDescription(count: Int): String = when (count) {
    0 -> "Mis notas, ninguna todavía"
    1 -> "Mis notas, 1 foto"
    else -> "Mis notas, $count fotos"
}
