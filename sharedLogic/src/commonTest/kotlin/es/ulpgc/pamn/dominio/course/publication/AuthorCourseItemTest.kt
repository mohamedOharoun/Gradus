package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AuthorCourseItemTest {

    private val author = Author(id = "user-1", name = "Ana")

    private fun summary(
        id: String,
        version: Int = 1,
        publishedAt: String = "2026-11-20T10:00:00Z",
        title: String = "Curso $id",
        moduleCount: Int = 5,
    ) = CourseSummary(
        id = id,
        version = version,
        author = author,
        publishedAt = publishedAt,
        title = title,
        description = "Descripción",
        language = "es",
        tags = listOf("grafos"),
        moduleCount = moduleCount,
    )

    private fun draft(
        id: String,
        targetCourseId: String? = null,
        title: String = "Borrador $id",
        moduleCount: Int? = 3,
        status: DraftStatus = DraftStatus.Pending(PendingAction.VALIDATING),
        updatedAt: String = "2026-11-25T10:00:00Z",
    ) = CourseDraft(
        id = id,
        userId = "user-1",
        targetCourseId = targetCourseId,
        filePath = "course_drafts/$id.json",
        title = title,
        moduleCount = moduleCount,
        status = status,
        updatedAt = Instant.parse(updatedAt),
    )

    @Test
    fun `item requires at least published or draft`() {
        assertFailsWith<IllegalArgumentException> {
            AuthorCourseItem(published = null, draft = null)
        }
    }

    @Test
    fun `item requires matching targetCourseId when both published and draft are present`() {
        val course = summary(id = "course-1")
        val unrelatedDraft = draft(id = "draft-1", targetCourseId = "course-2")
        val newCourseDraft = draft(id = "draft-2", targetCourseId = null)

        assertFailsWith<IllegalArgumentException> {
            AuthorCourseItem(published = course, draft = unrelatedDraft)
        }
        assertFailsWith<IllegalArgumentException> {
            AuthorCourseItem(published = course, draft = newCourseDraft)
        }
        assertFailsWith<IllegalArgumentException> {
            AuthorCourseItem(published = course).copy(published = null)
        }
        assertFailsWith<IllegalArgumentException> {
            AuthorCourseItem(published = course).copy(draft = unrelatedDraft)
        }
    }

    @Test
    fun `merging empty lists returns empty list`() {
        assertEquals(emptyList(), mergeAuthorCourses(emptyList(), emptyList()))
    }

    @Test
    fun `merging produces one card for published only new draft and published with version draft`() {
        val publishedOnly = summary(
            id = "c-pub",
            version = 1,
            publishedAt = "2026-11-20T10:00:00Z",
            title = "Estructuras",
            moduleCount = 6,
        )
        val publishedWithDraft = summary(
            id = "c-ver",
            version = 2,
            publishedAt = "2026-11-21T10:00:00Z",
            title = "Grafos v2",
            moduleCount = 4,
        )
        val versionDraft = draft(
            id = "d-ver",
            targetCourseId = "c-ver",
            title = "Grafos v3",
            moduleCount = 7,
            status = DraftStatus.Accepted(),
            updatedAt = "2026-11-27T12:00:00Z",
        )
        val newDraft = draft(
            id = "d-new",
            targetCourseId = null,
            title = "Álgebra",
            moduleCount = 2,
            status = DraftStatus.Pending(PendingAction.VALIDATING),
            updatedAt = "2026-11-26T12:00:00Z",
        )

        val merged = mergeAuthorCourses(
            published = listOf(publishedOnly, publishedWithDraft),
            drafts = listOf(versionDraft, newDraft),
        )

        assertEquals(3, merged.size)

        val verItem = merged[0]
        assertEquals("c-ver", verItem.id)
        assertEquals(publishedWithDraft, verItem.published)
        assertEquals(versionDraft, verItem.draft)
        assertEquals(2, verItem.publishedVersion)
        assertEquals(Instant.parse("2026-11-27T12:00:00Z"), verItem.activityAt)
        assertEquals("Grafos v3", verItem.displayTitle)
        assertEquals(7, verItem.displayModuleCount)
        assertEquals(AuthorCourseFilter.ACCEPTED, verItem.statusFilter)

        val newItem = merged[1]
        assertEquals("d-new", newItem.id)
        assertNull(newItem.published)
        assertEquals(newDraft, newItem.draft)
        assertNull(newItem.publishedVersion)
        assertEquals("Álgebra", newItem.displayTitle)
        assertEquals(2, newItem.displayModuleCount)
        assertEquals(AuthorCourseFilter.PENDING, newItem.statusFilter)

        val pubItem = merged[2]
        assertEquals("c-pub", pubItem.id)
        assertEquals(publishedOnly, pubItem.published)
        assertNull(pubItem.draft)
        assertEquals(1, pubItem.publishedVersion)
        assertEquals("Estructuras", pubItem.displayTitle)
        assertEquals(6, pubItem.displayModuleCount)
        assertEquals(AuthorCourseFilter.PUBLISHED, pubItem.statusFilter)
    }

    @Test
    fun `orphan new version draft without matching published course is preserved`() {
        val orphan = draft(
            id = "d-orphan",
            targetCourseId = "c-offline",
            title = "Offline v2",
            moduleCount = null,
            status = DraftStatus.Pending(PendingAction.PUBLISHING, PendingNotice.NETWORK),
            updatedAt = "2026-11-27T09:00:00Z",
        )

        val merged = mergeAuthorCourses(published = emptyList(), drafts = listOf(orphan))

        assertEquals(1, merged.size)
        assertNull(merged[0].published)
        assertEquals(orphan, merged[0].draft)
        assertEquals("d-orphan", merged[0].id)
        assertNull(merged[0].publishedVersion)
        assertNull(merged[0].displayModuleCount)
    }

    @Test
    fun `multiple drafts for same targetCourseId attach the newest to published and keep extras`() {
        val course = summary(id = "c1", version = 2, publishedAt = "2026-11-10T10:00:00Z")
        val olderDraft = draft(
            id = "d-old",
            targetCourseId = "c1",
            title = "Intento viejo",
            updatedAt = "2026-11-25T10:00:00Z",
        )
        val newerDraft = draft(
            id = "d-new",
            targetCourseId = "c1",
            title = "Intento nuevo",
            updatedAt = "2026-11-26T10:00:00Z",
        )

        val merged = mergeAuthorCourses(
            published = listOf(course),
            drafts = listOf(olderDraft, newerDraft),
        )

        assertEquals(2, merged.size)
        assertEquals(AuthorCourseItem(published = course, draft = newerDraft), merged[0])
        assertEquals(AuthorCourseItem(published = null, draft = olderDraft), merged[1])
    }

    @Test
    fun `multiple drafts for same targetCourseId with same timestamp tie break by draft id ascending`() {
        val course = summary(id = "c1", version = 1, publishedAt = "2026-11-10T10:00:00Z")
        val draftB = draft(id = "d-b", targetCourseId = "c1", updatedAt = "2026-11-26T10:00:00Z")
        val draftA = draft(id = "d-a", targetCourseId = "c1", updatedAt = "2026-11-26T10:00:00Z")

        val merged = mergeAuthorCourses(
            published = listOf(course),
            drafts = listOf(draftB, draftA),
        )

        assertEquals(2, merged.size)
        assertEquals(AuthorCourseItem(published = course, draft = draftA), merged[0])
        assertEquals(AuthorCourseItem(published = null, draft = draftB), merged[1])
    }

    @Test
    fun `displayTitle and displayModuleCount fall back to published when draft lacks them`() {
        val course = summary(id = "c1", title = "Título publicado", moduleCount = 8)
        val brokenVersionDraft = draft(
            id = "d1",
            targetCourseId = "c1",
            title = "   ",
            moduleCount = null,
            status = DraftStatus.Rejected(DraftRejection.TooLarge),
        )

        val item = AuthorCourseItem(published = course, draft = brokenVersionDraft)

        assertEquals("Título publicado", item.displayTitle)
        assertEquals(8, item.displayModuleCount)
    }

    @Test
    fun `items are sorted in reverse chronological order with ascending id tie breaker`() {
        val olderPublished = summary(id = "c-old", publishedAt = "2026-11-01T10:00:00Z")
        val tieCourseB = summary(id = "c-b", publishedAt = "2026-11-20T10:00:00Z")
        val tieCourseA = summary(id = "c-a", publishedAt = "2026-11-20T10:00:00Z")
        val recentDraft = draft(id = "d-recent", updatedAt = "2026-11-27T10:00:00Z")
        val tieDraft = draft(id = "a-draft", updatedAt = "2026-11-20T10:00:00Z")

        val merged = mergeAuthorCourses(
            published = listOf(olderPublished, tieCourseB, tieCourseA),
            drafts = listOf(recentDraft, tieDraft),
        )

        assertEquals(
            listOf("d-recent", "a-draft", "c-a", "c-b", "c-old"),
            merged.map { it.id },
        )
    }

    @Test
    fun `malformed publishedAt falls back to distant past without throwing`() {
        val brokenDate = summary(id = "c-broken", publishedAt = "not-a-date")
        val validDate = summary(id = "c-valid", publishedAt = "2020-01-01T00:00:00Z")

        val merged = mergeAuthorCourses(
            published = listOf(brokenDate, validDate),
            drafts = emptyList(),
        )

        assertEquals(listOf("c-valid", "c-broken"), merged.map { it.id })
        assertEquals(Instant.DISTANT_PAST, merged[1].activityAt)
    }

    @Test
    fun `filtering by status uses draft status when published has an active draft`() {
        val pubOnly = summary(id = "c-pub", publishedAt = "2026-11-20T10:00:00Z")
        val pubWithAccepted = summary(id = "c-acc", publishedAt = "2026-11-19T10:00:00Z")
        val acceptedDraft = draft(
            id = "d-acc",
            targetCourseId = "c-acc",
            status = DraftStatus.Accepted(),
            updatedAt = "2026-11-25T10:00:00Z",
        )
        val pendingDraft = draft(
            id = "d-pend",
            status = DraftStatus.Pending(PendingAction.VALIDATING),
            updatedAt = "2026-11-24T10:00:00Z",
        )
        val rejectedDraft = draft(
            id = "d-rej",
            status = DraftStatus.Rejected(DraftRejection.IdenticalToLatest),
            updatedAt = "2026-11-23T10:00:00Z",
        )

        val items = mergeAuthorCourses(
            published = listOf(pubOnly, pubWithAccepted),
            drafts = listOf(acceptedDraft, pendingDraft, rejectedDraft),
        )

        assertEquals(4, items.filterBy(AuthorCourseFilter.ALL).size)
        assertEquals(listOf("c-pub"), items.filterBy(AuthorCourseFilter.PUBLISHED).map { it.id })
        assertEquals(listOf("c-acc"), items.filterBy(AuthorCourseFilter.ACCEPTED).map { it.id })
        assertEquals(listOf("d-pend"), items.filterBy(AuthorCourseFilter.PENDING).map { it.id })
        assertEquals(listOf("d-rej"), items.filterBy(AuthorCourseFilter.REJECTED).map { it.id })

        val mergedAcceptedItem = items.first { it.id == "c-acc" }
        assertTrue(mergedAcceptedItem.matches(AuthorCourseFilter.ALL))
        assertTrue(mergedAcceptedItem.matches(AuthorCourseFilter.ACCEPTED))
        assertFalse(mergedAcceptedItem.matches(AuthorCourseFilter.PUBLISHED))
    }

    @Test
    fun `countByFilter includes every enum entry including zeros`() {
        val emptyCounts = emptyList<AuthorCourseItem>().countByFilter()
        assertEquals(
            mapOf(
                AuthorCourseFilter.ALL to 0,
                AuthorCourseFilter.PUBLISHED to 0,
                AuthorCourseFilter.ACCEPTED to 0,
                AuthorCourseFilter.PENDING to 0,
                AuthorCourseFilter.REJECTED to 0,
            ),
            emptyCounts,
        )

        val items = mergeAuthorCourses(
            published = listOf(summary(id = "c1"), summary(id = "c2")),
            drafts = listOf(
                draft(id = "d1", status = DraftStatus.Pending(PendingAction.VALIDATING)),
                draft(id = "d2", targetCourseId = "c2", status = DraftStatus.Rejected(DraftRejection.TooLarge)),
            ),
        )

        assertEquals(
            mapOf(
                AuthorCourseFilter.ALL to 3,
                AuthorCourseFilter.PUBLISHED to 1,
                AuthorCourseFilter.ACCEPTED to 0,
                AuthorCourseFilter.PENDING to 1,
                AuthorCourseFilter.REJECTED to 1,
            ),
            items.countByFilter(),
        )
    }
}
