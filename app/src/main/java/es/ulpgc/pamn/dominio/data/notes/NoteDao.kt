package es.ulpgc.pamn.dominio.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Las consultas a la tabla `notes`. Room escribe el código al compilar (KSP) a partir de estas
// anotaciones y comprueba el SQL: una columna mal escrita no compila.
// Las que devuelven Flow avisan cada vez que la tabla cambia.
@Dao
abstract class NoteDao {

    // La más reciente primero y, a igual fecha, por id: la misma regla que newestFirst (sharedLogic).
    @Query(
        """
        SELECT * FROM notes
        WHERE courseId = :courseId AND moduleId = :moduleId
        ORDER BY createdAt DESC, id DESC
        """,
    )
    abstract fun notes(courseId: String, moduleId: String): Flow<List<NoteEntity>>

    @Query("SELECT moduleId, COUNT(*) AS count FROM notes WHERE courseId = :courseId GROUP BY moduleId")
    abstract fun counts(courseId: String): Flow<List<ModuleNoteCount>>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun find(id: String): NoteEntity?

    @Insert
    abstract suspend fun insert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    abstract suspend fun delete(id: String)

    // Buscar y borrar en una sola transacción: nadie puede borrarla entre medias.
    @Transaction
    open suspend fun takeOut(id: String): NoteEntity? = find(id)?.also { delete(id) }
}
