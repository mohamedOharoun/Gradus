package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import es.ulpgc.pamn.dominio.data.course.CourseCopyDao
import es.ulpgc.pamn.dominio.data.course.CourseCopyEntity
import es.ulpgc.pamn.dominio.data.publication.CourseDraftDao
import es.ulpgc.pamn.dominio.data.publication.CourseDraftEntity

// La base de datos local de progreso, copias de cursos y borradores de publicación (Room sobre SQLite).
// Esquemas congelados y exportados en app/schemas/ sin borrado destructivo: cada incremento de
// versión exige su Migration explícita.
@Database(
    entities = [
        EnrollmentEntity::class,
        MasteredModuleEntity::class,
        CourseCopyEntity::class,
        CourseDraftEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
abstract class ProgressDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao

    abstract fun courseCopyDao(): CourseCopyDao

    abstract fun courseDraftDao(): CourseDraftDao

    companion object {
        const val DATABASE_NAME = "gradus-progress.db"

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `course_drafts` (" +
                        "`id` TEXT NOT NULL, " +
                        "`user_id` TEXT NOT NULL, " +
                        "`target_course_id` TEXT, " +
                        "`file_path` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`module_count` INTEGER, " +
                        "`status_kind` TEXT NOT NULL, " +
                        "`pending_action` TEXT, " +
                        "`pending_notice` TEXT, " +
                        "`rejection_kind` TEXT, " +
                        "`report_json` TEXT, " +
                        "`updated_at` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_course_drafts_user_id` " +
                        "ON `course_drafts` (`user_id`)",
                )
            }
        }

        // Método fábrica (factory method). Los tests pasan otro nombre para no tocar la base real.
        fun create(context: Context, name: String = DATABASE_NAME): ProgressDatabase =
            Room.databaseBuilder(context.applicationContext, ProgressDatabase::class.java, name)
                .addMigrations(MIGRATION_2_3)
                .build()
    }
}
