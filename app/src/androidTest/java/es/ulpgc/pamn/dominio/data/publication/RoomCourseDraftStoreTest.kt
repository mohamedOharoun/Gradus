package es.ulpgc.pamn.dominio.data.publication

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.data.progress.ProgressDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

// Tests instrumentados de RoomCourseDraftStore sobre SQLite real en memoria: CRUD, actualización
// condicional sin resurrección, borrado atómico por usuario, orden, aislamiento de sesiones y
// persistencia exacta de CourseReport e Instant con nanosegundos.
@RunWith(AndroidJUnit4::class)
class RoomCourseDraftStoreTest {

    private lateinit var database: ProgressDatabase
    private lateinit var sessions: InMemorySessionStore
    private lateinit var store: RoomCourseDraftStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ProgressDatabase::class.java,
        ).build()
        sessions = InMemorySessionStore()
        store = RoomCourseDraftStore(database.courseDraftDao(), sessions)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sessionOf(userId: String) = Session(
        token = "t-$userId",
        expiresAt = "2026-12-27T10:00:00Z",
        account = Account(userId, "User $userId", "$userId@ulpgc.es", "2026-11-01T10:00:00Z"),
    )

    private fun draft(
        id: String,
        userId: String = ANA,
        targetCourseId: String? = null,
        title: String = "Curso $id",
        moduleCount: Int? = 3,
        status: DraftStatus = DraftStatus.Pending(PendingAction.VALIDATING),
        updatedAt: Instant = T0,
    ) = CourseDraft(
        id = id,
        userId = userId,
        targetCourseId = targetCourseId,
        filePath = "course_drafts/$id.json",
        title = title,
        moduleCount = moduleCount,
        status = status,
        updatedAt = updatedAt,
    )

    // Escucha en backgroundScope para que el colector se cancele automáticamente al terminar runTest.
    private fun <T> TestScope.listenTo(flow: Flow<T>): ReceiveChannel<T> {
        val seen = Channel<T>(Channel.UNLIMITED)
        backgroundScope.launch { flow.collect { seen.send(it) } }
        return seen
    }

    @Test
    fun find_returns_null_when_missing_and_saved_draft_after_upsert() = runTest {
        val created = draft("d-1")

        assertNull(store.find("d-1"))
        store.upsert(created)

        assertEquals(created, store.find("d-1"))
    }

    @Test
    fun upsert_replaces_existing_draft_with_same_id() = runTest {
        val first = draft("d-1", title = "V1", updatedAt = T0)
        val second = first.copy(
            title = "V2",
            status = DraftStatus.Pending(PendingAction.PUBLISHING, PendingNotice.NETWORK),
            updatedAt = T1,
        )

        store.upsert(first)
        store.upsert(second)

        assertEquals(second, store.find("d-1"))
    }

    @Test
    fun updateIfPresent_updates_existing_draft_and_returns_true() = runTest {
        val original = draft("d-1", updatedAt = T0)
        val updated = original.copy(status = DraftStatus.Accepted(), updatedAt = T1)
        store.upsert(original)

        val changed = store.updateIfPresent(updated)

        assertTrue(changed)
        assertEquals(updated, store.find("d-1"))
    }

    @Test
    fun updateIfPresent_returns_false_and_does_not_resurrect_missing_or_deleted_draft() = runTest {
        val missing = draft("d-missing")
        val discarded = draft("d-discarded")
        store.upsert(discarded)
        store.delete(discarded.id)

        assertFalse(store.updateIfPresent(missing))
        assertNull(store.find(missing.id))

        assertFalse(store.updateIfPresent(discarded.copy(status = DraftStatus.Accepted(), updatedAt = T1)))
        assertNull(store.find(discarded.id))
    }

    @Test
    fun delete_removes_draft_and_is_idempotent() = runTest {
        store.upsert(draft("d-1"))

        store.delete("d-1")
        store.delete("d-1")
        store.delete("unknown")

        assertNull(store.find("d-1"))
    }

    @Test
    fun clearForUser_removes_only_target_user_drafts_and_returns_them_ordered() = runTest {
        val anaOlder = draft("d-ana-1", userId = ANA, updatedAt = T0)
        val anaNewer = draft("d-ana-2", userId = ANA, updatedAt = T1)
        val betoDraft = draft("d-beto-1", userId = BETO, updatedAt = T2)
        store.upsert(anaOlder)
        store.upsert(betoDraft)
        store.upsert(anaNewer)

        val removed = store.clearForUser(ANA)
        val removedAgain = store.clearForUser(ANA)

        assertEquals(listOf(anaNewer, anaOlder), removed)
        assertTrue(removedAgain.isEmpty())
        assertNull(store.find(anaOlder.id))
        assertNull(store.find(anaNewer.id))
        assertEquals(betoDraft, store.find(betoDraft.id))
    }

    @Test
    fun drafts_orders_by_updated_at_descending_and_id_ascending_on_tie() = runTest {
        sessions.save(sessionOf(ANA))
        val oldest = draft("d-z", updatedAt = T0)
        val tieB = draft("d-b", updatedAt = T1)
        val tieA = draft("d-a", updatedAt = T1)
        val newest = draft("d-m", updatedAt = T2)
        store.upsert(oldest)
        store.upsert(tieB)
        store.upsert(tieA)
        store.upsert(newest)

        val current = store.drafts().first()

        assertEquals(listOf(newest, tieA, tieB, oldest), current)
    }

    @Test
    fun drafts_without_session_emits_empty_list_and_completes() = runTest {
        store.upsert(draft("d-1", userId = ANA))

        val emissions = store.drafts().toList()

        assertEquals(listOf(emptyList<CourseDraft>()), emissions)
    }

    @Test
    fun drafts_isolates_users_and_suppresses_emissions_when_another_user_changes() = runTest {
        sessions.save(sessionOf(ANA))
        val seen = listenTo(store.drafts())
        assertEquals(emptyList<CourseDraft>(), seen.receive())

        val betoDraft = draft("d-beto", userId = BETO, updatedAt = T0)
        val anaDraft = draft("d-ana", userId = ANA, updatedAt = T1)
        store.upsert(betoDraft)
        store.upsert(betoDraft.copy(title = "Beto v2", updatedAt = T2))
        store.delete(betoDraft.id)
        store.upsert(anaDraft)

        assertEquals(listOf(anaDraft), seen.receive())
    }

    @Test
    fun drafts_reads_session_when_listening_starts_not_when_flow_is_created() = runTest {
        sessions.save(sessionOf(ANA))
        val anaDraft = draft("d-ana", userId = ANA)
        val betoDraft = draft("d-beto", userId = BETO)
        store.upsert(anaDraft)
        store.upsert(betoDraft)

        val flow = store.drafts()
        sessions.save(sessionOf(BETO))

        assertEquals(listOf(betoDraft), flow.first())
    }

    @Test
    fun drafts_keeps_listening_to_the_user_active_when_collection_started() = runTest {
        sessions.save(sessionOf(ANA))
        val seen = listenTo(store.drafts())
        assertEquals(emptyList<CourseDraft>(), seen.receive())

        sessions.save(sessionOf(BETO))
        val betoDraft = draft("d-beto", userId = BETO, updatedAt = T0)
        val anaDraft = draft("d-ana", userId = ANA, updatedAt = T1)
        store.upsert(betoDraft)
        store.upsert(anaDraft)

        assertEquals(listOf(anaDraft), seen.receive())
        assertEquals(listOf(betoDraft), store.drafts().first())
    }

    @Test
    fun persists_course_report_and_instant_with_nanoseconds() = runTest {
        val accepted = draft(
            id = "d-accepted",
            targetCourseId = "course-1",
            moduleCount = 7,
            status = DraftStatus.Accepted(
                CourseReport(
                    warnings = listOf(CourseIssue("REMOVED_IDS", "/modules", "Módulo eliminado.")),
                ),
            ),
            updatedAt = Instant.parse("2026-11-27T10:15:30.123456789Z"),
        )
        val rejected = draft(
            id = "d-rejected",
            targetCourseId = null,
            moduleCount = null,
            status = DraftStatus.Rejected(
                DraftRejection.ValidationReport(
                    CourseReport(
                        errors = listOf(CourseIssue("CYCLE", "/modules/1", "Ciclo detectado.")),
                        warnings = listOf(CourseIssue("WARN", "/title", "Título muy largo.")),
                    ),
                ),
            ),
            updatedAt = Instant.parse("2026-11-27T10:15:31.987654321Z"),
        )

        store.upsert(accepted)
        store.upsert(rejected)

        assertEquals(accepted, store.find(accepted.id))
        assertEquals(rejected, store.find(rejected.id))
    }

    private companion object {
        const val ANA = "ana"
        const val BETO = "beto"
        val T0 = Instant.parse("2026-11-27T10:00:00Z")
        val T1 = Instant.parse("2026-11-27T10:05:00Z")
        val T2 = Instant.parse("2026-11-27T10:10:00Z")
    }
}
