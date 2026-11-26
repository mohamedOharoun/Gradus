package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import es.ulpgc.pamn.dominio.data.course.CourseCopyDao
import es.ulpgc.pamn.dominio.data.course.CourseCopyEntity

// La base de datos local de progreso y copias de cursos (Room sobre el SQLite del sistema).
// Esquema congelado en versión 2 (tarea 14, C-4): exportado a app/schemas/ y sin borrado
// destructivo; cualquier cambio futuro de esquema exige añadir una Migration de Room.
@Database(
    entities = [EnrollmentEntity::class, MasteredModuleEntity::class, CourseCopyEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
abstract class ProgressDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao

    abstract fun courseCopyDao(): CourseCopyDao

    companion object {
        const val DATABASE_NAME = "gradus-progress.db"

        // Método fábrica (factory method). Los tests pasan otro nombre para no tocar la base real.
        fun create(context: Context, name: String = DATABASE_NAME): ProgressDatabase =
            Room.databaseBuilder(context.applicationContext, ProgressDatabase::class.java, name)
                .build()
    }
}
