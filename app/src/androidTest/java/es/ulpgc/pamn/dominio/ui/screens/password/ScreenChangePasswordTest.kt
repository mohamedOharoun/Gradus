package es.ulpgc.pamn.dominio.ui.screens.password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.PasswordForm
import es.ulpgc.pamn.dominio.model.PasswordProblem
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// «Cambiar contraseña» (docs/tareas/ajustes.md, fase 2, tarea 7): qué enseña y qué se puede tocar.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenChangePasswordTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val filled = PasswordForm().withCurrent("Gradus2026!").withNext("Nueva2026?").withConfirmation("Nueva2026?")

    private fun show(form: PasswordForm = PasswordForm(), actions: ChangePasswordActions = ChangePasswordActions()) =
        composeRule.setContent { GradusTheme { ScreenChangePassword(form = form, actions = actions) } }

    // Los tres campos de contraseña, en orden.
    private fun field(index: Int) = composeRule.onAllNodes(hasSetTextAction())[index]

    private fun mainButton(text: String) = composeRule.onNode(hasClickAction() and hasText(text))

    @Test
    fun there_are_three_password_fields() {
        show()
        assertEquals(3, composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    // Con el formulario guardado como lo hace el ViewModel: cada campo avisa de lo suyo y lo enseña.
    @Test
    fun each_field_tells_what_is_typed_in_it() {
        var form by mutableStateOf(PasswordForm())
        composeRule.setContent {
            GradusTheme {
                ScreenChangePassword(
                    form = form,
                    actions = ChangePasswordActions(
                        onCurrentChange = { form = form.withCurrent(it) },
                        onNextChange = { form = form.withNext(it) },
                        onConfirmationChange = { form = form.withConfirmation(it) },
                    ),
                )
            }
        }

        field(0).performTextReplacement("a")
        field(1).performTextReplacement("b")
        field(2).performTextReplacement("c")

        assertEquals(Triple("a", "b", "c"), Triple(form.current, form.next, form.confirmation))
    }

    // Como en el registro: los requisitos y la coincidencia se ven mientras se escribe.
    @Test
    fun the_requirements_and_the_match_are_shown_live() {
        show(filled)
        composeRule.onNodeWithText("Las contraseñas coinciden", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saving_asks_to_save() {
        var saves = 0
        show(filled, ChangePasswordActions(onSave = { saves++ }))

        mainButton("Cambiar contraseña").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun while_saving_the_button_says_so_and_is_disabled() {
        show(filled.sending())
        mainButton("Cambiando…").assertIsNotEnabled()
    }

    @Test
    fun a_wrong_current_password_is_told() {
        show(filled.sending().rejected(PasswordProblem.WrongCurrent))
        composeRule.onNodeWithText("La contraseña actual no es correcta", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun empty_fields_say_they_are_required_after_trying() {
        show(PasswordForm().sending())
        assertEquals(3, composeRule.onAllNodesWithText("Este campo es obligatorio", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun without_connection_it_says_so_and_offers_to_retry() {
        show(filled.sending().failed(FailureReason.UNREACHABLE))
        composeRule.onNodeWithText("No hay conexión con el servidor", substring = true).assertIsDisplayed()
        mainButton("Reintentar").assertIsEnabled()
    }

    // Decisión G: se avisa de que las otras sesiones se han cerrado.
    @Test
    fun once_changed_it_says_so_and_offers_to_go_back() {
        var backs = 0
        show(filled.sending().succeeded(), ChangePasswordActions(onBack = { backs++ }))

        composeRule.onNode(isHeading() and hasText("Contraseña cambiada")).assertExists()
        composeRule.onNodeWithText("Hemos cerrado la sesión en tus otros dispositivos", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        mainButton("Volver a Ajustes").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun back_asks_to_go_back() {
        var backs = 0
        show(actions = ChangePasswordActions(onBack = { backs++ }))

        composeRule.onNode(hasClickAction() and hasContentDescription("Volver")).performClick()

        assertEquals(1, backs)
    }
}
