package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.model.DeleteAccountForm
import es.ulpgc.pamn.dominio.model.DeleteProblem
import es.ulpgc.pamn.dominio.model.PasswordForm
import es.ulpgc.pamn.dominio.model.ProfileForm
import es.ulpgc.pamn.dominio.settings.TagAddition
import es.ulpgc.pamn.dominio.ui.screens.ScreenSettings
import es.ulpgc.pamn.dominio.ui.screens.password.ChangePasswordActions
import es.ulpgc.pamn.dominio.ui.screens.password.ScreenChangePassword
import es.ulpgc.pamn.dominio.ui.screens.profile.EditProfileActions
import es.ulpgc.pamn.dominio.ui.screens.profile.ScreenEditProfile
import es.ulpgc.pamn.dominio.ui.screens.tags.PreferredTagsActions
import es.ulpgc.pamn.dominio.ui.screens.tags.PreferredTagsUiState
import es.ulpgc.pamn.dominio.ui.screens.tags.ScreenPreferredTags
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Ajustes y sus pantallas en inglés (docs/tareas/ajustes.md, fase 3, paso 7). En español ya lo comprueban
// sus propios tests (con setContentInSpanish). Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsInEnglishTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun row(text: String) = composeRule.onNode(hasClickAction() and hasText(text))

    @Test
    fun settings_in_english() {
        val state = SettingsScreenState(name = "Ana Ruiz", email = "ana@ejemplo.com", darkMode = false)
        composeRule.setContentInEnglish { GradusTheme { ScreenSettings(state, SettingsActions({}, {})) } }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        // El título de la sección se pinta en mayúsculas («Account» también está en «Delete account»).
        composeRule.onNodeWithText("ACCOUNT").assertIsDisplayed()
        row("Password").assert(hasText("Change"))
        composeRule.onNodeWithText("Dark mode").performScrollTo().assertIsDisplayed()
        row("Preferred tags").performScrollTo().assert(hasText("Edit"))
        composeRule.onNodeWithText("You haven't chosen any yet.").performScrollTo().assertIsDisplayed()
        row("Log out").performScrollTo().assertIsDisplayed()
        row("Delete account").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun edit_profile_in_english() {
        composeRule.setContentInEnglish {
            GradusTheme { ScreenEditProfile(email = "ana@ejemplo.com", form = ProfileForm("Ana"), actions = EditProfileActions()) }
        }
        composeRule.onNodeWithText("Edit profile").assertIsDisplayed()
        composeRule.onNodeWithText("The email can't be changed.").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun change_password_in_english() {
        composeRule.setContentInEnglish { GradusTheme { ScreenChangePassword(PasswordForm(), ChangePasswordActions()) } }
        composeRule.onNodeWithText("Current password").assertIsDisplayed()
        composeRule.onNodeWithText("New password").assertIsDisplayed()
        composeRule.onNodeWithText("Repeat the new password").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun delete_account_in_english() {
        composeRule.setContentInEnglish { GradusTheme { DeleteAccountDialog(DeleteAccountForm(), DeleteAccountActions()) } }
        composeRule.onNodeWithText("Delete your account?").assertIsDisplayed()
        composeRule.onNodeWithText("Enter your password to confirm.").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun author_of_courses_in_english() {
        val author = DeleteAccountForm(problem = DeleteProblem.AUTHOR_OF_COURSES)
        composeRule.setContentInEnglish { GradusTheme { DeleteAccountDialog(author, DeleteAccountActions()) } }
        composeRule.onNodeWithText("It can't be deleted").assertIsDisplayed()
        composeRule.onNodeWithText("Got it").assertIsDisplayed()
    }

    @Test
    fun preferred_tags_in_english() {
        val state = PreferredTagsUiState(tags = listOf("grafos"), problem = TagAddition.Empty)
        composeRule.setContentInEnglish { GradusTheme { ScreenPreferredTags(state, PreferredTagsActions()) } }
        composeRule.onNodeWithText("Preferred tags").assertIsDisplayed()
        composeRule.onNodeWithText("Write a tag").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 10").assertIsDisplayed()
        // La etiqueta no se traduce (la escribe el alumno); el ✕ que la quita, sí.
        composeRule.onNodeWithContentDescription("Remove the tag grafos").assertExists()
    }
}
