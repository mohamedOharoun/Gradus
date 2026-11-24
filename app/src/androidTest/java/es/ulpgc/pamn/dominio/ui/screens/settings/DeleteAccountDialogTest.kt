package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.model.DeleteAccountForm
import es.ulpgc.pamn.dominio.model.DeleteProblem
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// El diálogo «Eliminar cuenta» (docs/tareas/ajustes.md, fase 2, tarea 8, decisiones H e I).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class DeleteAccountDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val typed = DeleteAccountForm().withPassword("Gradus2026!")

    private fun show(form: DeleteAccountForm = DeleteAccountForm(), actions: DeleteAccountActions = DeleteAccountActions()) =
        composeRule.setContent { GradusTheme { DeleteAccountDialog(form, actions) } }

    private fun button(text: String) = composeRule.onNode(hasClickAction() and hasText(text))

    // Decisión H: avisa de que no se puede deshacer y pide la contraseña.
    @Test
    fun it_warns_that_it_cannot_be_undone_and_asks_for_the_password() {
        show()
        composeRule.onNodeWithText("¿Eliminar tu cuenta?").assertIsDisplayed()
        composeRule.onNodeWithText("No se puede deshacer", substring = true).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertExists()
    }

    @Test
    fun typing_the_password_tells_it() {
        var form by mutableStateOf(DeleteAccountForm())
        composeRule.setContent {
            GradusTheme { DeleteAccountDialog(form, DeleteAccountActions(onPasswordChange = { form = form.withPassword(it) })) }
        }

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Gradus2026!")

        assertEquals("Gradus2026!", form.password)
    }

    @Test
    fun confirming_asks_to_delete() {
        var confirms = 0
        show(typed, DeleteAccountActions(onConfirm = { confirms++ }))

        button("Eliminar cuenta").performClick()

        assertEquals(1, confirms)
    }

    @Test
    fun cancelling_asks_to_close() {
        var dismisses = 0
        show(actions = DeleteAccountActions(onDismiss = { dismisses++ }))

        button("Cancelar").performClick()

        assertEquals(1, dismisses)
    }

    // Mientras se borra no se puede ni repetir ni cancelar.
    @Test
    fun while_deleting_both_buttons_are_disabled() {
        show(typed.sending())
        button("Eliminando…").assertIsNotEnabled()
        button("Cancelar").assertIsNotEnabled()
    }

    @Test
    fun a_wrong_password_is_told() {
        show(typed.sending().rejected(DeleteProblem.WRONG_PASSWORD))
        composeRule.onNodeWithText("La contraseña no es correcta", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun an_empty_password_is_required_after_trying() {
        show(DeleteAccountForm().sending())
        composeRule.onNodeWithText("Este campo es obligatorio", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun without_connection_it_says_so_and_offers_to_retry() {
        show(typed.sending().failed(FailureReason.UNREACHABLE))
        composeRule.onNodeWithText("No hay conexión con el servidor", substring = true).assertIsDisplayed()
        button("Reintentar").assertIsEnabled()
    }

    // Decisión I: explica por qué y solo queda «Entendido» (sin contraseña ni «Eliminar»).
    @Test
    fun the_author_of_courses_is_told_why_and_can_only_close() {
        var dismisses = 0
        show(typed.sending().rejected(DeleteProblem.AUTHOR_OF_COURSES), DeleteAccountActions(onDismiss = { dismisses++ }))

        composeRule.onNodeWithText("No se puede eliminar").assertIsDisplayed()
        composeRule.onNodeWithText("Has publicado cursos", substring = true).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
        button("Eliminar cuenta").assertDoesNotExist()
        button("Entendido").performClick()

        assertEquals(1, dismisses)
    }
}
