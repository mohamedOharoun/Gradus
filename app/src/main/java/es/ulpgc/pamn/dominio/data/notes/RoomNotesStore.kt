package es.ulpgc.pamn.dominio.data.notes

import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.notes.NotesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Adaptador del puerto NotesStore con Room (docs/tareas/notas-camara.md, tarea 2): guarda las notas en
// la base de datos SQLite del móvil. Solo traduce entre Note y NoteEntity; las reglas (orden, recuentos)
// están en las consultas de NoteDao y se prueban igual que InMemoryNotesStore (RoomNotesStoreTest).
class RoomNotesStore(private val dao: NoteDao) : NotesStore {

    // distinctUntilChanged: Room avisa cuando cambia la tabla, aunque sea en otro módulo; así solo
    // llega un valor nuevo si cambian de verdad las notas de este.
    override fun notes(courseId: String, moduleId: String): Flow<List<Note>> =
        dao.notes(courseId, moduleId).map { rows -> rows.map(NoteEntity::toNote) }.distinctUntilChanged()

    override fun counts(courseId: String): Flow<Map<String, Int>> =
        dao.counts(courseId).map { rows -> rows.associate { it.moduleId to it.count } }.distinctUntilChanged()

    override suspend fun find(id: String): Note? = dao.find(id)?.toNote()

    override suspend fun add(note: Note) = dao.insert(note.toEntity())

    override suspend fun delete(id: String): Note? = dao.takeOut(id)?.toNote()
}
