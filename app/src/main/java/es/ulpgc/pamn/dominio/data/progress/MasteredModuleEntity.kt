package es.ulpgc.pamn.dominio.data.progress

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import kotlin.time.Instant

// Fila de la tabla `mastered_modules`: un módulo que un usuario ha dominado en un curso (C-4).
// La clave foránea (user_id, course_id) → enrollments impide guardar un dominado sin la inscripción
// de ese mismo usuario en ese mismo curso.
// En el esquema v3 la clave foránea no declara `onDelete = ForeignKey.CASCADE` (no se modifican las
// anotaciones para no alterar el hash congelado de 3.json): el borrado en cascada se garantiza
// mediante `@Transaction` en ProgressDao.deleteEnrollmentWithMastered borrando primero
// `mastered_modules` y luego `enrollments`.
// En la futura MIGRATION_3_4 («Pausar curso») se añadirá `onDelete = ForeignKey.CASCADE` en esta @ForeignKey.
// Índice: la clave foránea empieza igual que la clave primaria (user_id, course_id, …), y SQLite ya
// tiene un índice para la clave primaria; por eso Room no pide un índice aparte.
@Entity(
    tableName = "mastered_modules",
    primaryKeys = ["user_id", "course_id", "module_id"],
    foreignKeys = [
        ForeignKey(
            entity = EnrollmentEntity::class,
            parentColumns = ["user_id", "course_id"],
            childColumns = ["user_id", "course_id"],
        ),
    ],
)
data class MasteredModuleEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "course_id") val courseId: String,
    @ColumnInfo(name = "module_id") val moduleId: String,
    @ColumnInfo(name = "mastered_at") val masteredAt: Instant,
    val pending: Boolean,
)
