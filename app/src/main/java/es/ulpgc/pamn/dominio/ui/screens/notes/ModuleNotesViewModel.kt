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
import java.util.UUID

// Lo que enseña «Mis notas» de un módulo (docs/tareas/notas-camara.md, boceto 2).
// - `adding`: copiando una foto de la galería (el botón se desactiva).
// - `addFailed`: no se pudo copiar la última (se avisa; elegir otra lo borra).
data class ModuleNotesUiState(
    val moduleTitle: String,
    val notes: List<Note> = emptyList(),
    val adding: Boolean = false,
    val addFailed: Boolean = false,
)

// «Mis notas» de un módulo: escucha sus notas y añade las que llegan «De la galería» (decisión U).
// Recibe los puertos por constructor, y también el reloj y cómo crear ids, para que los tests sepan
// exactamente qué nota se guarda.
class ModuleNotesViewModel(
    private val courseId: String,
    private val moduleId: String,
    moduleTitle: String,
    private val notes: NotesStore,
    private val photos: PhotoFiles,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    companion object {
        fun factory(courseId: String, moduleId: String, moduleTitle: String, notes: NotesStore, photos: PhotoFiles) =
            viewModelFactory { initializer { ModuleNotesViewModel(courseId, moduleId, moduleTitle, notes, photos) } }
    }

    var state by mutableStateOf(ModuleNotesUiState(moduleTitle))
        private set

    // Se queda escuchando: al guardar una foto (aquí o desde la cámara), la cuadrícula se actualiza sola.
    init {
        viewModelScope.launch {
            notes.notes(courseId, moduleId).collect { state = state.copy(notes = it) }
        }
    }

    // `source`: la dirección (URI) de la foto elegida, como texto. Se copia a la carpeta de la app y se
    // guarda como nota sin pie. Mientras se copia una, otra elegida se ignora.
    fun onPhotoPicked(source: String) {
        if (state.adding) return
        state = state.copy(adding = true, addFailed = false)
        viewModelScope.launch {
            val path = photos.import(source)
            if (path != null) {
                notes.add(Note(newId(), courseId, moduleId, path, caption = null, createdAt = clock()))
            }
            state = state.copy(adding = false, addFailed = path == null)
        }
    }
}
