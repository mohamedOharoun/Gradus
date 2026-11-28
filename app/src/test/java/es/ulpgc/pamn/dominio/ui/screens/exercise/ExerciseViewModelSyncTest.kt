package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.submission.FakeSubmissionGateway
import es.ulpgc.pamn.dominio.ui.screens.course.loggedInStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

// Sincronizar al dominar (8b, C-5): se pide justo después de markMastered, solo en el momento de
// aprobar la prueba de dominio y no en cada «Continuar» posterior.
// Curso mínimo: un módulo sin lecciones y con una sola pregunta en la prueba (hay que acertarla).
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelSyncTest {

    private val module = Module(
        id = "m",
        title = "Módulo",
        review = listOf(SingleChoice("q1", "¿Una pregunta?", listOf("Bien", "Mal"), correct = 0)),
        mastery = Mastery(questions = 1, minCorrect = 1),
    )
    private val course = Course(id = "c", title = "Curso", modules = listOf(module))

    private var requests = 0

    // Si markMastered ya se había llamado en el momento de pedir sincronizar.
    private val markedOnRequest = mutableListOf<Boolean>()
    private val requester = ProgressSyncRequester {
        requests++
        markedOnRequest += spy.marked
    }

    private lateinit var spy: MarkedFlagProgressStore

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `passing the mastery test requests a sync once`() = runTest {
        val viewModel = openEnrolled()
        pass(viewModel)
        assertTrue(viewModel.study!!.isPassed)
        assertEquals(1, requests)
    }

    @Test
    fun `the sync is requested after markMastered, not before`() = runTest {
        val viewModel = openEnrolled()
        pass(viewModel)
        assertEquals(listOf(true), markedOnRequest)
    }

    @Test
    fun `continuing after passing does not request again`() = runTest {
        val viewModel = openEnrolled()
        pass(viewModel)
        viewModel.onContinue()
        runCurrent()
        viewModel.onContinue()
        runCurrent()
        assertEquals(1, requests)
    }

    @Test
    fun `failing the mastery test requests nothing`() = runTest {
        val viewModel = openEnrolled()
        viewModel.onStartTest()
        viewModel.onChoose(1)
        viewModel.onCheck()
        viewModel.onContinue()
        runCurrent()
        assertTrue(!viewModel.study!!.isPassed)
        assertEquals(0, requests)
    }

    @Test
    fun `answering and checking without continuing requests nothing`() = runTest {
        val viewModel = openEnrolled()
        viewModel.onStartTest()
        viewModel.onChoose(0)
        viewModel.onCheck()
        runCurrent()
        assertEquals(0, requests)
    }

    @Test
    fun `starting the test requests nothing`() = runTest {
        val viewModel = openEnrolled()
        viewModel.onStartTest()
        runCurrent()
        assertEquals(0, requests)
    }

    @Test
    fun `passing after a failed attempt requests once`() = runTest {
        val viewModel = openEnrolled()
        viewModel.onStartTest()
        viewModel.onChoose(1)
        viewModel.onCheck()
        viewModel.onContinue()
        runCurrent()
        viewModel.onRetry()
        viewModel.onChoose(0)
        viewModel.onCheck()
        viewModel.onContinue()
        runCurrent()
        assertEquals(1, requests)
    }

    // markMastered lanza sin inscripción: la excepción sale y no se pide sincronizar.
    @Test
    fun `when markMastered fails nothing is requested`() {
        assertThrows(IllegalStateException::class.java) {
            runTest {
                spy = MarkedFlagProgressStore(loggedInStore()) // con sesión, pero sin inscripción
                val viewModel = ExerciseViewModel(
                    course, module.id, spy, loggedInStore(), requester,
                    FakeSubmissionGateway(), InMemorySessionStore(), FakeSubmissionPhotos(), this, Random(0),
                )
                runCurrent()
                pass(viewModel)
            }
        }
        assertEquals(0, requests)
    }

    // --- Ayudantes ---

    private suspend fun TestScope.openEnrolled(): ExerciseViewModel {
        val store = loggedInStore()
        store.enroll(course.id)
        spy = MarkedFlagProgressStore(store)
        val viewModel = ExerciseViewModel(
            course, module.id, spy, store, requester,
            FakeSubmissionGateway(), InMemorySessionStore(), FakeSubmissionPhotos(), this, Random(0),
        )
        runCurrent()
        return viewModel
    }

    // Empezar la prueba, acertar la pregunta y pulsar «Continuar» (aquí se aprueba).
    private fun TestScope.pass(viewModel: ExerciseViewModel) {
        viewModel.onStartTest()
        viewModel.onChoose(0)
        viewModel.onCheck()
        viewModel.onContinue()
        runCurrent()
    }

    // Un ProgressStore que recuerda si markMastered ya se ha llamado (para comprobar el orden).
    private class MarkedFlagProgressStore(private val delegate: InMemoryProgressStore) : ProgressStore {
        var marked = false
            private set

        override fun mastered(courseId: String): Flow<Set<String>> = delegate.mastered(courseId)

        override suspend fun markMastered(courseId: String, moduleId: String) {
            delegate.markMastered(courseId, moduleId)
            marked = true
        }
    }
}
