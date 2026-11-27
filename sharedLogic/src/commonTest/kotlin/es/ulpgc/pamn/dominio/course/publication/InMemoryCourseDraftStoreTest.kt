package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class InMemoryCourseDraftStoreTest {

    private val t0 = Instant.parse("2026-11-27T10:00:00Z")
    private val t1 = Instant.parse("2026-11-27T10:05:00Z")

    private fun sessionOf(userId: String) = Session(
        token = "tok-$userId",
        expiresAt = "2026-12-27T10:00:00Z",
        account = Account(userId, "User $userId", "$userId@ulpgc.es", "2026-11-01T10:00:00Z"),
    )

    private fun draft(
        id: String,
        userId: String = "ana",
        title: String = "Grafos",
        status: DraftStatus = DraftStatus.Pending(PendingAction.VALIDATING),
        updatedAt: Instant = t0,
    ) = CourseDraft(
        id = id,
        userId = userId,
        targetCourseId = null,
        filePath = "course_drafts/$id.json",
        title = title,
        moduleCount = 3,
        status = status,
        updatedAt = updatedAt,
    )

    @Test
    fun `drafts without session emits empty list and completes`() = runTest {
        val sessions = InMemorySessionStore()
        val store = InMemoryCourseDraftStore(sessions)
        store.upsert(draft("d-1", userId = "ana"))

        val emissions = store.drafts().toList()

        assertEquals(listOf(emptyList()), emissions)
    }

    @Test
    fun `drafts emits initial empty list and subsequent changes for current user`() = runTest {
        val sessions = InMemorySessionStore()
        sessions.save(sessionOf("ana"))
        val store = InMemoryCourseDraftStore(sessions)
        val created = draft("d-1", userId = "ana")
        val updated = created.copy(status = DraftStatus.Accepted(), updatedAt = t1)

        val seen = mutableListOf<List<CourseDraft>>()
        val job = launch { store.drafts().take(4).toList(seen) }
        testScheduler.runCurrent()

        store.upsert(created)
        testScheduler.runCurrent()
        store.upsert(updated)
        testScheduler.runCurrent()
        store.delete("d-1")
        job.join()

        assertEquals(
            listOf(
                emptyList(),
                listOf(created),
                listOf(updated),
                emptyList(),
            ),
            seen,
        )
    }

    @Test
    fun `drafts isolates users and suppresses emissions when another user changes`() = runTest {
        val sessions = InMemorySessionStore()
        sessions.save(sessionOf("ana"))
        val store = InMemoryCourseDraftStore(sessions)
        val anaDraft = draft("d-ana", userId = "ana")
        val betoDraft = draft("d-beto", userId = "beto")

        val seen = mutableListOf<List<CourseDraft>>()
        val job = launch { store.drafts().toList(seen) }
        testScheduler.runCurrent()

        store.upsert(betoDraft)
        testScheduler.runCurrent()
        store.upsert(anaDraft)
        testScheduler.runCurrent()
        store.upsert(betoDraft.copy(title = "Beto v2"))
        testScheduler.runCurrent()
        store.delete(betoDraft.id)
        testScheduler.runCurrent()
        job.cancel()

        assertEquals(listOf(emptyList(), listOf(anaDraft)), seen)
    }

    @Test
    fun `drafts keeps listening to the user active when collection started`() = runTest {
        val sessions = InMemorySessionStore()
        sessions.save(sessionOf("ana"))
        val store = InMemoryCourseDraftStore(sessions)
        val anaDraft = draft("d-ana", userId = "ana")
        val betoDraft = draft("d-beto", userId = "beto")

        val seen = mutableListOf<List<CourseDraft>>()
        val job = launch { store.drafts().toList(seen) }
        testScheduler.runCurrent()

        sessions.save(sessionOf("beto"))
        store.upsert(betoDraft)
        testScheduler.runCurrent()
        store.upsert(anaDraft)
        testScheduler.runCurrent()
        job.cancel()

        assertEquals(listOf(emptyList(), listOf(anaDraft)), seen)
        assertEquals(listOf(betoDraft), store.drafts().first())
    }

    @Test
    fun `find returns stored draft or null when missing`() = runTest {
        val store = InMemoryCourseDraftStore(InMemorySessionStore())
        val created = draft("d-1")

        assertNull(store.find("d-1"))
        store.upsert(created)
        assertEquals(created, store.find("d-1"))
    }

    @Test
    fun `upsert replaces an existing draft with same id`() = runTest {
        val store = InMemoryCourseDraftStore(InMemorySessionStore())
        val first = draft("d-1", title = "V1")
        val second = first.copy(title = "V2", updatedAt = t1)

        store.upsert(first)
        store.upsert(second)

        assertEquals(second, store.find("d-1"))
    }

    @Test
    fun `updateIfPresent updates existing draft and returns true`() = runTest {
        val store = InMemoryCourseDraftStore(InMemorySessionStore())
        val original = draft("d-1")
        val updated = original.copy(status = DraftStatus.Accepted(), updatedAt = t1)
        store.upsert(original)

        val changed = store.updateIfPresent(updated)

        assertTrue(changed)
        assertEquals(updated, store.find("d-1"))
    }

    @Test
    fun `updateIfPresent does not resurrect a missing or deleted draft`() = runTest {
        val store = InMemoryCourseDraftStore(InMemorySessionStore())
        val missing = draft("d-missing")
        val discarded = draft("d-discarded")
        store.upsert(discarded)
        store.delete(discarded.id)

        assertFalse(store.updateIfPresent(missing))
        assertNull(store.find(missing.id))

        assertFalse(store.updateIfPresent(discarded.copy(status = DraftStatus.Accepted())))
        assertNull(store.find(discarded.id))
    }

    @Test
    fun `delete removes existing draft and is idempotent when missing`() = runTest {
        val store = InMemoryCourseDraftStore(InMemorySessionStore())
        store.upsert(draft("d-1"))

        store.delete("d-1")
        store.delete("d-1")
        store.delete("unknown")

        assertNull(store.find("d-1"))
    }

    @Test
    fun `clearForUser removes only target user drafts and returns removed list`() = runTest {
        val store = InMemoryCourseDraftStore(InMemorySessionStore())
        val ana1 = draft("d-ana-1", userId = "ana")
        val ana2 = draft("d-ana-2", userId = "ana")
        val beto1 = draft("d-beto-1", userId = "beto")
        store.upsert(ana1)
        store.upsert(beto1)
        store.upsert(ana2)

        val removed = store.clearForUser("ana")
        val removedAgain = store.clearForUser("ana")

        assertEquals(listOf(ana1, ana2), removed)
        assertTrue(removedAgain.isEmpty())
        assertNull(store.find(ana1.id))
        assertNull(store.find(ana2.id))
        assertEquals(beto1, store.find(beto1.id))
    }

    @Test
    fun `clearForUser notifies affected user listener without emitting to another user`() = runTest {
        val sessions = InMemorySessionStore()
        val store = InMemoryCourseDraftStore(sessions)
        val anaDraft = draft("d-ana", userId = "ana")
        val betoDraft = draft("d-beto", userId = "beto")
        store.upsert(anaDraft)
        store.upsert(betoDraft)

        sessions.save(sessionOf("ana"))
        val anaSeen = mutableListOf<List<CourseDraft>>()
        val anaJob = launch { store.drafts().toList(anaSeen) }
        testScheduler.runCurrent()

        sessions.save(sessionOf("beto"))
        val betoSeen = mutableListOf<List<CourseDraft>>()
        val betoJob = launch { store.drafts().toList(betoSeen) }
        testScheduler.runCurrent()

        store.clearForUser("ana")
        testScheduler.runCurrent()
        anaJob.cancel()
        betoJob.cancel()

        assertEquals(listOf(listOf(anaDraft), emptyList()), anaSeen)
        assertEquals(listOf(listOf(betoDraft)), betoSeen)
    }
}
