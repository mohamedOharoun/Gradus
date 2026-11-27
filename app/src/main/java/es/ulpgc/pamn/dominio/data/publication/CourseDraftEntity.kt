package es.ulpgc.pamn.dominio.data.publication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

// Fila de la tabla `course_drafts`: metadatos y estado de un borrador local (decisión D).
// El JSON del curso se guarda en un fichero privado (`file_path`), nunca en SQLite.
// Sin clave foránea a `enrollments`: los borradores de autor son independientes de las inscripciones.
@Entity(
    tableName = "course_drafts",
    indices = [Index(value = ["user_id"], name = "index_course_drafts_user_id")],
)
data class CourseDraftEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "target_course_id")
    val targetCourseId: String?,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    val title: String,
    @ColumnInfo(name = "module_count")
    val moduleCount: Int?,
    @ColumnInfo(name = "status_kind")
    val statusKind: String,
    @ColumnInfo(name = "pending_action")
    val pendingAction: String?,
    @ColumnInfo(name = "pending_notice")
    val pendingNotice: String?,
    @ColumnInfo(name = "rejection_kind")
    val rejectionKind: String?,
    @ColumnInfo(name = "report_json")
    val reportJson: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
