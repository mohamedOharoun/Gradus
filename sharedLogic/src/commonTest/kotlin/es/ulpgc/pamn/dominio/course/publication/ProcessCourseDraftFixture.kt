package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseCacheInvalidator
import es.ulpgc.pamn.dominio.course.CourseSummary
import kotlin.time.Clock
import kotlin.time.Instant

internal val T0: Instant = Instant.parse("2026-11-27T10:00:00Z")
internal val T1: Instant = Instant.parse("2026-11-27T10:05:00Z")

internal class FixedClock(var current: Instant = T1) : Clock {
    override fun now(): Instant = current
}

internal fun testSession(userId: String = "ana", token: String = "tok-$userId") = Session(
    token = token,
    expiresAt = "2026-12-27T10:00:00Z",
    account = Account(userId, "User $userId", "$userId@ulpgc.es", "2026-11-01T10:00:00Z"),
)

internal fun testSummary(id: String = "course-42", version: Int = 1, userId: String = "ana") = CourseSummary(
    id = id,
    version = version,
    author = Author(id = userId, name = "User $userId"),
    publishedAt = "2026-11-27T10:05:00Z",
    title = "Grafos",
    description = "Curso de grafos",
    language = "es",
    tags = listOf("algoritmos"),
    moduleCount = 3,
)

internal val SAMPLE_JSON: ByteArray = """{"title":"Grafos"}""".encodeToByteArray()

internal val SAMPLE_ERROR_REPORT = CourseReport(
    errors = listOf(CourseIssue("MISSING_TITLE", "/title", "Falta el título")),
)

internal val SAMPLE_WARNING_REPORT = CourseReport(
    warnings = listOf(CourseIssue("REMOVED_IDS", "/modules", "Se eliminaron módulos")),
)

internal fun testDraft(
    id: String = "draft-1",
    userId: String = "ana",
    targetCourseId: String? = null,
    filePath: String = "course_drafts/$id.json",
    status: DraftStatus = DraftStatus.Pending(PendingAction.VALIDATING),
    updatedAt: Instant = T0,
) = CourseDraft(
    id = id,
    userId = userId,
    targetCourseId = targetCourseId,
    filePath = filePath,
    title = "Grafos",
    moduleCount = 3,
    status = status,
    updatedAt = updatedAt,
)

internal class SpyContentStore(
    private val delegate: InMemoryCourseDraftContentStore = InMemoryCourseDraftContentStore(),
) : CourseDraftContentStore {
    val readPaths = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    fun put(filePath: String, content: ByteArray) = delegate.put(filePath, content)

    override suspend fun read(filePath: String): ByteArray? {
        readPaths += filePath
        return delegate.read(filePath)
    }

    override suspend fun delete(filePath: String) {
        deletedPaths += filePath
        delegate.delete(filePath)
    }

    suspend fun peek(filePath: String): ByteArray? = delegate.read(filePath)
}

internal class ProcessDraftFixture(
    val gateway: FakeCoursePublicationGateway = FakeCoursePublicationGateway(),
    initialClock: Instant = T1,
) {
    val sessions = InMemorySessionStore()
    val drafts = InMemoryCourseDraftStore(sessions)
    val contents = SpyContentStore()
    val clock = FixedClock(initialClock)
    val invalidatedCourseIds = mutableListOf<String>()
    private val cacheInvalidator = CourseCacheInvalidator { courseId -> invalidatedCourseIds += courseId }

    val processor = ProcessCourseDraft(
        drafts = drafts,
        contents = contents,
        sessions = sessions,
        gateway = gateway,
        cacheInvalidator = cacheInvalidator,
        clock = clock,
    )

    suspend fun logIn(session: Session = testSession("ana")): Session {
        sessions.save(session)
        return session
    }

    suspend fun saveDraft(draft: CourseDraft, bytes: ByteArray? = SAMPLE_JSON): CourseDraft {
        drafts.upsert(draft)
        if (bytes != null) contents.put(draft.filePath, bytes)
        return draft
    }
}
