package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.course.ModuleState
import org.junit.Assert.assertEquals
import org.junit.Test

// Los textos de la hoja de un módulo (docs/tareas/notas-camara.md, tarea 3).
class ModuleSheetTextsTest {

    @Test
    fun `a frontier module is studied and a mastered one reviewed`() {
        assertEquals("Estudiar", studyButtonText(ModuleState.FRONTIER))
        assertEquals("Repasar", studyButtonText(ModuleState.MASTERED))
    }

    @Test
    fun `the state of the module is said in words`() {
        assertEquals("Por estudiar", moduleStateText(ModuleState.FRONTIER))
        assertEquals("Dominado", moduleStateText(ModuleState.MASTERED))
    }

    // Boceto 1: «Mis notas · 3»; sin notas, sin número.
    @Test
    fun `my notes shows how many there are`() {
        assertEquals("Mis notas", notesButtonText(0))
        assertEquals("Mis notas · 3", notesButtonText(3))
    }

    // TalkBack no lee bien «·»: se dice con palabras.
    @Test
    fun `talkback hears how many photos there are in words`() {
        assertEquals("Mis notas, ninguna todavía", notesButtonDescription(0))
        assertEquals("Mis notas, 1 foto", notesButtonDescription(1))
        assertEquals("Mis notas, 3 fotos", notesButtonDescription(3))
    }
}
