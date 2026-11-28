package es.ulpgc.pamn.dominio.ui.screens.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.data.notes.PhotoFiles
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.notes.NotesStore
import kotlinx.coroutines.launch

// En qué punto está el visor de una nota (docs/tareas/notas-camara.md, boceto 4).
sealed interface NoteViewerState {

    // Buscándola (un instante).
    data object Loading : NoteViewerState

    // La nota. `confirmingDelete`: el diálogo «¿Eliminar esta nota?» está abierto (decisión Z);
    // `deleting`: ya se confirmó y se está borrando.
    data class Showing(val note: Note, val confirmingDelete: Boolean = false, val deleting: Boolean = false) : NoteViewerState

    // Eliminada, o no existe: la navegación vuelve a «Mis notas».
    data object Gone : NoteViewerState
}

// Ver una nota y eliminarla. Eliminar borra la fila y su foto (decisión Z): no hay papelera.
class NoteViewerViewModel(
    private val noteId: String,
    private val notes: NotesStore,
    private val photos: PhotoFiles,
) : ViewModel() {

    companion object {
        fun factory(noteId: String, notes: NotesStore, photos: PhotoFiles) =
            viewModelFactory { initializer { NoteViewerViewModel(noteId, notes, photos) } }
    }

    var state by mutableStateOf<NoteViewerState>(NoteViewerState.Loading)
        private set

    init {
        viewModelScope.launch {
            state = notes.find(noteId)?.let { NoteViewerState.Showing(it) } ?: NoteViewerState.Gone
        }
    }

    // «Eliminar»: primero se pregunta.
    fun onDelete() = updateShowing { if (it.deleting) it else it.copy(confirmingDelete = true) }

    // «Cancelar» en el diálogo (o tocar fuera).
    fun onDeleteCancel() = updateShowing { if (it.deleting) it else it.copy(confirmingDelete = false) }

    // «Eliminar» en el diálogo: la fila y, después, su foto. Solo una vez.
    fun onDeleteConfirm() {
        val showing = state as? NoteViewerState.Showing ?: return
        if (!showing.confirmingDelete || showing.deleting) return
        state = showing.copy(deleting = true)
        viewModelScope.launch {
            notes.delete(noteId)?.let { photos.delete(it.file) }
            state = NoteViewerState.Gone
        }
    }

    private fun updateShowing(change: (NoteViewerState.Showing) -> NoteViewerState.Showing) {
        val showing = state as? NoteViewerState.Showing ?: return
        state = change(showing)
    }
}
