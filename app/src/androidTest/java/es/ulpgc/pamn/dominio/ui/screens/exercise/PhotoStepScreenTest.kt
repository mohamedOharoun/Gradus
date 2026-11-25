package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.study.ModuleStudy
import es.ulpgc.pamn.dominio.course.study.PhotoStep
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

// El paso con foto en pantalla (docs/tareas/ejercicio-foto.md, paso 5c): qué se ve y qué botón avisa de
// qué en cada estado, y lo que TalkBack necesita. Se ejecuta en el emulador:
// ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class PhotoStepScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val course = Course(
        "c", "Curso",
        listOf(Module("bfs", "Recorrido en anchura", steps = listOf(Step.Photo("foto", "Recorre el grafo desde **A**.", "Orden: A, B, C.")))),
    )
    private val pressed = mutableListOf<String>()
    private val actions = ExerciseActions(
        onTakePhoto = { pressed += "cámara" },
        onPickPhoto = { pressed += "galería" },
        onDiscardPhoto = { pressed += "quitar" },
        onSubmitPhoto = { pressed += "entregar" },
        onSubmitAgain = { pressed += "otra vez" },
        onSubmitPhotoLater = { pressed += "más tarde" },
        onContinue = { pressed += "continuar" },
    )

    private fun show(
        change: (PhotoStep) -> PhotoStep = { it },
        ui: PhotoStepUi = PhotoStepUi(),
        withActions: ExerciseActions = actions,
    ) {
        val study = ModuleStudy.start(course, "bfs", Random(1))!!.updatePhoto("foto", change)
        composeRule.setContent { GradusTheme { ScreenExercise(study, withActions, photo = ui) } }
    }

    private fun PhotoStep.sending() = take("no-existe.jpg").send()
    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
    private val hasLiveRegion = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

    // ── Sin foto ──

    @Test
    fun without_a_photo_the_prompt_is_a_heading_and_the_photo_can_be_taken_or_picked() {
        show()
        composeRule.onNodeWithText("Recorre el grafo desde A.").assert(isHeading)
        composeRule.onNodeWithText("Hacer foto").performClick()
        composeRule.onNodeWithText("De la galería").performClick()
        composeRule.onNodeWithText("Entregar").assertIsNotEnabled()
        assertEquals(listOf("cámara", "galería"), pressed)
    }

    // Decisión S: no se queda bloqueado.
    @Test
    fun it_can_be_left_for_later() {
        show()
        composeRule.onNodeWithText("Entregar más tarde").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(listOf("más tarde"), pressed)
    }

    // Sin cámara en el móvil, solo queda la galería.
    @Test
    fun without_a_camera_only_the_gallery_is_offered() {
        show(withActions = actions.copy(onTakePhoto = null))
        composeRule.onNodeWithText("Hacer foto").assertIsNotEnabled()
        composeRule.onNodeWithText("De la galería").assertIsEnabled()
    }

    @Test
    fun while_preparing_the_photo_it_says_so_and_the_buttons_wait() {
        show(ui = PhotoStepUi(notice = PhotoNotice.PREPARING))
        composeRule.onNodeWithText("Preparando la foto…").assert(hasLiveRegion)
        composeRule.onNodeWithText("De la galería").assertIsNotEnabled()
    }

    @Test
    fun a_photo_that_cannot_be_read_is_reported() {
        show(ui = PhotoStepUi(notice = PhotoNotice.UNREADABLE))
        composeRule.onNodeWithText("No se ha podido leer la foto. Prueba con otra.").assert(hasLiveRegion)
    }

    // ── Con foto ──

    @Test
    fun with_a_photo_it_is_shown_and_can_be_submitted_retaken_or_removed() {
        show({ it.take("no-existe.jpg") })
        composeRule.onNodeWithContentDescription("Tu foto").assertIsDisplayed()
        composeRule.onNodeWithText("Hacer otra").assertIsDisplayed()
        composeRule.onNodeWithText("Quitar la foto").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Entregar").assertIsEnabled().performClick()
        assertEquals(listOf("quitar", "entregar"), pressed)
    }

    @Test
    fun while_sending_nothing_else_can_be_done() {
        show({ it.sending() })
        composeRule.onNodeWithText("Entregando…").assertIsNotEnabled()
        composeRule.onNodeWithText("Entregar más tarde").assertDoesNotExist()
        composeRule.onNodeWithText("Hacer otra").assertDoesNotExist()
        composeRule.onNodeWithText("Quitar la foto").assertDoesNotExist()
    }

    // ── Fallos ──

    // Decisión J: aviso y «Reintentar».
    @Test
    fun without_connection_it_is_reported_and_can_be_retried() {
        show({ it.sending().finished(SubmitOutcome.Unreachable) })
        composeRule.onNodeWithText("Sin conexión. Comprueba la red y vuelve a intentarlo.").assert(hasLiveRegion)
        composeRule.onNodeWithText("Reintentar").performClick()
        assertEquals(listOf("entregar"), pressed)
    }

    @Test
    fun a_rejected_photo_asks_for_another_one() {
        show({ it.sending().finished(SubmitOutcome.PhotoRejected(PhotoRejection.NOT_JPEG)) })
        composeRule.onNodeWithText("Esa imagen no se puede entregar. Prueba con otra foto.").assertIsDisplayed()
        composeRule.onNodeWithText("Entregar").assertIsNotEnabled()
        composeRule.onNodeWithText("Hacer otra").assertIsEnabled()
    }

    // Decisión Y.
    @Test
    fun an_expired_session_asks_to_log_in_again() {
        show({ it.sending().finished(SubmitOutcome.SessionExpired) })
        composeRule.onNodeWithText("Tu sesión ha caducado. Vuelve a iniciar sesión para entregar.").assertIsDisplayed()
        composeRule.onNodeWithText("Entregar más tarde").assertIsDisplayed()
    }

    // ── Entregada ──

    // Decisión C: se ve la solución y se sigue; TalkBack lo lee al aparecer.
    @Test
    fun once_submitted_the_solution_is_shown_and_the_student_goes_on() {
        show({ it.sending().finished(SubmitOutcome.Submitted(Submission("s1", "2026-11-25T10:00:00Z"))) })
        composeRule.onNode(hasText("Entregada · pendiente de revisión", substring = true).and(hasLiveRegion)).assertIsDisplayed()
        composeRule.onNodeWithText("Orden: A, B, C.").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Entregar otra vez").performClick()
        assertEquals(listOf("continuar", "otra vez"), pressed)
    }

    @Test
    fun the_solution_is_not_shown_before_submitting() {
        show({ it.take("no-existe.jpg") })
        composeRule.onNodeWithText("Orden: A, B, C.").assertDoesNotExist()
    }

    // Decisión I: tras «Entregar otra vez», la anterior sigue contando.
    @Test
    fun after_submitting_again_the_student_can_go_on_without_another() {
        show({ it.sending().finished(SubmitOutcome.Submitted(Submission("s1", "2026-11-25T10:00:00Z"))).again() })
        composeRule.onNodeWithText("Continuar sin entregar otra").performClick()
        assertEquals(listOf("continuar"), pressed)
    }

    // ── Curso sin publicar (decisión Z) ──

    @Test
    fun in_a_course_that_is_not_published_it_explains_why_and_only_offers_later() {
        show(ui = PhotoStepUi(canSubmit = false))
        composeRule.onNodeWithText("no está publicado", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Hacer foto").assertDoesNotExist()
        composeRule.onNodeWithText("Entregar").assertIsNotEnabled()
        composeRule.onNodeWithText("Entregar más tarde").assertIsEnabled()
    }
}
