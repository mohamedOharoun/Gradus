package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// Los textos de la hoja de un módulo (docs/tareas/notas-camara.md, tarea 3).
class ModuleSheetTextsTest {

    @Test
    fun `a frontier module is studied and a mastered one reviewed`() {
        assertEquals(UiText.of(R.string.sheet_study), studyButtonText(ModuleState.FRONTIER))
        assertEquals(UiText.of(R.string.sheet_review), studyButtonText(ModuleState.MASTERED))
    }

    @Test
    fun `the state of the module is said in words`() {
        assertEquals(UiText.of(R.string.sheet_state_to_study), moduleStateText(ModuleState.FRONTIER))
        assertEquals(UiText.of(R.string.sheet_state_mastered), moduleStateText(ModuleState.MASTERED))
    }

    // Boceto 1: «Mis notas · 3»; sin notas, sin número.
    @Test
    fun `my notes shows how many there are`() {
        assertEquals(UiText.of(R.string.sheet_notes), notesButtonText(0))
        assertEquals(UiText.of(R.string.sheet_notes_count, 3), notesButtonText(3))
    }

    // TalkBack no lee bien «·»: se dice con palabras.
    @Test
    fun `talkback hears how many photos there are in words`() {
        assertEquals(UiText.of(R.string.sheet_notes_none), notesButtonDescription(0))
        // Un plural de verdad (<plurals>): «1 foto» / «3 fotos», «1 photo» / «3 photos».
        assertEquals(UiText.plural(R.plurals.sheet_notes_photos, 1), notesButtonDescription(1))
        assertEquals(UiText.plural(R.plurals.sheet_notes_photos, 3), notesButtonDescription(3))
    }
}
