package es.ulpgc.pamn.dominio.data.course

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Fila de la tabla `course_copies`: copia local del JSON de un curso (GET /api/courses/{id}, D-3).
// Independiente del usuario y de la sesión (sin clave foránea a `enrollments`): una sola fila por curso.
@Entity(tableName = "course_copies")
data class CourseCopyEntity(
    @PrimaryKey
    @ColumnInfo(name = "course_id") val courseId: String,
    val json: String,
)
