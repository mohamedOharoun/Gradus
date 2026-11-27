package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.lifecycle.viewmodel.MutableCreationExtras
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseFilter
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseDraftPreview
import es.ulpgc.pamn.dominio.course.publication.CourseIssue
import es.ulpgc.pamn.dominio.course.publication.CourseReport
import es.ulpgc.pamn.dominio.course.publication.DraftRejection
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.FakeCoursePublicationGateway
import es.ulpgc.pamn.dominio.course.publication.InMemoryCourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.MyCoursesOutcome
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.PendingNotice
import es.ulpgc.pamn.dominio.data.publication.CourseDraftStorage
import es.ulpgc.pamn.dominio.data.publication.CourseUploadRequester
import es.ulpgc.pamn.dominio.data.publication.DraftImportResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MyCoursesViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val defaultSession = Session(
        token = "token-1",
        expiresAt = "2026-12-31T00:00:00Z",
        account = Account(
            id = "user-1",
            name = "Ana Ruiz",
            email = "ana@example.com",
            createdAt = "2026-10-01T10:00:00Z",
        ),
    )

    private val fixedClock = object : Clock {
        var current: Instant = Instant.parse("2026-11-27T12:00:00Z")
        override fun now(): Instant = current
    }

    // --- Carga inicial y factory ---

    @Test
    fun `initial load merges published and drafts in chronological order and finishes loading`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val olderPublished = summary(id = "c-1", title = "Antiguo", publishedAt = "2026-11-20T10:00:00Z")
        val newestDraft = draft(
            id = "d-1",
            title = "Borrador reciente",
            status = DraftStatus.Accepted(),
            updatedAt = Instant.parse("2026-11-26T10:00:00Z"),
        )
        drafts.upsert(newestDraft)
        val gateway = FakeCoursePublicationGateway(
            myCoursesAnswer = { MyCoursesOutcome.Found(listOf(olderPublished)) },
        )

        val viewModel = buildViewModel(gateway = gateway, drafts = drafts, sessions = sessions)
        assertTrue(viewModel.uiState.isLoading)

        runCurrent()

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(listOf("d-1", "c-1"), viewModel.uiState.items.map { it.id })
        assertEquals(2, viewModel.uiState.allCount)
        assertEquals(
            mapOf(
                AuthorCourseFilter.ALL to 2,
                AuthorCourseFilter.PUBLISHED to 1,
                AuthorCourseFilter.ACCEPTED to 1,
                AuthorCourseFilter.PENDING to 0,
                AuthorCourseFilter.REJECTED to 0,
            ),
            viewModel.uiState.counts,
        )
    }

    @Test
    fun `initial load finishes loading when session is null so onResume can retry later`() = runTest {
        val sessions = InMemorySessionStore()
        val course = summary(id = "c-1", title = "Kotlin")
        val gateway = FakeCoursePublicationGateway(
            myCoursesAnswer = { MyCoursesOutcome.Found(listOf(course)) },
        )
        val viewModel = buildViewModel(gateway = gateway, sessions = sessions)

        runCurrent()

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(0, gateway.myCoursesCalls.size)

        sessions.save(defaultSession)
        viewModel.onResume()
        runCurrent()

        assertEquals(1, gateway.myCoursesCalls.size)
        assertEquals(listOf("c-1"), viewModel.uiState.items.map { it.id })
    }

    @Test
    fun `factory creates a functional view model using the given ports`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val gateway = FakeCoursePublicationGateway(
            myCoursesAnswer = { MyCoursesOutcome.Found(listOf(summary(id = "c-1", title = "Redes"))) },
        )
        val viewModel = MyCoursesViewModel.factory(
            gateway = gateway,
            drafts = drafts,
            storage = FakeCourseDraftStorage(),
            uploads = FakeCourseUploadRequester(),
            sessions = sessions,
        ).create(MyCoursesViewModel::class.java, MutableCreationExtras())

        runCurrent()

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(listOf("c-1"), viewModel.uiState.items.map { it.id })
    }

    // --- Filtro ---

    @Test
    fun `onSelectFilter filters items while preserving allCount and counts`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        drafts.upsert(draft(id = "d-acc", status = DraftStatus.Accepted()))
        drafts.upsert(draft(id = "d-rej", status = DraftStatus.Rejected(DraftRejection.TooLarge)))
        val gateway = FakeCoursePublicationGateway(
            myCoursesAnswer = { MyCoursesOutcome.Found(listOf(summary(id = "c-1"))) },
        )
        val viewModel = buildViewModel(gateway = gateway, drafts = drafts, sessions = sessions)
        runCurrent()

        viewModel.onSelectFilter(AuthorCourseFilter.REJECTED)

        assertEquals(AuthorCourseFilter.REJECTED, viewModel.uiState.filter)
        assertEquals(listOf("d-rej"), viewModel.uiState.items.map { it.id })
        assertEquals(3, viewModel.uiState.allCount)
        assertEquals(1, viewModel.uiState.counts[AuthorCourseFilter.PUBLISHED])
        assertEquals(1, viewModel.uiState.counts[AuthorCourseFilter.ACCEPTED])
        assertEquals(1, viewModel.uiState.counts[AuthorCourseFilter.REJECTED])
        assertEquals(0, viewModel.uiState.counts[AuthorCourseFilter.PENDING])
    }

    // --- Subir curso / nueva versión / reemplazo seguro ---

    @Test
    fun `uploading a new course imports file creates validating draft and enqueues upload`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val storage = FakeCourseDraftStorage { id, _ ->
            DraftImportResult.Imported(
                filePath = "$id.json",
                preview = CourseDraftPreview(title = "Curso Nuevo", moduleCount = 5),
            )
        }
        val uploads = FakeCourseUploadRequester()
        val viewModel = buildViewModel(
            drafts = drafts,
            storage = storage,
            uploads = uploads,
            sessions = sessions,
            idGenerator = sequenceIds("draft-new"),
        )
        runCurrent()

        viewModel.onPrepareUploadNew()
        viewModel.onFileSelected("content://docs/curso.json")
        runCurrent()

        val saved = drafts.find("draft-new")
        assertEquals("Curso Nuevo", saved?.title)
        assertEquals(5, saved?.moduleCount)
        assertNull(saved?.targetCourseId)
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING), saved?.status)
        assertEquals(fixedClock.current, saved?.updatedAt)
        assertEquals(listOf("draft-new"), uploads.enqueued)
        assertEquals(listOf("draft-new"), viewModel.uiState.items.map { it.id })
    }

    @Test
    fun `uploading a new version of a published course sets targetCourseId to published id`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val course = summary(id = "course-42", title = "Grafos")
        val gateway = FakeCoursePublicationGateway(
            myCoursesAnswer = { MyCoursesOutcome.Found(listOf(course)) },
        )
        val uploads = FakeCourseUploadRequester()
        val viewModel = buildViewModel(
            gateway = gateway,
            drafts = drafts,
            uploads = uploads,
            sessions = sessions,
            idGenerator = sequenceIds("draft-v2"),
        )
        runCurrent()

        viewModel.onPrepareUploadVersion(AuthorCourseItem(published = course))
        viewModel.onFileSelected("content://docs/grafos-v2.json")
        runCurrent()

        val saved = drafts.find("draft-v2")
        assertEquals("course-42", saved?.targetCourseId)
        assertEquals(listOf("draft-v2"), uploads.enqueued)
        assertEquals(1, viewModel.uiState.items.size)
        assertEquals("draft-v2", viewModel.uiState.items.single().draft?.id)
    }

    @Test
    fun `re-uploading over an existing draft replaces it safely without triggering background refresh`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val existing = draft(
            id = "draft-old",
            targetCourseId = "course-7",
            filePath = "draft-old.json",
            status = DraftStatus.Pending(PendingAction.PUBLISHING),
        )
        drafts.upsert(existing)
        val storage = FakeCourseDraftStorage()
        val uploads = FakeCourseUploadRequester()
        val gateway = FakeCoursePublicationGateway()
        val viewModel = buildViewModel(
            gateway = gateway,
            drafts = drafts,
            storage = storage,
            uploads = uploads,
            sessions = sessions,
            idGenerator = sequenceIds("draft-new"),
        )
        runCurrent()
        assertEquals(1, gateway.myCoursesCalls.size)

        viewModel.onPrepareUploadVersion(AuthorCourseItem(draft = existing))
        viewModel.onFileSelected("content://docs/fixed.json")
        runCurrent()

        assertNull(drafts.find("draft-old"))
        val replacement = drafts.find("draft-new")
        assertEquals("course-7", replacement?.targetCourseId)
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING), replacement?.status)
        assertEquals(listOf("draft-old"), uploads.cancelled)
        assertEquals(listOf("draft-old.json"), storage.deletedPaths)
        assertEquals(listOf("draft-new"), uploads.enqueued)
        // Reemplazar un borrador en PUBLISHING no debe disparar refresco automático de myCourses.
        assertEquals(1, gateway.myCoursesCalls.size)
    }

    @Test
    fun `re-uploading with TooLarge exposes notice and keeps previous draft intact`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val existing = draft(id = "draft-old", status = DraftStatus.Rejected(DraftRejection.TooLarge))
        drafts.upsert(existing)
        val storage = FakeCourseDraftStorage { _, _ -> DraftImportResult.TooLarge }
        val uploads = FakeCourseUploadRequester()
        val viewModel = buildViewModel(
            drafts = drafts,
            storage = storage,
            uploads = uploads,
            sessions = sessions,
            idGenerator = sequenceIds("draft-big"),
        )
        runCurrent()

        viewModel.onPrepareUploadVersion(AuthorCourseItem(draft = existing))
        viewModel.onFileSelected("content://docs/huge.json")
        runCurrent()

        assertEquals(MyCoursesInlineNotice.FILE_TOO_LARGE, viewModel.uiState.inlineNotice)
        assertEquals(existing, drafts.find("draft-old"))
        assertNull(drafts.find("draft-big"))
        assertTrue(uploads.enqueued.isEmpty())
        assertTrue(uploads.cancelled.isEmpty())
        assertTrue(storage.deletedPaths.isEmpty())
    }

    @Test
    fun `re-uploading with Unreadable exposes notice keeps previous draft and valid import clears notice`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val existing = draft(id = "draft-old", status = DraftStatus.Accepted())
        drafts.upsert(existing)
        var importResult: DraftImportResult = DraftImportResult.Unreadable
        val storage = FakeCourseDraftStorage { _, _ -> importResult }
        val viewModel = buildViewModel(
            drafts = drafts,
            storage = storage,
            sessions = sessions,
            idGenerator = sequenceIds("draft-bad", "draft-good"),
        )
        runCurrent()

        viewModel.onPrepareUploadVersion(AuthorCourseItem(draft = existing))
        viewModel.onFileSelected("content://docs/broken.json")
        runCurrent()

        assertEquals(MyCoursesInlineNotice.FILE_UNREADABLE, viewModel.uiState.inlineNotice)
        assertEquals(existing, drafts.find("draft-old"))

        importResult = DraftImportResult.Imported(
            filePath = "draft-good.json",
            preview = CourseDraftPreview(title = "Recuperado", moduleCount = 3),
        )
        viewModel.onPrepareUploadVersion(AuthorCourseItem(draft = existing))
        viewModel.onFileSelected("content://docs/ok.json")
        runCurrent()

        assertNull(viewModel.uiState.inlineNotice)
        assertEquals("Recuperado", drafts.find("draft-good")?.title)
    }

    @Test
    fun `onFileSelectionCancelled clears pending upload target`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val storage = FakeCourseDraftStorage()
        val uploads = FakeCourseUploadRequester()
        val viewModel = buildViewModel(
            drafts = drafts,
            storage = storage,
            uploads = uploads,
            sessions = sessions,
            idGenerator = sequenceIds("draft-1"),
        )
        runCurrent()

        viewModel.onPrepareUploadNew()
        viewModel.onFileSelectionCancelled()
        viewModel.onFileSelected("content://docs/ignored.json")
        runCurrent()

        assertNull(drafts.find("draft-1"))
        assertTrue(storage.importedUris.isEmpty())
        assertTrue(uploads.enqueued.isEmpty())
    }

    // --- Publicar ---

    @Test
    fun `onPublish transitions accepted draft to pending publishing closes its report and enqueues`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val warnings = CourseReport(
            warnings = listOf(CourseIssue("REMOVED_IDS", "/modules", "Se eliminaron módulos.")),
        )
        val accepted = draft(id = "d-1", title = "Sistemas", status = DraftStatus.Accepted(warnings))
        drafts.upsert(accepted)
        val uploads = FakeCourseUploadRequester()
        val viewModel = buildViewModel(drafts = drafts, uploads = uploads, sessions = sessions)
        runCurrent()

        viewModel.onOpenReport(AuthorCourseItem(draft = accepted))
        assertEquals("Sistemas", viewModel.uiState.openReport?.courseTitle)

        viewModel.onPublish("d-1")
        runCurrent()

        val updated = drafts.find("d-1")
        assertEquals(DraftStatus.Pending(PendingAction.PUBLISHING), updated?.status)
        assertEquals(listOf("d-1"), uploads.enqueued)
        assertNull(viewModel.uiState.openReport)
    }

    @Test
    fun `onPublish ignores drafts that are not in Accepted state or do not exist`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        drafts.upsert(draft(id = "d-pend", status = DraftStatus.Pending(PendingAction.VALIDATING)))
        drafts.upsert(draft(id = "d-rej", status = DraftStatus.Rejected(DraftRejection.NotAuthor)))
        val uploads = FakeCourseUploadRequester()
        val viewModel = buildViewModel(drafts = drafts, uploads = uploads, sessions = sessions)
        runCurrent()

        viewModel.onPublish("d-pend")
        viewModel.onPublish("d-rej")
        viewModel.onPublish("d-missing")
        runCurrent()

        assertTrue(uploads.enqueued.isEmpty())
        assertEquals(DraftStatus.Pending(PendingAction.VALIDATING), drafts.find("d-pend")?.status)
        assertEquals(DraftStatus.Rejected(DraftRejection.NotAuthor), drafts.find("d-rej")?.status)
    }

    // --- Publicación en segundo plano ---

    @Test
    fun `background publish completion refreshes myCourses automatically`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        drafts.upsert(
            draft(
                id = "d-pub",
                status = DraftStatus.Pending(PendingAction.PUBLISHING, notice = PendingNotice.NETWORK),
            ),
        )
        var remoteCourses = emptyList<CourseSummary>()
        val gateway = FakeCoursePublicationGateway(
            myCoursesAnswer = { MyCoursesOutcome.Found(remoteCourses) },
        )
        val viewModel = buildViewModel(gateway = gateway, drafts = drafts, sessions = sessions)
        runCurrent()
        assertEquals(1, gateway.myCoursesCalls.size)

        // Simula el worker terminando de publicar: el servidor ya tiene el curso y el borrador se borra.
        remoteCourses = listOf(summary(id = "c-published", title = "Publicado en segundo plano"))
        drafts.delete("d-pub")
        runCurrent()

        assertEquals(2, gateway.myCoursesCalls.size)
        assertEquals(listOf("c-published"), viewModel.uiState.items.map { it.id })
        assertFalse(viewModel.uiState.isLoading)
    }

    // --- Descartar ---

    @Test
    fun `confirming discard cancels work deletes draft and file closes report and does not refresh`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val report = CourseReport(
            warnings = listOf(CourseIssue("SHORT_DESCRIPTION", "/description", "Muy breve.")),
        )
        val publishingDraft = draft(
            id = "d-discard",
            filePath = "d-discard.json",
            status = DraftStatus.Pending(PendingAction.PUBLISHING),
        )
        val acceptedDraft = draft(
            id = "d-report",
            filePath = "d-report.json",
            status = DraftStatus.Accepted(report),
        )
        drafts.upsert(publishingDraft)
        drafts.upsert(acceptedDraft)
        val storage = FakeCourseDraftStorage()
        val uploads = FakeCourseUploadRequester()
        val gateway = FakeCoursePublicationGateway()
        val viewModel = buildViewModel(
            gateway = gateway,
            drafts = drafts,
            storage = storage,
            uploads = uploads,
            sessions = sessions,
        )
        runCurrent()
        assertEquals(1, gateway.myCoursesCalls.size)

        // Descartar un borrador con informe abierto cierra el informe y borra fila y fichero.
        viewModel.onOpenReport(AuthorCourseItem(draft = acceptedDraft))
        viewModel.onRequestDiscard(acceptedDraft)
        assertEquals(acceptedDraft, viewModel.uiState.pendingDiscard)
        viewModel.onConfirmDiscard()
        runCurrent()

        assertNull(viewModel.uiState.pendingDiscard)
        assertNull(viewModel.uiState.openReport)
        assertNull(drafts.find("d-report"))
        assertEquals(listOf("d-report"), uploads.cancelled)
        assertEquals(listOf("d-report.json"), storage.deletedPaths)

        // Descartar un borrador en PUBLISHING NO debe disparar refresco automático de publicación.
        viewModel.onRequestDiscard(publishingDraft)
        viewModel.onConfirmDiscard()
        runCurrent()

        assertNull(drafts.find("d-discard"))
        assertEquals(listOf("d-report", "d-discard"), uploads.cancelled)
        assertEquals(1, gateway.myCoursesCalls.size)
    }

    @Test
    fun `onDismissDiscard closes dialog without deleting the draft`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val existing = draft(id = "d-keep")
        drafts.upsert(existing)
        val viewModel = buildViewModel(drafts = drafts, sessions = sessions)
        runCurrent()

        viewModel.onRequestDiscard(existing)
        assertEquals(existing, viewModel.uiState.pendingDiscard)

        viewModel.onDismissDiscard()
        assertNull(viewModel.uiState.pendingDiscard)
        assertEquals(existing, drafts.find("d-keep"))
    }

    // --- Informe ---

    @Test
    fun `onOpenReport opens for validation rejection and accepted with warnings and ignores others`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val errorReport = CourseReport(
            errors = listOf(CourseIssue("MISSING_FIELD", "/title", "Falta el título.")),
        )
        val warningReport = CourseReport(
            warnings = listOf(CourseIssue("REMOVED_IDS", "/modules", "Módulos eliminados.")),
        )
        val rejected = draft(id = "d-rej", title = "Con error", status = DraftStatus.Rejected(DraftRejection.ValidationReport(errorReport)))
        val acceptedWithWarnings = draft(id = "d-warn", title = "Con avisos", status = DraftStatus.Accepted(warningReport))
        val acceptedClean = draft(id = "d-clean", title = "Limpio", status = DraftStatus.Accepted())
        val rejectedWithoutReport = draft(id = "d-ident", title = "Idéntico", status = DraftStatus.Rejected(DraftRejection.IdenticalToLatest))
        val pending = draft(id = "d-pend", title = "Pendiente", status = DraftStatus.Pending(PendingAction.VALIDATING))
        val publishedOnly = AuthorCourseItem(published = summary(id = "c-1", title = "Publicado"))
        listOf(rejected, acceptedWithWarnings, acceptedClean, rejectedWithoutReport, pending).forEach { drafts.upsert(it) }

        val viewModel = buildViewModel(drafts = drafts, sessions = sessions)
        runCurrent()

        viewModel.onOpenReport(publishedOnly)
        viewModel.onOpenReport(AuthorCourseItem(draft = acceptedClean))
        viewModel.onOpenReport(AuthorCourseItem(draft = rejectedWithoutReport))
        viewModel.onOpenReport(AuthorCourseItem(draft = pending))
        assertNull(viewModel.uiState.openReport)

        viewModel.onOpenReport(AuthorCourseItem(draft = rejected))
        assertEquals(OpenCourseReport("Con error", errorReport, isRejection = true), viewModel.uiState.openReport)

        viewModel.onOpenReport(AuthorCourseItem(draft = acceptedWithWarnings))
        assertEquals(OpenCourseReport("Con avisos", warningReport, isRejection = false), viewModel.uiState.openReport)

        viewModel.onCloseReport()
        assertNull(viewModel.uiState.openReport)
    }

    @Test
    fun `open report and pending discard close automatically when their draft disappears from store`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        val warningReport = CourseReport(
            warnings = listOf(CourseIssue("SHORT_DESCRIPTION", "/description", "Breve.")),
        )
        val target = draft(id = "d-vanish", status = DraftStatus.Accepted(warningReport))
        drafts.upsert(target)
        val viewModel = buildViewModel(drafts = drafts, sessions = sessions)
        runCurrent()

        viewModel.onOpenReport(AuthorCourseItem(draft = target))
        viewModel.onRequestDiscard(target)
        assertEquals(target, viewModel.uiState.pendingDiscard)
        assertEquals(warningReport, viewModel.uiState.openReport?.report)

        drafts.delete("d-vanish")
        runCurrent()

        assertNull(viewModel.uiState.openReport)
        assertNull(viewModel.uiState.pendingDiscard)
    }

    // --- Errores de red, servidor y 401 ---

    @Test
    fun `unreachable keeps local drafts shows notice dismisses and clears on retry recovery`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val drafts = InMemoryCourseDraftStore(sessions)
        drafts.upsert(draft(id = "d-local", title = "Local sin red"))
        var outcome: MyCoursesOutcome = MyCoursesOutcome.Unreachable
        val gateway = FakeCoursePublicationGateway(myCoursesAnswer = { outcome })
        val viewModel = buildViewModel(gateway = gateway, drafts = drafts, sessions = sessions)
        runCurrent()

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(MyCoursesInlineNotice.UNREACHABLE, viewModel.uiState.inlineNotice)
        assertEquals(listOf("d-local"), viewModel.uiState.items.map { it.id })

        viewModel.onDismissNotice()
        assertNull(viewModel.uiState.inlineNotice)

        // Nuevo fallo al reintentar vuelve a mostrar el aviso.
        viewModel.onRetry()
        runCurrent()
        assertEquals(MyCoursesInlineNotice.UNREACHABLE, viewModel.uiState.inlineNotice)

        // Al recuperarse, limpia el aviso y añade los cursos del servidor.
        outcome = MyCoursesOutcome.Found(listOf(summary(id = "c-remote", title = "Remoto")))
        viewModel.onRetry()
        runCurrent()
        assertNull(viewModel.uiState.inlineNotice)
        assertEquals(setOf("d-local", "c-remote"), viewModel.uiState.items.mapTo(HashSet()) { it.id })
    }

    @Test
    fun `server failure keeps previously loaded published courses and shows notice`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        var outcome: MyCoursesOutcome = MyCoursesOutcome.Found(listOf(summary(id = "c-1")))
        val gateway = FakeCoursePublicationGateway(myCoursesAnswer = { outcome })
        val viewModel = buildViewModel(gateway = gateway, sessions = sessions)
        runCurrent()
        assertEquals(listOf("c-1"), viewModel.uiState.items.map { it.id })

        outcome = MyCoursesOutcome.ServerFailure
        viewModel.onResume()
        runCurrent()

        assertEquals(MyCoursesInlineNotice.SERVER_FAILURE, viewModel.uiState.inlineNotice)
        assertEquals(listOf("c-1"), viewModel.uiState.items.map { it.id })
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun `session expired clears current session in SessionStore`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val gateway = FakeCoursePublicationGateway(myCoursesAnswer = { MyCoursesOutcome.SessionExpired })
        val viewModel = buildViewModel(gateway = gateway, sessions = sessions)
        runCurrent()

        assertNull(sessions.current())
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun `session expired does not clear a newer session saved while request was in flight`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val responseGate = CompletableDeferred<MyCoursesOutcome>()
        val gateway = FakeCoursePublicationGateway(myCoursesAnswer = { responseGate.await() })
        buildViewModel(gateway = gateway, sessions = sessions)
        runCurrent()

        val newerSession = defaultSession.copy(token = "token-2")
        sessions.save(newerSession)
        responseGate.complete(MyCoursesOutcome.SessionExpired)
        runCurrent()

        assertEquals(newerSession, sessions.current())
    }

    // --- onResume ---

    @Test
    fun `onResume refreshes silently without activating isLoading and skips while refresh is active`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val initialGate = CompletableDeferred<MyCoursesOutcome>()
        val resumeGate = CompletableDeferred<MyCoursesOutcome>()
        var callCount = 0
        val gateway = FakeCoursePublicationGateway(
            myCoursesAnswer = {
                callCount++
                when (callCount) {
                    1 -> initialGate.await()
                    2 -> resumeGate.await()
                    else -> MyCoursesOutcome.Found(emptyList())
                }
            },
        )
        val viewModel = buildViewModel(gateway = gateway, sessions = sessions)
        runCurrent()

        // Antes de terminar la carga inicial, onResume no lanza otra petición.
        viewModel.onResume()
        runCurrent()
        assertEquals(1, gateway.myCoursesCalls.size)

        initialGate.complete(MyCoursesOutcome.Found(listOf(summary(id = "c-1"))))
        runCurrent()
        assertFalse(viewModel.uiState.isLoading)

        // Tras cargar, onResume refresca en silencio (isLoading sigue en false).
        viewModel.onResume()
        runCurrent()
        assertFalse(viewModel.uiState.isLoading)
        assertEquals(2, gateway.myCoursesCalls.size)

        // Otro onResume mientras sigue en vuelo tampoco duplica la petición.
        viewModel.onResume()
        runCurrent()
        assertEquals(2, gateway.myCoursesCalls.size)

        resumeGate.complete(MyCoursesOutcome.Found(listOf(summary(id = "c-1"), summary(id = "c-2"))))
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.allCount)
        assertFalse(viewModel.uiState.isLoading)
    }

    // --- actions() ---

    @Test
    fun `actions delegates callbacks and preserves equality for the same external lambdas`() = runTest {
        val sessions = sessionStoreWith(defaultSession)
        val viewModel = buildViewModel(sessions = sessions)
        runCurrent()

        val onUpload: () -> Unit = {}
        val onVersion: (AuthorCourseItem) -> Unit = {}
        val onGuide: () -> Unit = {}

        val first = viewModel.actions(onUpload, onVersion, onGuide)
        val second = viewModel.actions(onUpload, onVersion, onGuide)
        assertSame(onUpload, first.onUploadCourse)
        assertSame(onVersion, first.onUploadNewVersion)
        assertSame(onGuide, first.onOpenGuide)
        assertEquals(first, second)
        assertNotEquals(first, viewModel.actions({}, onVersion, onGuide))

        first.onSelectFilter(AuthorCourseFilter.PUBLISHED)
        assertEquals(AuthorCourseFilter.PUBLISHED, viewModel.uiState.filter)
    }

    // --- Ayudantes y fakes locales ---

    private suspend fun sessionStoreWith(session: Session): InMemorySessionStore =
        InMemorySessionStore().apply { save(session) }

    private fun buildViewModel(
        gateway: FakeCoursePublicationGateway = FakeCoursePublicationGateway(),
        sessions: InMemorySessionStore = InMemorySessionStore(),
        drafts: InMemoryCourseDraftStore = InMemoryCourseDraftStore(sessions),
        storage: FakeCourseDraftStorage = FakeCourseDraftStorage(),
        uploads: FakeCourseUploadRequester = FakeCourseUploadRequester(),
        idGenerator: () -> String = sequenceIds("draft-1", "draft-2", "draft-3"),
    ) = MyCoursesViewModel(
        gateway = gateway,
        drafts = drafts,
        storage = storage,
        uploads = uploads,
        sessions = sessions,
        clock = fixedClock,
        idGenerator = idGenerator,
    )

    private fun sequenceIds(vararg ids: String): () -> String {
        var index = 0
        return { ids[index++] }
    }

    private fun summary(
        id: String,
        title: String = id,
        version: Int = 1,
        publishedAt: String = "2026-11-20T10:00:00Z",
    ) = CourseSummary(
        id = id,
        version = version,
        author = Author(id = defaultSession.account.id, name = defaultSession.account.name),
        publishedAt = publishedAt,
        title = title,
        description = "",
        language = "es",
        tags = emptyList(),
        moduleCount = 4,
    )

    private fun draft(
        id: String,
        title: String = id,
        targetCourseId: String? = null,
        filePath: String = "$id.json",
        status: DraftStatus = DraftStatus.Pending(PendingAction.VALIDATING),
        updatedAt: Instant = Instant.parse("2026-11-25T10:00:00Z"),
    ) = CourseDraft(
        id = id,
        userId = defaultSession.account.id,
        targetCourseId = targetCourseId,
        filePath = filePath,
        title = title,
        moduleCount = 3,
        status = status,
        updatedAt = updatedAt,
    )

    private class FakeCourseDraftStorage(
        private val onImport: suspend (String, String) -> DraftImportResult = { draftId, _ ->
            DraftImportResult.Imported(
                filePath = "$draftId.json",
                preview = CourseDraftPreview(title = "Curso $draftId", moduleCount = 2),
            )
        },
    ) : CourseDraftStorage {
        val importedUris = mutableListOf<Pair<String, String>>()
        val deletedPaths = mutableListOf<String>()

        override suspend fun import(draftId: String, sourceUri: String): DraftImportResult {
            importedUris += draftId to sourceUri
            return onImport(draftId, sourceUri)
        }

        override suspend fun read(filePath: String): ByteArray? = null

        override suspend fun delete(filePath: String) {
            deletedPaths += filePath
        }

        override suspend fun deleteAll(filePaths: Iterable<String>) {
            deletedPaths += filePaths
        }
    }

    private class FakeCourseUploadRequester : CourseUploadRequester {
        val enqueued = mutableListOf<String>()
        val cancelled = mutableListOf<String>()

        override fun enqueue(draftId: String) {
            enqueued += draftId
        }

        override fun cancel(draftId: String) {
            cancelled += draftId
        }
    }
}
