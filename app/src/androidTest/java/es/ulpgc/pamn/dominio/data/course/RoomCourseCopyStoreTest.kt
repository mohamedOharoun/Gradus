package es.ulpgc.pamn.dominio.data.course

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.OfflineCourseGateway
import es.ulpgc.pamn.dominio.course.SearchOutcome
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.progress.ProgressDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Tests instrumentados de RoomCourseCopyStore sobre ProgressDatabase en memoria (12b-1).
// Comprueban la tabla `course_copies`, el DAO (INSERT OR REPLACE, válido desde SQLite 3.22 /
// minSdk 29) y la ida y vuelta completa junto a OfflineCourseGateway.
@RunWith(AndroidJUnit4::class)
class RoomCourseCopyStoreTest {

    private lateinit var database: ProgressDatabase
    private lateinit var store: RoomCourseCopyStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ProgressDatabase::class.java,
        ).build()
        store = RoomCourseCopyStore(database.courseCopyDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun loading_unknown_course_returns_null() = runTest {
        assertNull(store.load("unknown-course"))
    }

    @Test
    fun saving_and_loading_returns_exact_json() = runTest {
        val json = """{"id":"grafos","title":"Árboles y grafos 🌳 — ñ"}"""

        store.save("grafos", json)

        assertEquals(json, store.load("grafos"))
    }

    @Test
    fun saving_same_course_overwrites_previous_copy() = runTest {
        store.save("grafos", """{"v":1}""")
        store.save("grafos", """{"v":2}""")

        assertEquals("""{"v":2}""", store.load("grafos"))
        assertEquals(1, rowCount("course_copies"))
    }

    @Test
    fun different_courses_are_stored_independently() = runTest {
        store.save("grafos", """{"course":"grafos"}""")
        store.save("arboles", """{"course":"arboles"}""")
        store.save("grafos", """{"course":"grafos-v2"}""")

        assertEquals("""{"course":"grafos-v2"}""", store.load("grafos"))
        assertEquals("""{"course":"arboles"}""", store.load("arboles"))
        assertEquals(2, rowCount("course_copies"))
    }

    // Las copias de cursos no van por usuario ni dependen de estar inscrito (sin clave foránea).
    @Test
    fun saving_course_copy_does_not_require_enrollment() = runTest {
        store.save("grafos", """{"id":"grafos"}""")

        assertEquals(0, rowCount("enrollments"))
        assertEquals("""{"id":"grafos"}""", store.load("grafos"))
    }

    @Test
    fun database_opens_at_version_3_with_progress_course_copies_and_drafts_tables() = runTest {
        val db = database.openHelper.readableDatabase

        assertEquals(3, db.version)
        assertEquals(0, rowCount("enrollments"))
        assertEquals(0, rowCount("mastered_modules"))
        assertEquals(0, rowCount("course_copies"))
        assertEquals(0, rowCount("course_drafts"))
    }

    // Ida y vuelta real: OfflineCourseGateway serializa demoCourse, Room lo guarda y lo recupera
    // sin red; una descarga posterior sobrescribe la copia.
    @Test
    fun offline_course_gateway_with_room_saves_and_recovers_course_when_unreachable() = runTest {
        val remote = SwitchableCourseGateway(CourseOutcome.Found(demoCourse))
        val gateway = OfflineCourseGateway(remote, store)

        assertEquals(CourseOutcome.Found(demoCourse), gateway.course(demoCourse.id))

        remote.outcome = CourseOutcome.Unreachable
        assertEquals(CourseOutcome.Found(demoCourse), gateway.course(demoCourse.id))

        val updated = demoCourse.copy(title = "Estructuras de datos (v2)")
        remote.outcome = CourseOutcome.Found(updated)
        assertEquals(CourseOutcome.Found(updated), gateway.course(demoCourse.id))

        remote.outcome = CourseOutcome.Unreachable
        assertEquals(CourseOutcome.Found(updated), gateway.course(demoCourse.id))
        assertEquals(1, rowCount("course_copies"))
    }

    @Test
    fun offline_course_gateway_with_corrupt_copy_in_room_returns_unreachable_without_deleting_row() = runTest {
        val broken = "{not valid json"
        store.save(demoCourse.id, broken)
        val gateway = OfflineCourseGateway(SwitchableCourseGateway(CourseOutcome.Unreachable), store)

        assertEquals(CourseOutcome.Unreachable, gateway.course(demoCourse.id))
        assertEquals(broken, store.load(demoCourse.id))
    }

    private fun rowCount(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }
}

private class SwitchableCourseGateway(var outcome: CourseOutcome) : CourseGateway {
    override suspend fun search(query: CourseQuery, page: Int): SearchOutcome =
        error("Not used in this test")

    override suspend fun course(id: String): CourseOutcome = outcome
}
