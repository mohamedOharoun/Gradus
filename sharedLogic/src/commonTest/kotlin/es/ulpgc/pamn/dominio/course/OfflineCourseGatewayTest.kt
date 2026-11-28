package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.http.CourseDecoding
import es.ulpgc.pamn.dominio.course.http.EXAMPLE_COURSE_RESPONSE
import es.ulpgc.pamn.dominio.course.http.decodeCourseVersion
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// OfflineCourseGateway: red primero, guarda copia al descargar y la usa solo cuando no hay red.
class OfflineCourseGatewayTest {

    private val exampleCourse: Course = (decodeCourseVersion(EXAMPLE_COURSE_RESPONSE) as CourseDecoding.Decoded).course
    private val graphsV1 = Course("g", "Grafos v1", listOf(Module("m1", "Representación")))
    private val graphsV2 = Course("g", "Grafos v2", listOf(Module("m1", "Representación"), Module("m2", "BFS", requires = listOf("m1"))))

    @Test
    fun `found from delegate saves a copy and returns found`() = runTest {
        val copies = InMemoryCourseCopyStore()
        val gateway = OfflineCourseGateway(FakeCourseGateway(listOf(exampleCourse)), copies)

        val outcome = gateway.course(exampleCourse.id)

        assertEquals(CourseOutcome.Found(exampleCourse), outcome)
        val savedJson = copies.load(exampleCourse.id)
        assertNotNull(savedJson)
        assertEquals(CourseDecoding.Decoded(exampleCourse), decodeCourseVersion(savedJson))
    }

    @Test
    fun `a newer found from delegate overwrites the previous copy`() = runTest {
        val copies = InMemoryCourseCopyStore()
        var answer: CourseOutcome = CourseOutcome.Found(graphsV1)
        val server = FakeCourseGateway(courseAnswer = { answer })
        val gateway = OfflineCourseGateway(server, copies)

        assertEquals(CourseOutcome.Found(graphsV1), gateway.course("g"))

        // Red primero incluso habiendo copia: devuelve la versión nueva y actualiza el almacén.
        answer = CourseOutcome.Found(graphsV2)
        assertEquals(CourseOutcome.Found(graphsV2), gateway.course("g"))
        assertEquals(listOf("g", "g"), server.requestedCourses)

        // Al perder la red después, sale ya la versión nueva guardada.
        answer = CourseOutcome.Unreachable
        assertEquals(CourseOutcome.Found(graphsV2), gateway.course("g"))
    }

    @Test
    fun `uses the requested id as storage key`() = runTest {
        val backing = InMemoryCourseCopyStore()
        val copies = RecordingCourseCopyStore(backing)
        var answer: CourseOutcome = CourseOutcome.Found(graphsV1)
        val gateway = OfflineCourseGateway(FakeCourseGateway(courseAnswer = { answer }), copies)

        assertEquals(CourseOutcome.Found(graphsV1), gateway.course("alias-id"))
        assertEquals(listOf("alias-id"), copies.savedKeys)
        assertNotNull(backing.load("alias-id"))
        assertNull(backing.load(graphsV1.id))

        answer = CourseOutcome.Unreachable
        assertEquals(CourseOutcome.Found(graphsV1), gateway.course("alias-id"))
        assertEquals(listOf("alias-id"), copies.loadedKeys)
    }

    @Test
    fun `unreachable with a valid copy returns found with the same course`() = runTest {
        val copies = InMemoryCourseCopyStore()
        var answer: CourseOutcome = CourseOutcome.Found(exampleCourse)
        val gateway = OfflineCourseGateway(FakeCourseGateway(courseAnswer = { answer }), copies)

        assertEquals(CourseOutcome.Found(exampleCourse), gateway.course(exampleCourse.id))

        answer = CourseOutcome.Unreachable
        assertEquals(CourseOutcome.Found(exampleCourse), gateway.course(exampleCourse.id))
    }

    @Test
    fun `unreachable without a copy returns unreachable`() = runTest {
        val gateway = OfflineCourseGateway(
            delegate = FakeCourseGateway(courseAnswer = { CourseOutcome.Unreachable }),
            copies = InMemoryCourseCopyStore(),
        )

        assertEquals(CourseOutcome.Unreachable, gateway.course("g"))
    }

    @Test
    fun `unreachable with malformed json in store returns unreachable`() = runTest {
        val copies = InMemoryCourseCopyStore()
        copies.save("g", "{roto")
        val gateway = OfflineCourseGateway(
            delegate = FakeCourseGateway(courseAnswer = { CourseOutcome.Unreachable }),
            copies = copies,
        )

        assertEquals(CourseOutcome.Unreachable, gateway.course("g"))
        assertEquals("{roto", copies.load("g"))
    }

