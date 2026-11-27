package es.ulpgc.pamn.dominio.data.publication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Acceso a la tabla `course_drafts`: consultas por usuario, inserción/sustitución compatible con
// SQLite 3.22 (minSdk 29), actualización condicional sin resurrección y limpieza atómica por cuenta.
@Dao
interface CourseDraftDao {

    // Los Flow de Room vuelven a consultar ante cualquier cambio en la tabla: el adaptador filtra
    // repeticiones con distinctUntilChanged().
    @Query("SELECT * FROM course_drafts WHERE user_id = :userId ORDER BY updated_at DESC, id ASC")
    fun watchByUser(userId: String): Flow<List<CourseDraftEntity>>

    @Query("SELECT * FROM course_drafts WHERE id = :draftId")
    suspend fun find(draftId: String): CourseDraftEntity?

    @Query("SELECT * FROM course_drafts WHERE user_id = :userId ORDER BY updated_at DESC, id ASC")
    suspend fun findByUser(userId: String): List<CourseDraftEntity>

    // INSERT OR REPLACE (válido desde SQLite 3.22 / minSdk 29; no usar @Upsert ni ON CONFLICT DO UPDATE).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CourseDraftEntity)

    // UPDATE ... WHERE id = ? atómico: devuelve filas modificadas (0 si ya fue descartado) y nunca inserta.
    @Update
    suspend fun updateIfPresent(entity: CourseDraftEntity): Int

    @Query("DELETE FROM course_drafts WHERE id = :draftId")
    suspend fun delete(draftId: String)

    @Query("DELETE FROM course_drafts WHERE user_id = :userId")
    suspend fun deleteByUser(userId: String)

    // Lee y borra todos los borradores del usuario en una única transacción para poder limpiar
    // ficheros y trabajos pendientes sin carreras al eliminar la cuenta.
    @Transaction
    suspend fun clearForUser(userId: String): List<CourseDraftEntity> {
        val existing = findByUser(userId)
        deleteByUser(userId)
        return existing
    }
}
