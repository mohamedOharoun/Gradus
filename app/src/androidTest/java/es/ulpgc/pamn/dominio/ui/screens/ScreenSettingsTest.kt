package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsActions
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsScreenState
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import es.ulpgc.pamn.dominio.settings.AppLanguage
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// La pantalla de Ajustes (ajustes.md, tarea 5): qué enseña, qué se puede tocar y qué no (decisión A).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = SettingsScreenState(name = "Ana Ruiz", email = "ana@ejemplo.com", darkMode = false)

    private fun show(
        state: SettingsScreenState = this.state,
        onDarkModeChange: (Boolean) -> Unit = {},
        onLogOut: () -> Unit = {},
        onEditProfile: () -> Unit = {},
        onChangePassword: () -> Unit = {},
        onDeleteAccount: () -> Unit = {},
        onEditTags: () -> Unit = {},
        onChooseLanguage: () -> Unit = {},
    ) = composeRule.setContentInSpanish {
        GradusTheme {
            ScreenSettings(
                state,
                SettingsActions(onDarkModeChange, onLogOut, onEditProfile, onChangePassword, onDeleteAccount, onEditTags, onChooseLanguage),
            )
        }
    }

    // La fila (el elemento pulsable, fusionado) que contiene ese texto.
    private fun row(text: String) = composeRule.onNode(hasClickAction() and hasText(text))

    private val switch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    @Test
    fun the_title_and_the_four_sections_are_headings() {
        show()

        val headings = listOf("Ajustes", "CUENTA", "APARIENCIA E IDIOMA", "TEMAS QUE TE INTERESAN", "SESIÓN")
        composeRule.onAllNodes(isHeading()).assertCountEquals(headings.size)
        headings.forEach { composeRule.onNode(isHeading() and hasText(it)).assertExists() }
    }

    @Test
    fun the_profile_shows_the_name_and_email_of_the_account() {
        show()

        composeRule.onNodeWithText("Ana Ruiz", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("ana@ejemplo.com", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("AR", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun the_switch_shows_light_mode() {
        show(state.copy(darkMode = false))

        composeRule.onNode(switch).assertIsOff()
    }

    @Test
    fun the_switch_shows_dark_mode() {
        show(state.copy(darkMode = true))

        composeRule.onNode(switch).assertIsOn()
    }

    @Test
    fun touching_the_switch_asks_for_the_opposite_mode() {
        val asked = mutableListOf<Boolean>()
        show(state.copy(darkMode = true), onDarkModeChange = { asked += it })

        composeRule.onNode(switch).performClick()

        assertEquals(listOf(false), asked)
    }

    // Cerrar sesión está abajo: hay que desplazarse para llegar.
    @Test
    fun touching_log_out_asks_to_log_out() {
        var logOuts = 0
        show(onLogOut = { logOuts++ })

        row("Cerrar sesión").performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, logOuts)
    }

    // Fase 3b: la fila dice el idioma de ahora y al tocarla se elige otro.
    @Test
    fun the_language_says_which_one_and_opens_the_choice() {
        var opens = 0
        show(state = state.copy(language = AppLanguage.ENGLISH), onChooseLanguage = { opens++ })

        row("Idioma").performScrollTo().assert(hasText("English")).assertIsEnabled().performClick()
        assertEquals(1, opens)
    }

    // Ya se puede tocar todo: perfil, contraseña, modo oscuro, idioma, etiquetas, cerrar sesión y eliminar la cuenta.
    @Test
    fun everything_can_be_touched() {
        show()

        composeRule.onAllNodes(hasClickAction() and SemanticsMatcher("activado") {
            !it.config.contains(SemanticsProperties.Disabled)
        }).assertCountEquals(7)
    }

    @Test
    fun touching_the_preferred_tags_opens_them() {
        var opens = 0
        show(onEditTags = { opens++ })

        row("Etiquetas preferidas").performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, opens)
    }

    @Test
    fun the_chosen_tags_are_shown_with_a_hash() {
        show(state.copy(preferredTags = listOf("grafos", "bases de datos")))

        composeRule.onNodeWithText("#grafos", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("#bases de datos", useUnmergedTree = true).assertExists()
    }

    @Test
    fun without_tags_it_says_none_was_chosen() {
        show()
        composeRule.onNodeWithText("Aún no has elegido ninguna.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun touching_delete_account_opens_its_dialog() {
        var opens = 0
        show(onDeleteAccount = { opens++ })

        row("Eliminar cuenta").performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, opens)
    }

    @Test
    fun touching_password_opens_change_password() {
        var opens = 0
        show(onChangePassword = { opens++ })

        row("Contraseña").assert(hasText("Cambiar")).assertIsEnabled().performClick()

        assertEquals(1, opens)
    }

    // Fase 2: el perfil se puede editar.
    @Test
    fun touching_the_profile_opens_edit_profile() {
        var edits = 0
        show(onEditProfile = { edits++ })

        row("Ana Ruiz").assert(hasText("Editar")).assertIsEnabled().performClick()

        assertEquals(1, edits)
    }

    @Test
    fun the_preferred_tags_explain_what_they_are_for() {
        show()

        composeRule.onNodeWithText("Pronto verás antes los cursos con estas etiquetas en Inicio y en Buscar.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // Eliminar cuenta es lo último de la pantalla: se llega desplazándose (pantallas pequeñas).
    @Test
    fun the_screen_scrolls_down_to_delete_account() {
        show()

        composeRule.onNodeWithText("Eliminar cuenta", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    // Decisión V: en Android 10–12 no se puede elegir; la fila no dice «Próximamente», sino que sigue al sistema.
    @Test
    fun where_the_language_cannot_be_chosen_it_follows_the_system() {
        show(state = state.copy(canChooseLanguage = false))

        composeRule.onNode(hasText("Idioma") and hasText("El del sistema")).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Próximamente").assertDoesNotExist()
    }

    // Decisión S: con la app en inglés, lo que ya está traducido sale en inglés.
    @Test
    fun in_english_the_language_row_is_translated() {
        composeRule.setContentInEnglish {
            GradusTheme { ScreenSettings(state.copy(language = AppLanguage.SPANISH), SettingsActions({}, {})) }
        }

        composeRule.onNode(hasClickAction() and hasText("Language") and hasText("Español")).performScrollTo().assertIsDisplayed()
    }
}