    @Test
    fun `unreachable with unsupported format version in store returns unreachable`() = runTest {
        val copies = InMemoryCourseCopyStore()
        val futureJson = """{"id": "g", "content": {"formatVersion": 2, "title": "Del futuro"}}"""
        copies.save("g", futureJson)
        val gateway = OfflineCourseGateway(
            delegate = FakeCourseGateway(courseAnswer = { CourseOutcome.Unreachable }),
            copies = copies,
        )

        assertEquals(CourseOutcome.Unreachable, gateway.course("g"))
        assertEquals(futureJson, copies.load("g"))
    }

    @Test
    fun `not found passes through without using or touching the stored copy`() = runTest {
        assertErrorPassesThroughWithoutTouchingCopy(CourseOutcome.NotFound)
    }

    @Test
    fun `unsupported format passes through without using or touching the stored copy`() = runTest {
        assertErrorPassesThroughWithoutTouchingCopy(CourseOutcome.UnsupportedFormat)
    }

    @Test
    fun `server failure passes through without using or touching the stored copy`() = runTest {
        assertErrorPassesThroughWithoutTouchingCopy(CourseOutcome.ServerFailure)
    }

    @Test
    fun `store failure during save propagates instead of being swallowed`() = runTest {
        val failingStore = FailingCourseCopyStore(onSave = { throw IllegalStateException("disk full") })
        val gateway = OfflineCourseGateway(FakeCourseGateway(listOf(graphsV1)), failingStore)

        val error = assertFailsWith<IllegalStateException> { gateway.course("g") }
        assertEquals("disk full", error.message)
    }

    @Test
    fun `store failure during load propagates instead of being swallowed`() = runTest {
        val failingStore = FailingCourseCopyStore(onLoad = { throw IllegalStateException("read failed") })
        val gateway = OfflineCourseGateway(
            delegate = FakeCourseGateway(courseAnswer = { CourseOutcome.Unreachable }),
            copies = failingStore,
        )

        val error = assertFailsWith<IllegalStateException> { gateway.course("g") }
        assertEquals("read failed", error.message)
    }

    @Test
    fun `search delegates query and page directly without touching copies`() = runTest {
        val copies = RecordingCourseCopyStore()
        val expectedPage = CoursePage(
            items = listOf(
                CourseSummary(
                    id = "g",
                    version = 1,
                    author = Author(id = "a1", name = "Ana"),
                    publishedAt = "2026-11-26T12:00:00Z",
                    title = "Grafos v1",
                    description = "",
                    language = "es",
                    tags = emptyList(),
                    moduleCount = 1,
                ),
            ),
            page = 0,
            size = 20,
            total = 1,
        )
        var searchAnswer: SearchOutcome = SearchOutcome.Found(expectedPage)
        val server = FakeCourseGateway(searchAnswer = { _, _ -> searchAnswer })
        val gateway = OfflineCourseGateway(server, copies)
        val query = CourseQuery(text = "grafos")

        assertEquals(SearchOutcome.Found(expectedPage), gateway.search(query, page = 0))

        searchAnswer = SearchOutcome.Unreachable
        assertEquals(SearchOutcome.Unreachable, gateway.search(query, page = 2))

        searchAnswer = SearchOutcome.ServerFailure
        assertEquals(SearchOutcome.ServerFailure, gateway.search(query, page = 3))

        assertEquals(listOf(query to 0, query to 2, query to 3), server.searches)
        assertTrue(copies.savedKeys.isEmpty())
        assertTrue(copies.loadedKeys.isEmpty())
    }

    private suspend fun assertErrorPassesThroughWithoutTouchingCopy(outcome: CourseOutcome) {
        val backing = InMemoryCourseCopyStore()
        backing.save("g", EXAMPLE_COURSE_RESPONSE)
        val copies = RecordingCourseCopyStore(backing)
        val gateway = OfflineCourseGateway(
            delegate = FakeCourseGateway(courseAnswer = { outcome }),
            copies = copies,
        )

        assertEquals(outcome, gateway.course("g"))
        assertTrue(copies.savedKeys.isEmpty())
        assertTrue(copies.loadedKeys.isEmpty())
        assertEquals(EXAMPLE_COURSE_RESPONSE, backing.load("g"))
    }

    private class RecordingCourseCopyStore(
        private val delegate: CourseCopyStore = InMemoryCourseCopyStore(),
    ) : CourseCopyStore {
        val savedKeys = mutableListOf<String>()
        val loadedKeys = mutableListOf<String>()

        override suspend fun save(courseId: String, json: String) {
            savedKeys += courseId
            delegate.save(courseId, json)
        }

        override suspend fun load(courseId: String): String? {
            loadedKeys += courseId
            return delegate.load(courseId)
        }
    }

    private class FailingCourseCopyStore(
        private val onSave: () -> Unit = {},
        private val onLoad: () -> String? = { null },
    ) : CourseCopyStore {
        override suspend fun save(courseId: String, json: String) = onSave()
        override suspend fun load(courseId: String): String? = onLoad()
    }
}
