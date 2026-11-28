package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

// El esquema de Room (C-4): la clave foránea impide un módulo dominado sin la inscripción de ese
// mismo usuario en ese mismo curso, y las fechas vuelven exactas. Base en memoria salvo el último
// test, que usa un fichero con un nombre propio de los tests (nunca el de la app).
// Se ejecuta en el emulador:
// ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=es.ulpgc.pamn.dominio.data.progress.ProgressDatabaseTest
@RunWith(AndroidJUnit4::class)
class ProgressDatabaseTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: ProgressDatabase
    private lateinit var dao: ProgressDao

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, ProgressDatabase::class.java).build()
        dao = database.progressDao()
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    private fun enrollment(userId: String = ANA, courseId: String = GRAPHS) =
        EnrollmentEntity(userId, courseId, enrolledAt = ENROLLED_AT, pending = true)

    private fun mastered(userId: String = ANA, courseId: String = GRAPHS, moduleId: String = "bfs") =
        MasteredModuleEntity(userId, courseId, moduleId, masteredAt = MASTERED_AT, pending = true)

    private suspend fun assertRejected(module: MasteredModuleEntity) {
        val failure = runCatching { dao.insertMasteredModule(module) }.exceptionOrNull()

        assertTrue("se esperaba SQLiteConstraintException y llegó $failure", failure is SQLiteConstraintException)
        assertEquals(emptyList<MasteredModuleEntity>(), dao.masteredModules(module.userId, module.courseId))
    }

    @Test
    fun a_mastered_module_without_any_enrollment_is_rejected() = runTest {
        assertRejected(mastered())
    }

    @Test
    fun a_mastered_module_is_rejected_when_only_another_user_is_enrolled_in_that_course() = runTest {
        dao.insertEnrollment(enrollment(userId = LUIS))

        assertRejected(mastered(userId = ANA))
    }

    @Test
    fun a_mastered_module_is_rejected_when_the_user_is_only_enrolled_in_another_course() = runTest {
        dao.insertEnrollment(enrollment(courseId = TREES))

        assertRejected(mastered(courseId = GRAPHS))
    }

    @Test
    fun with_its_enrollment_the_mastered_module_is_saved_and_read_back_equal() = runTest {
        dao.insertEnrollment(enrollment())
        val module = mastered()

        dao.insertMasteredModule(module)

        assertEquals(listOf(module), dao.masteredModules(ANA, GRAPHS))
    }

    @Test
    fun the_enrollment_is_read_back_equal() = runTest {
        dao.insertEnrollment(enrollment())

        assertEquals(enrollment(), dao.enrollment(ANA, GRAPHS))
    }

    @Test
    fun an_enrollment_that_does_not_exist_reads_as_null() = runTest {
        dao.insertEnrollment(enrollment(userId = LUIS))

        assertNull(dao.enrollment(ANA, GRAPHS))
    }

    @Test
    fun the_dates_come_back_exact_with_their_nanoseconds() = runTest {
        dao.insertEnrollment(enrollment())
        dao.insertMasteredModule(mastered())

        assertEquals(ENROLLED_AT, dao.enrollment(ANA, GRAPHS)?.enrolledAt)
        assertEquals(MASTERED_AT, dao.masteredModules(ANA, GRAPHS).single().masteredAt)
    }

    @Test
    fun the_pending_flag_is_saved_as_given() = runTest {
        val settled = enrollment().copy(pending = false)
        dao.insertEnrollment(settled)

        assertEquals(false, dao.enrollment(ANA, GRAPHS)?.pending)
    }

    @Test
    fun create_opens_a_file_database_that_keeps_its_data_between_instances() = runTest {
        context.deleteDatabase(TEST_DATABASE_NAME)
        ProgressDatabase.create(context, TEST_DATABASE_NAME).apply {
            progressDao().insertEnrollment(enrollment())
            close()
        }

        val reopened = ProgressDatabase.create(context, TEST_DATABASE_NAME)
        val saved = reopened.progressDao().enrollment(ANA, GRAPHS)
        reopened.close()

        assertEquals(enrollment(), saved)
    }

    private companion object {
        const val TEST_DATABASE_NAME = "progress-database-test.db"
        const val ANA = "ana"
        const val LUIS = "luis"
        const val GRAPHS = "graphs"
        const val TREES = "trees"
        val ENROLLED_AT = Instant.parse("2026-11-25T10:15:30.123456789Z")
        val MASTERED_AT = Instant.parse("2026-11-26T08:00:00.000000001Z")
    }
}
