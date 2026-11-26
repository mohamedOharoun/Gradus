package es.ulpgc.pamn.dominio.data.notes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import es.ulpgc.pamn.dominio.notes.Note

// Una fila de la tabla `notes` (docs/tareas/notas-camara.md, decisiones V y W). Es el formato de la base
// de datos; la app trabaja con Note (sharedLogic). Las dos funciones de abajo traducen de uno a otro.
// El índice acelera lo que más se pide: las notas de un módulo de un curso.
@Entity(tableName = "notes", indices = [Index("courseId", "moduleId")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val moduleId: String,
    // Ruta de la foto en la carpeta privada de la app: la foto no va en la base de datos.
    val file: String,
    val caption: String?,
    // Milisegundos desde el 1-1-1970 (UTC).
    val createdAt: Long,
)

fun NoteEntity.toNote() = Note(id, courseId, moduleId, file, caption, createdAt)

fun Note.toEntity() = NoteEntity(id, courseId, moduleId, file, caption, createdAt)

// Resultado de contar las notas de cada módulo (NoteDao.counts).
data class ModuleNoteCount(val moduleId: String, val count: Int)
