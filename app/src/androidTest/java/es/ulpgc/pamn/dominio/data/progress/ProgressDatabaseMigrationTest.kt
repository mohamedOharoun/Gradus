package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.data.publication.toDomain
import es.ulpgc.pamn.dominio.data.publication.toEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

// Prueba la migración real v2 -> v3 sobre una base física sin añadir room-testing:
// crea el esquema exacto de 2.json con SQLiteOpenHelper, inserta datos de v2, abre la base con
// ProgressDatabase.create (que dispara MIGRATION_2_3 y la validación de esquema de Room) y
// comprueba que todos los datos previos se conservan y la nueva tabla `course_drafts` funciona.
@RunWith(AndroidJUnit4::class)
class ProgressDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instantConverter = InstantConverter()
    private var database: ProgressDatabase? = null

    @Before
    fun cleanBefore() {
        context.deleteDatabase(TEST_DB_NAME)
    }

    @After
    fun cleanAfter() {
        database?.close()
        database = null
        context.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun migrates_v2_to_v3_preserving_existing_data_and_creating_course_drafts_table() = runTest {
        createPopulatedV2Database()

        val migrated = ProgressDatabase.create(context, TEST_DB_NAME)
        database = migrated

        assertEquals(
            EnrollmentEntity(ANA, GRAPHS, ENROLLED_ANA, pending = true),
            migrated.progressDao().enrollment(ANA, GRAPHS),
        )
        assertEquals(
            EnrollmentEntity(LUIS, TREES, ENROLLED_LUIS, pending = false),
            migrated.progressDao().enrollment(LUIS, TREES),
        )
        assertEquals(
            listOf(MasteredModuleEntity(ANA, GRAPHS, "bfs", MASTERED_BFS, pending = true)),
            migrated.progressDao().masteredModules(ANA, GRAPHS),
        )
        assertEquals(COURSE_COPY_JSON, migrated.courseCopyDao().loadJson(GRAPHS))

        assertTrue(hasCourseDraftsUserIndex(migrated))

        val draft = CourseDraft(
            id = "draft-1",
            userId = ANA,
            targetCourseId = GRAPHS,
            filePath = "course_drafts/draft-1.json",
            title = "Grafos v2",
            moduleCount = 5,
            status = DraftStatus.Accepted(
                CourseReport(warnings = listOf(CourseIssue("REMOVED_IDS", "/modules", "Aviso"))),
            ),
            updatedAt = DRAFT_UPDATED_AT,
        )
        migrated.courseDraftDao().upsert(draft.toEntity())
        migrated.close()
        database = null

        val reopened = ProgressDatabase.create(context, TEST_DB_NAME)
        database = reopened
        assertEquals(draft, reopened.courseDraftDao().find(draft.id)?.toDomain())
    }

    private fun createPopulatedV2Database() {
        val helper = object : SQLiteOpenHelper(context, TEST_DB_NAME, null, 2) {
            override fun onCreate(db: SQLiteDatabase) {
                // DDL exacto de app/schemas/es.ulpgc.pamn.dominio.data.progress.ProgressDatabase/2.json
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `enrollments` (" +
                        "`user_id` TEXT NOT NULL, " +
                        "`course_id` TEXT NOT NULL, " +
                        "`enrolled_at` INTEGER NOT NULL, " +
                        "`pending` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`user_id`, `course_id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mastered_modules` (" +
                        "`user_id` TEXT NOT NULL, " +
                        "`course_id` TEXT NOT NULL, " +
                        "`module_id` TEXT NOT NULL, " +
                        "`mastered_at` INTEGER NOT NULL, " +
                        "`pending` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`user_id`, `course_id`, `module_id`), " +
                        "FOREIGN KEY(`user_id`, `course_id`) REFERENCES `enrollments`(`user_id`, `course_id`) " +
                        "ON UPDATE NO ACTION ON DELETE NO ACTION )",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `course_copies` (" +
                        "`course_id` TEXT NOT NULL, " +
                        "`json` TEXT NOT NULL, " +
                        "PRIMARY KEY(`course_id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table " +
                        "(id INTEGER PRIMARY KEY,identity_hash TEXT)",
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                        "VALUES(42, 'd83a836c328e03cfa20d53f199de8140')",
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        helper.use { openHelper ->
            val db = openHelper.writableDatabase
            db.execSQL(
                "INSERT INTO enrollments (user_id, course_id, enrolled_at, pending) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(ANA, GRAPHS, instantConverter.toEpochNanos(ENROLLED_ANA), 1),
            )
            db.execSQL(
                "INSERT INTO enrollments (user_id, course_id, enrolled_at, pending) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(LUIS, TREES, instantConverter.toEpochNanos(ENROLLED_LUIS), 0),
            )
            db.execSQL(
                "INSERT INTO mastered_modules (user_id, course_id, module_id, mastered_at, pending) " +
                    "VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>(ANA, GRAPHS, "bfs", instantConverter.toEpochNanos(MASTERED_BFS), 1),
            )
            db.execSQL(
                "INSERT INTO course_copies (course_id, json) VALUES (?, ?)",
                arrayOf<Any>(GRAPHS, COURSE_COPY_JSON),
            )
        }
    }

    private fun hasCourseDraftsUserIndex(db: ProgressDatabase): Boolean =
        db.openHelper.readableDatabase.query("PRAGMA index_list(`course_drafts`)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == "index_course_drafts_user_id") return true
            }
            false
        }

    private companion object {
        const val TEST_DB_NAME = "progress-migration-2-to-3-test.db"
        const val ANA = "ana"
        const val LUIS = "luis"
        const val GRAPHS = "graphs"
        const val TREES = "trees"
        const val COURSE_COPY_JSON = """{"id":"graphs","title":"Grafos"}"""
        val ENROLLED_ANA = Instant.parse("2026-11-25T10:15:30.123456789Z")
        val ENROLLED_LUIS = Instant.parse("2026-11-25T11:00:00.987654321Z")
        val MASTERED_BFS = Instant.parse("2026-11-26T08:00:00.000000001Z")
        val DRAFT_UPDATED_AT = Instant.parse("2026-11-27T19:30:00.555555555Z")
    }
}
