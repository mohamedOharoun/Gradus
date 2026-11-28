package es.ulpgc.pamn.dominio.ui.screens.profile

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.ProfileForm
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// «Editar perfil» (docs/tareas/ajustes.md, fase 2, tarea 6): qué enseña y qué se puede tocar.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenEditProfileTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val start = ProfileForm(original = "Ana")

    private fun show(form: ProfileForm = start, actions: EditProfileActions = EditProfileActions()) =
        composeRule.setContentInSpanish { GradusTheme { ScreenEditProfile(email = "ana@ejemplo.com", form = form, actions = actions) } }

    private fun saveButton() = composeRule.onNode(hasClickAction() and (hasText("Guardar") or hasText("Guardando…") or hasText("Reintentar")))

    @Test
    fun the_name_field_starts_with_the_name_of_the_account() {
        show()
        composeRule.onNode(hasSetTextAction()).assert(hasText("Ana"))
    }

    @Test
    fun typing_tells_the_new_name() {
        val typed = mutableListOf<String>()
        show(actions = EditProfileActions(onNameChange = { typed += it }))

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Ana Ruiz")

        assertEquals(listOf("Ana Ruiz"), typed)
    }

    // Decisión F: el correo se ve, pero no se puede editar (no hay un campo para él).
    @Test
    fun the_email_is_shown_but_cannot_be_edited() {
        show()
        composeRule.onNodeWithText("ana@ejemplo.com", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("El correo no se puede cambiar.", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().let { assertEquals(1, it.size) }
    }

    @Test
    fun without_changes_save_is_disabled() {
        show()
        saveButton().assertIsNotEnabled()
    }

    @Test
    fun with_a_change_save_asks_to_save() {
        var saves = 0
        show(start.withName("Ana Ruiz"), EditProfileActions(onSave = { saves++ }))

        saveButton().assertIsEnabled().performClick()

        assertEquals(1, saves)
    }

    @Test
    fun while_saving_the_button_says_so_and_is_disabled() {
        show(start.withName("Ana Ruiz").sending())
        saveButton().assert(hasText("Guardando…")).assertIsNotEnabled()
    }

    @Test
    fun the_error_of_the_name_shows_under_it() {
        show(start.withName("A").sending())
        composeRule.onNodeWithText("Escribe al menos 2 caracteres", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun without_connection_it_says_so_and_offers_to_retry() {
        show(start.withName("Ana Ruiz").sending().failed(FailureReason.UNREACHABLE))
        composeRule.onNodeWithText("No hay conexión con el servidor", substring = true).assertIsDisplayed()
        saveButton().assert(hasText("Reintentar")).assertIsEnabled()
    }

    @Test
    fun back_asks_to_go_back() {
        var backs = 0
        show(actions = EditProfileActions(onBack = { backs++ }))

        composeRule.onNode(hasClickAction() and hasContentDescription("Volver")).performClick()

        assertEquals(1, backs)
    }
}
