package es.ulpgc.pamn.dominio.ui.screens.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// La pantalla de la cámara (docs/tareas/notas-camara.md, tarea 5): qué enseña en cada momento y qué se
// toca. La vista previa es de mentira (un recuadro con etiqueta): aquí no se prueba CameraX.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenCameraTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        stage: CameraStage,
        captureFailed: Boolean = false,
        actions: CameraActions = CameraActions(),
        forSubmission: Boolean = false,
    ) =
        composeRule.setContent {
            GradusTheme {
                ScreenCamera(
                    title = "Nueva nota · BFS",
                    state = CameraUiState(stage, captureFailed, forSubmission),
                    actions = actions,
                    preview = { modifier -> Box(modifier.testTag("vista previa")) },
                    photoFile = { File("no-existe") },
                )
            }
        }

    private fun button(text: String) = composeRule.onNode(hasClickAction() and hasText(text))
    private fun shutter() = composeRule.onNode(hasClickAction() and hasContentDescription("Hacer foto"))

    // ── Permiso (decisión X) ──

    @Test
    fun without_permission_it_explains_why_and_asks() {
        var asks = 0
        show(CameraStage.NeedsPermission(denied = true), actions = CameraActions(onAllowCamera = { asks++ }))

        composeRule.onNodeWithText("Gradus necesita usar la cámara", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("«De la galería»", substring = true).assertIsDisplayed()
        button("Permitir la cámara").performClick()

        assertEquals(1, asks)
    }

    @Test
    fun denied_for_good_it_sends_to_the_settings() {
        var opens = 0
        show(CameraStage.NeedsPermission(denied = true, permanently = true), actions = CameraActions(onOpenSettings = { opens++ }))

        composeRule.onNodeWithText("activa el permiso de cámara en los ajustes", substring = true).assertIsDisplayed()
        button("Permitir la cámara").assertDoesNotExist()
        button("Abrir ajustes").performClick()

        assertEquals(1, opens)
    }

    // ── Disparar ──

    @Test
    fun ready_it_shows_the_preview_and_the_shutter() {
        var shots = 0
        show(CameraStage.Ready, actions = CameraActions(onShutter = { shots++ }))

        composeRule.onNodeWithTag("vista previa").assertExists()
        shutter().assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, shots)
    }

    @Test
    fun while_capturing_the_shutter_waits() {
        show(CameraStage.Capturing("captura.jpg"))
        shutter().assertIsNotEnabled()
    }

    @Test
    fun a_failed_photo_is_told() {
        show(CameraStage.Ready, captureFailed = true)
        composeRule.onNodeWithText("No se ha podido hacer la foto", substring = true).assertIsDisplayed()
        shutter().assertIsEnabled()
    }

    // ── Revisar ──

    @Test
    fun reviewing_there_is_no_preview_nor_shutter() {
        show(CameraStage.Reviewing("captura.jpg"))
        composeRule.onNodeWithTag("vista previa").assertDoesNotExist()
        shutter().assertDoesNotExist()
    }

    @Test
    fun the_caption_can_be_typed() {
        val typed = mutableListOf<String>()
        show(CameraStage.Reviewing("captura.jpg"), actions = CameraActions(onCaptionChange = { typed += it }))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Dijkstra")

        assertEquals(listOf("Dijkstra"), typed)
    }

    @Test
    fun a_too_long_caption_is_told() {
        show(CameraStage.Reviewing("captura.jpg", captionTooLong = true))
        composeRule.onNodeWithText("Como mucho 100 caracteres", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun retake_and_save_ask_for_it() {
        val asked = mutableListOf<String>()
        show(CameraStage.Reviewing("captura.jpg"), actions = CameraActions(onRetake = { asked += "repetir" }, onSave = { asked += "guardar" }))

        button("Repetir").performClick()
        button("Guardar").performClick()

        assertEquals(listOf("repetir", "guardar"), asked)
    }

    @Test
    fun while_saving_both_buttons_wait() {
        show(CameraStage.Reviewing("captura.jpg", saving = true))
        button("Guardando…").assertIsNotEnabled()
        button("Repetir").assertIsNotEnabled()
    }

    @Test
    fun a_failed_save_is_told() {
        show(CameraStage.Reviewing("captura.jpg", saveFailed = true))
        composeRule.onNodeWithText("No se ha podido guardar la foto", substring = true).assertIsDisplayed()
    }

    @Test
    fun close_asks_to_leave() {
        var leaves = 0
        show(CameraStage.Ready, actions = CameraActions(onClose = { leaves++ }))

        composeRule.onNode(hasClickAction() and hasContentDescription("Volver")).performClick()

        assertEquals(1, leaves)
    }

    // ── La foto de una entrega (docs/tareas/ejercicio-foto.md, decisión W) ──

    // Sin pie y con «Usar foto»: la foto va al paso, no a las notas.
    @Test
    fun the_photo_of_a_submission_has_no_caption_and_is_used() {
        var used = 0
        show(CameraStage.Reviewing("captura.jpg"), actions = CameraActions(onSave = { used++ }), forSubmission = true)

        composeRule.onNodeWithText("Pie de foto (opcional)").assertDoesNotExist()
        button("Guardar").assertDoesNotExist()
        button("Repetir").assertIsDisplayed()
        button("Usar foto").performClick()
        assertEquals(1, used)
    }

    // Al pedir el permiso dice para qué es: la foto solo sale del móvil al entregarla.
    @Test
    fun for_a_submission_the_permission_says_what_the_photo_is_for() {
        show(CameraStage.NeedsPermission(), forSubmission = true)
        composeRule.onNodeWithText("Para fotografiar tu ejercicio", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("tus apuntes", substring = true).assertDoesNotExist()
    }
}
