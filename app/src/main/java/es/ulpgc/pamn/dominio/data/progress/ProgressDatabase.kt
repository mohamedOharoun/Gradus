package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// La base de datos local del progreso (Room sobre el SQLite del sistema).
// Desarrollo (C-4): versión 1 sin exportar el esquema; en la tarea 14 se congela.
@Database(
    entities = [EnrollmentEntity::class, MasteredModuleEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(InstantConverter::class)
abstract class ProgressDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao

    companion object {
        const val DATABASE_NAME = "gradus-progress.db"

        // Método fábrica (factory method). Si el esquema cambia mientras desarrollamos, se borran todas
        // las tablas en vez de migrar. Los tests pasan otro nombre para no tocar la base real.
        fun create(context: Context, name: String = DATABASE_NAME): ProgressDatabase =
            Room.databaseBuilder(context.applicationContext, ProgressDatabase::class.java, name)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
