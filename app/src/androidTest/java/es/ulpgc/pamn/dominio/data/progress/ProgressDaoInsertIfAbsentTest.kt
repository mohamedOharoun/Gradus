package es.ulpgc.pamn.dominio.data.progress

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours

// Lo que dice el comentario de ProgressDao sobre INSERT OR IGNORE: una fila que ya existe no se toca
// (ni la fecha ni pending), pero la clave foránea no se ignora.
@RunWith(AndroidJUnit4::class)
class ProgressDaoInsertIfAbsentTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val dao get() = progress.dao

    private val settledEnrollment = EnrollmentEntity(ANA, GRAPHS, enrolledAt = START, pending = false)
    private val settledModule = MasteredModuleEntity(ANA, GRAPHS, "bfs", masteredAt = START, pending = false)

    @Test
    fun a_missing_enrollment_is_inserted() = runTest {
        dao.insertEnrollmentIfAbsent(settledEnrollment)

        assertEquals(settledEnrollment, dao.enrollment(ANA, GRAPHS))
    }

    @Test
    fun an_existing_enrollment_is_not_touched() = runTest {
        dao.insertEnrollment(settledEnrollment)

        dao.insertEnrollmentIfAbsent(settledEnrollment.copy(enrolledAt = START + 1.hours, pending = true))

        assertEquals(settledEnrollment, dao.enrollment(ANA, GRAPHS))
    }

    @Test
    fun a_missing_mastered_module_is_inserted() = runTest {
        dao.insertEnrollment(settledEnrollment)

        dao.insertMasteredModuleIfAbsent(settledModule)

        assertEquals(listOf(settledModule), dao.masteredModules(ANA, GRAPHS))
    }

    @Test
    fun an_existing_mastered_module_is_not_touched() = runTest {
        dao.insertEnrollment(settledEnrollment)
        dao.insertMasteredModule(settledModule)

        dao.insertMasteredModuleIfAbsent(settledModule.copy(masteredAt = START + 1.hours, pending = true))

        assertEquals(listOf(settledModule), dao.masteredModules(ANA, GRAPHS))
    }

    // En SQLite, OR IGNORE no cubre las claves foráneas: sin inscripción sigue fallando.
    @Test
    fun the_foreign_key_is_not_ignored() = runTest {
        val failure = failureOf { dao.insertMasteredModuleIfAbsent(settledModule) }

        assertTrue("se esperaba SQLiteConstraintException y llegó $failure", failure is SQLiteConstraintException)
        assertEquals(0, progress.rowCount("mastered_modules"))
    }
}
