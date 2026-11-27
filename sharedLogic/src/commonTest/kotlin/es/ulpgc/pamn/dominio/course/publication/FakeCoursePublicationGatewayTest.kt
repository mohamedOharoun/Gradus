package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class FakeCoursePublicationGatewayTest {

    private val session = Session(
        token = "tok-fake",
        expiresAt = "2026-12-25T10:00:00Z",
        account = Account("u-1", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"),
    )
    private val payload = """{"title":"Grafos"}""".encodeToByteArray()

    @Test
    fun `defaults answer all five operations and record calls`() = runTest {
        val fake = FakeCoursePublicationGateway()

        assertEquals(MyCoursesOutcome.Found(emptyList()), fake.myCourses(session))
        assertEquals(ValidationOutcome.Validated(CourseReport.EMPTY), fake.validateNew(session, payload))
        assertEquals(ValidationOutcome.Validated(CourseReport.EMPTY), fake.validateVersion(session, "c-1", payload))
        val firstPublished = assertIs<PublishOutcome.Published>(fake.publishNew(session, payload))
        val secondVersion = assertIs<PublishOutcome.Published>(fake.publishVersion(session, "c-1", payload))

        assertEquals("course-1", firstPublished.course.id)
        assertEquals(1, firstPublished.course.version)
        assertEquals(Author("u-1", "Ana"), firstPublished.course.author)
        assertEquals("c-1", secondVersion.course.id)
        assertEquals(2, secondVersion.course.version)

        assertEquals(listOf(session), fake.myCoursesCalls)
        assertEquals(1, fake.validateNewCalls.size)
        assertEquals(session, fake.validateNewCalls.single().first)
        assertContentEquals(payload, fake.validateNewCalls.single().second)
        assertEquals("c-1", fake.validateVersionCalls.single().courseId)
        assertContentEquals(payload, fake.validateVersionCalls.single().courseJson)
        assertEquals(1, fake.publishNewCalls.size)
        assertEquals("c-1", fake.publishVersionCalls.single().courseId)
    }

    @Test
    fun `custom answers override each operation`() = runTest {
        val summary = CourseSummary(
            id = "c-9",
            version = 3,
            author = Author("u-1", "Ana"),
            publishedAt = "2026-11-27T10:00:00Z",
            title = "Grafos",
            description = "Desc",
            language = "es",
            tags = listOf("grafos"),
            moduleCount = 2,
        )
        val report = CourseReport(errors = listOf(CourseIssue("BAD", "", "Error")))
        val fake = FakeCoursePublicationGateway(
            myCoursesAnswer = { MyCoursesOutcome.Found(listOf(summary)) },
            validateNewAnswer = { _, _ -> ValidationOutcome.Validated(report) },
            validateVersionAnswer = { _, _, _ -> ValidationOutcome.IdenticalToLatest },
            publishNewAnswer = { _, _ -> PublishOutcome.Invalid(report) },
            publishVersionAnswer = { _, _, _ -> PublishOutcome.NotAuthor },
        )

        assertEquals(MyCoursesOutcome.Found(listOf(summary)), fake.myCourses(session))
        assertEquals(ValidationOutcome.Validated(report), fake.validateNew(session, payload))
        assertEquals(ValidationOutcome.IdenticalToLatest, fake.validateVersion(session, "c-9", payload))
        assertEquals(PublishOutcome.Invalid(report), fake.publishNew(session, payload))
        assertEquals(PublishOutcome.NotAuthor, fake.publishVersion(session, "c-9", payload))
    }

    @Test
    fun `suspend answers allow pausing an in flight request`() = runTest {
        val gate = CompletableDeferred<PublishOutcome>()
        val fake = FakeCoursePublicationGateway(publishNewAnswer = { _, _ -> gate.await() })

        val deferred = async { fake.publishNew(session, payload) }
        testScheduler.runCurrent()
        assertFalse(deferred.isCompleted)

        gate.complete(PublishOutcome.Unreachable)
        assertEquals(PublishOutcome.Unreachable, deferred.await())
    }

    @Test
    fun `mutating the byte array after calling does not corrupt recorded calls`() = runTest {
        val fake = FakeCoursePublicationGateway()
        val mutableBytes = byteArrayOf(1, 2, 3)

        fake.validateNew(session, mutableBytes)
        fake.validateVersion(session, "c-1", mutableBytes)
        mutableBytes[0] = 99

        assertContentEquals(byteArrayOf(1, 2, 3), fake.validateNewCalls.single().second)
        assertContentEquals(byteArrayOf(1, 2, 3), fake.validateVersionCalls.single().courseJson)
    }
}
