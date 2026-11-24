package es.ulpgc.pamn.dominio.notes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Puerto (patrón Strategy, como SettingsStore): dónde se guardan las notas. En la app, un adaptador con
// Room (tarea 2); en tests y previews, InMemoryNotesStore. Solo guarda los datos de la nota: el
// fichero de la foto lo crea y lo borra quien llama (decisión W).
interface NotesStore {

    // Las notas de un módulo, la más reciente primero (a igual fecha, por id, para que el orden sea
    // siempre el mismo). Es un Flow: quien escucha recibe las de ahora y, después, cada cambio.
    fun notes(courseId: String, moduleId: String): Flow<List<Note>>

    // Cuántas notas tiene cada módulo del curso («Mis notas · 3»). Los módulos sin notas no aparecen.
    fun counts(courseId: String): Flow<Map<String, Int>>

    suspend fun find(id: String): Note?

    suspend fun add(note: Note)

    // Borra la nota y la devuelve (para borrar después su fichero); null si no existía.
    suspend fun delete(id: String): Note?
}

// La regla de orden, compartida por todas las implementaciones (también Room la usará en sus tests).
val newestFirst: Comparator<Note> = compareByDescending<Note> { it.createdAt }.thenByDescending { it.id }

// En memoria: dura lo que dure el objeto. Un solo MutableStateFlow con todas las notas.
class InMemoryNotesStore(initial: List<Note> = emptyList()) : NotesStore {

    private val all = MutableStateFlow(initial)

    override fun notes(courseId: String, moduleId: String): Flow<List<Note>> =
        all.map { notes -> notes.filter { it.courseId == courseId && it.moduleId == moduleId }.sortedWith(newestFirst) }
            .distinctUntilChanged()

    override fun counts(courseId: String): Flow<Map<String, Int>> =
        all.map { notes -> notes.filter { it.courseId == courseId }.groupingBy { it.moduleId }.eachCount() }
            .distinctUntilChanged()

    override suspend fun find(id: String): Note? = all.value.firstOrNull { it.id == id }

    override suspend fun add(note: Note) {
        all.update { it + note }
    }

    override suspend fun delete(id: String): Note? {
        val note = find(id) ?: return null
        all.update { notes -> notes.filterNot { it.id == id } }
        return note
    }
}
