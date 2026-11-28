package es.ulpgc.pamn.dominio.data.course

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// Acceso a la tabla `course_copies`: guarda y lee la copia JSON de un curso (12b-1).
@Dao
interface CourseCopyDao {

    // INSERT OR REPLACE (válido desde SQLite 3.22 / minSdk 29; no usar @Upsert ni ON CONFLICT DO UPDATE).
    // Sobrescribe la copia anterior completa si ya existía ese curso.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(copy: CourseCopyEntity)

    @Query("SELECT json FROM course_copies WHERE course_id = :courseId")
    suspend fun loadJson(courseId: String): String?
}
