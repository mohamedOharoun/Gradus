package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.study.PhotoTask
import es.ulpgc.pamn.dominio.course.study.StepState
import es.ulpgc.pamn.dominio.course.study.StudyStage
import es.ulpgc.pamn.dominio.submission.FakeSubmissionGateway
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.PhotoSubmission
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

// El paso con foto en el estudio de un módulo (docs/tareas/ejercicio-foto.md, pasos 4a y 5b): elegir la
// foto, entregarla, los fallos, «Entregar otra vez», «Entregar más tarde» y borrar las fotos temporales.
// Tiempo simulado: nada de lo que va en segundo plano corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val photo = Step.Photo("foto", "Dibuja el grafo.", "Cuatro vértices.")
    private val lesson = Step.Lesson("l1", "Un **grafo**…")
    private val published = Publication(Author("u1", "Autora"), version = 3, publishedAt = "2026-11-20T10:00:00Z")
    private val course = Course("c1", "Curso", listOf(Module("m", "Con foto", steps = listOf(photo, lesson))), publication = published)
    private val session = Session("tok", "2026-12-25T00:00:00Z", Account("u2", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))

    private val photos = FakeSubmissionPhotos(unreadable = setOf("content://rota", "/cache/captures/rota.jpg"))
    private val sessions = InMemorySessionStore()

    private suspend fun TestScope.viewModel(
        gateway: FakeSubmissionGateway = FakeSubmissionGateway(),
        studied: Course = course,
        withSession: Boolean = true,
    ): ExerciseViewModel {
        if (withSession) sessions.save(session)
        return ExerciseViewModel(studied, "m", InMemoryProgressStore(), Random(1), gateway, sessions, photos, cleanup = this)
    }

    private val ExerciseViewModel.task get() = study!!.currentPhoto!!.task

    private suspend fun TestScope.withPhoto(gateway: FakeSubmissionGateway = FakeSubmissionGateway()): ExerciseViewModel {
        val viewModel = viewModel(gateway)
        viewModel.onPhotoChosen("content://galeria/1")
        runCurrent()
        return viewModel
    }

    // ── Elegir la foto ──

    @Test
    fun `a chosen photo is prepared and becomes ready to send`() = runTest {
        val viewModel = withPhoto()
        assertEquals(listOf("content://galeria/1"), photos.prepared)
        assertEquals(PhotoTask.Ready("preparada-1.jpg"), viewModel.task)
        assertNull(viewModel.photoNotice)
    }

    @Test
    fun `while the photo is being prepared the screen says so`() = runTest {
        val viewModel = viewModel()
        viewModel.onPhotoChosen("content://galeria/1")
        assertEquals(PhotoNotice.PREPARING, viewModel.photoNotice)
        runCurrent()
        assertNull(viewModel.photoNotice)
    }

    @Test
    fun `a photo that cannot be read is reported and nothing changes`() = runTest {
        val viewModel = viewModel()
        viewModel.onPhotoChosen("content://rota")
        runCurrent()
        assertEquals(PhotoNotice.UNREADABLE, viewModel.photoNotice)
        assertEquals(PhotoTask.Empty, viewModel.task)
    }

    // La foto de la cámara es temporal: se prepara para entregarla y la de la cámara se tira.
    @Test
    fun `a photo from the camera is prepared and the capture is thrown away`() = runTest {
        val viewModel = viewModel()
        viewModel.onPhotoTaken("/cache/captures/c1.jpg")
        runCurrent()
        assertEquals(listOf("/cache/captures/c1.jpg"), photos.preparedCaptures)
        assertEquals(PhotoTask.Ready("preparada-1.jpg"), viewModel.task)
    }

    @Test
    fun `a photo from the camera that cannot be read is reported`() = runTest {
        val viewModel = viewModel()
        viewModel.onPhotoTaken("/cache/captures/rota.jpg")
        runCurrent()
        assertEquals(PhotoNotice.UNREADABLE, viewModel.photoNotice)
        assertEquals(PhotoTask.Empty, viewModel.task)
    }

    // Si llega cuando el paso ya no acepta foto (se está entregando), no se prepara y no se queda en el móvil.
    @Test
    fun `a photo from the camera that arrives while sending is thrown away`() = runTest {
        val viewModel = withPhoto()
        viewModel.onSubmitPhoto()
        viewModel.onPhotoTaken("/cache/captures/c2.jpg")
        runCurrent()
        assertTrue(photos.preparedCaptures.isEmpty())
        assertEquals(listOf("/cache/captures/c2.jpg"), photos.discardedCaptures)
    }

    // Decisión K: la foto que ya no hace falta se borra del móvil.
    @Test
    fun `a new photo replaces the previous one, which is deleted`() = runTest {
        val viewModel = withPhoto()
        viewModel.onPhotoChosen("content://galeria/2")
        runCurrent()
        assertEquals(PhotoTask.Ready("preparada-2.jpg"), viewModel.task)
        assertEquals(listOf("preparada-1.jpg"), photos.deleted)
    }

    @Test
    fun `discarding the photo deletes it`() = runTest {
        val viewModel = withPhoto()
        viewModel.onDiscardPhoto()
        runCurrent()
        assertEquals(PhotoTask.Empty, viewModel.task)
        assertEquals(listOf("preparada-1.jpg"), photos.deleted)
    }

    // ── Entregar ──

    @Test
    fun `submitting sends the photo of that exercise and version with the session`() = runTest {
        val gateway = FakeSubmissionGateway()
        val viewModel = withPhoto(gateway)
        val bytes = photos.files.getValue("preparada-1.jpg")
        viewModel.onSubmitPhoto()
        runCurrent()

        assertEquals(listOf(PhotoSubmission("c1", 3, "foto", bytes)), gateway.received)
        assertEquals(listOf(session), gateway.sessions)
        assertEquals(PhotoTask.Sent, viewModel.task)
        assertTrue(viewModel.study!!.canContinue)
    }

    @Test
    fun `while sending the step waits`() = runTest {
        val viewModel = withPhoto()
        viewModel.onSubmitPhoto()
        assertEquals(PhotoTask.Sending("preparada-1.jpg"), viewModel.task)
        assertFalse(viewModel.study!!.canContinue)
    }

    // Decisión K: en cuanto el servidor confirma la entrega, la foto se borra del móvil.
    @Test
    fun `once sent the photo is deleted`() = runTest {
        val viewModel = withPhoto()
        viewModel.onSubmitPhoto()
        runCurrent()
        assertEquals(listOf("preparada-1.jpg"), photos.deleted)
    }

    // Dos toques seguidos en «Entregar» hacen una sola entrega.
    @Test
    fun `submitting twice in a row sends once`() = runTest {
        val gateway = FakeSubmissionGateway()
        val viewModel = withPhoto(gateway)
        viewModel.onSubmitPhoto()
        viewModel.onSubmitPhoto()
        runCurrent()
        assertEquals(1, gateway.received.size)
    }

    // Decisión J: sin conexión, la foto se conserva y se puede reintentar.
    @Test
    fun `without connection the photo is kept and can be sent again`() = runTest {
        val answers = ArrayDeque(listOf(SubmitOutcome.Unreachable, SubmitOutcome.Submitted(Submission("s1", "2026-11-25T10:00:00Z"))))
        val gateway = FakeSubmissionGateway { _, _ -> answers.removeFirst() }
        val viewModel = withPhoto(gateway)

        viewModel.onSubmitPhoto()
        runCurrent()
        assertEquals(PhotoTask.Failed("preparada-1.jpg", SubmitOutcome.Unreachable), viewModel.task)
        assertTrue(photos.deleted.isEmpty())

        viewModel.onSubmitPhoto()
        runCurrent()
        assertEquals(PhotoTask.Sent, viewModel.task)
        assertEquals(2, gateway.received.size)
    }

    @Test
    fun `a rejected photo is reported`() = runTest {
        val viewModel = withPhoto(FakeSubmissionGateway(SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG)))
        viewModel.onSubmitPhoto()
        runCurrent()
        assertEquals(PhotoTask.Failed("preparada-1.jpg", SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG)), viewModel.task)
    }

    // Decisión Y: sin sesión (caducada y olvidada) no se llega a enviar; se avisa como sesión caducada.
    @Test
    fun `without a session nothing is sent and it is an expired session`() = runTest {
        val gateway = FakeSubmissionGateway()
        val viewModel = viewModel(gateway, withSession = false)
        viewModel.onPhotoChosen("content://galeria/1")
        runCurrent()

        viewModel.onSubmitPhoto()
        runCurrent()

        assertTrue(gateway.received.isEmpty())
        assertEquals(PhotoTask.Failed("preparada-1.jpg", SubmitOutcome.SessionExpired), viewModel.task)
    }

    // Android puede vaciar la caché: si la foto ya no está, hay que hacer otra.
    @Test
    fun `a photo that is no longer on the phone is reported as missing`() = runTest {
        val gateway = FakeSubmissionGateway()
        val viewModel = withPhoto(gateway)
        photos.files.clear()

        viewModel.onSubmitPhoto()
        runCurrent()

        assertTrue(gateway.received.isEmpty())
        assertEquals(PhotoTask.Failed("preparada-1.jpg", SubmitOutcome.PhotoRejected(PhotoRejection.MISSING)), viewModel.task)
    }

    // Decisión Z: un curso que no está publicado (el de ejemplo) no tiene versión a la que entregar.
    @Test
    fun `a course that is not published cannot receive photos`() = runTest {
        val gateway = FakeSubmissionGateway()
        val viewModel = viewModel(gateway, studied = course.copy(publication = null))
        assertFalse(viewModel.canSubmitPhotos)

        viewModel.onPhotoChosen("content://galeria/1")
        runCurrent()
        viewModel.onSubmitPhoto()
        runCurrent()

        assertTrue(gateway.received.isEmpty())
        assertEquals(PhotoTask.Ready("preparada-1.jpg"), viewModel.task)
    }

    @Test
    fun `a published course can receive photos`() = runTest {
        assertTrue(viewModel().canSubmitPhotos)
    }

    // Decisión I: «Entregar otra vez» empieza sin foto, pero se puede seguir (cuenta la anterior).
    @Test
    fun `submitting again starts without a photo and the student can still go on`() = runTest {
        val viewModel = withPhoto()
        viewModel.onSubmitPhoto()
        runCurrent()

        viewModel.onSubmitAgain()

        assertEquals(PhotoTask.Empty, viewModel.task)
        assertTrue(viewModel.study!!.canContinue)
    }

    // Mientras se prepara o se entrega no se cambia la foto: el toque se ignora sin preparar nada.
    @Test
    fun `a photo chosen while sending is ignored`() = runTest {
        val viewModel = withPhoto()
        viewModel.onSubmitPhoto()
        viewModel.onPhotoChosen("content://galeria/2")
        runCurrent()
        assertEquals(listOf("content://galeria/1"), photos.prepared)
    }

    // ── Más tarde y salir (decisión K) ──

    // Un solo toque: la deja para más tarde, borra la foto y pasa al paso siguiente.
    @Test
    fun `leaving the photo for later deletes it and goes on to the next step`() = runTest {
        val viewModel = withPhoto()
        viewModel.onSubmitPhotoLater()
        runCurrent()

        val stage = viewModel.study!!.stage as StudyStage.Learning
        assertEquals(StepState.Reading(lesson), stage.steps.current)
        assertEquals(listOf("preparada-1.jpg"), photos.deleted)
    }

    // En una lección no hay foto que dejar: no cambia nada (ni se salta la lección).
    @Test
    fun `leaving for later at a lesson changes nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.onSubmitPhotoLater()
        val atLesson = viewModel.study

        viewModel.onSubmitPhotoLater()

        assertEquals(atLesson, viewModel.study)
    }

    @Test
    fun `leaving the module deletes the photos still on the phone`() = runTest {
        val viewModel = withPhoto()
        viewModel.onLeave()
        runCurrent()
        assertEquals(listOf("preparada-1.jpg"), photos.deleted)
    }
}
