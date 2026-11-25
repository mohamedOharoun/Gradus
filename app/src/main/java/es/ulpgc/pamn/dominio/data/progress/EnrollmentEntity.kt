package es.ulpgc.pamn.dominio.data.progress

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlin.time.Instant

// Fila de la tabla `enrollments`: un usuario inscrito en un curso (C-4). Una sola fila por usuario y
// curso. `pending` = todavía no se ha subido al servidor.
@Entity(tableName = "enrollments", primaryKeys = ["user_id", "course_id"])
data class EnrollmentEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "course_id") val courseId: String,
    @ColumnInfo(name = "enrolled_at") val enrolledAt: Instant,
    val pending: Boolean,
)
