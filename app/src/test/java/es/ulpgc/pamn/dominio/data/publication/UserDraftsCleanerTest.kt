package es.ulpgc.pamn.dominio.data.publication

import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.InMemoryCourseDraftContentStore
import es.ulpgc.pamn.dominio.course.publication.InMemoryCourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class UserDraftsCleanerTest {

    private fun draft(id: String, userId: String) = CourseDraft(
        id = id,
        userId = userId,
        targetCourseId = null,
        filePath = "course_drafts/$id.json",
        title = "Curso $id",
        moduleCount = 3,
        status = DraftStatus.Pending(PendingAction.VALIDATING),
        updatedAt = Instant.parse("2026-11-27T12:00:00Z"),
    )

    @Test
    fun `deletes user rows, cancels each upload, and deletes each local file`() = runTest {
        val drafts = InMemoryCourseDraftStore(InMemorySessionStore())
        val contents = InMemoryCourseDraftContentStore()
        val cancelled = mutableListOf<String>()
        val cleaner = UserDraftsCleaner(drafts, contents, cancelUpload = cancelled::add)

        val d1 = draft("d1", "u1")
        val d2 = draft("d2", "u1")
        drafts.upsert(d1)
        drafts.upsert(d2)
        contents.put(d1.filePath, "{}".toByteArray())
        contents.put(d2.filePath, "{}".toByteArray())

        cleaner.clearForUser("u1")

        assertNull(drafts.find("d1"))
        assertNull(drafts.find("d2"))
        assertEquals(setOf("d1", "d2"), cancelled.toSet())
        assertNull(contents.read(d1.filePath))
        assertNull(contents.read(d2.filePath))
    }

    @Test
    fun `does not affect drafts, uploads, or files of other users`() = runTest {
        val drafts = InMemoryCourseDraftStore(InMemorySessionStore())
        val contents = InMemoryCourseDraftContentStore()
        val cancelled = mutableListOf<String>()
        val cleaner = UserDraftsCleaner(drafts, contents, cancelUpload = cancelled::add)

        val own = draft("d-u1", "u1")
        val other = draft("d-u2", "u2")
        drafts.upsert(own)
        drafts.upsert(other)
        contents.put(own.filePath, "{}".toByteArray())
        contents.put(other.filePath, "{}".toByteArray())

        cleaner.clearForUser("u1")

        assertNull(drafts.find("d-u1"))
        assertEquals(other, drafts.find("d-u2"))
        assertEquals(listOf("d-u1"), cancelled)
        assertNull(contents.read(own.filePath))
        assertNotNull(contents.read(other.filePath))
    }

    @Test
    fun `does nothing when user has no drafts`() = runTest {
        val drafts = InMemoryCourseDraftStore(InMemorySessionStore())
        val contents = InMemoryCourseDraftContentStore()
        val cancelled = mutableListOf<String>()
        val cleaner = UserDraftsCleaner(drafts, contents, cancelUpload = cancelled::add)

        cleaner.clearForUser("u-empty")

        assertEquals(emptyList<String>(), cancelled)
    }
}
