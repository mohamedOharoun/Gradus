package es.ulpgc.pamn.dominio.data.notes

import androidx.room.Database
import androidx.room.RoomDatabase

// La base de datos SQLite de la app (docs/tareas/notas-camara.md, decisión V). Hoy solo tiene notas; el
// progreso u otras tablas podrán añadirse después (entonces se sube `version` y se escribe la migración).
// exportSchema = false: todavía no hay migraciones que comprobar contra un esquema guardado.
@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class GradusDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
}
