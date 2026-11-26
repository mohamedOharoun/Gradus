package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.settings.AppLanguage
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Elegir el idioma de la app (docs/tareas/ajustes.md, fase 3, tarea 5b). Se ejecuta en el emulador:
// ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class LanguageDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val chosen = mutableListOf<AppLanguage>()
    private var dismissed = 0

    private fun show(selected: AppLanguage = AppLanguage.SYSTEM) = composeRule.setContentInSpanish {
        GradusTheme { LanguageDialog(selected, onChoose = { chosen += it }, onDismiss = { dismissed++ }) }
    }

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun it_offers_the_three_options_and_marks_the_current_one() {
        show(selected = AppLanguage.ENGLISH)

        composeRule.onNodeWithText("Idioma de la app").assertIsDisplayed()
        composeRule.onNodeWithText("El del sistema").assert(isRadio).assertIsNotSelected()
        composeRule.onNodeWithText("Español").assert(isRadio).assertIsNotSelected()
        composeRule.onNodeWithText("English").assert(isRadio).assertIsSelected()
    }

    // Tu aclaración (decisión R): lo dice el propio diálogo.
    @Test
    fun it_says_that_courses_keep_their_language() {
        show()
        composeRule.onNodeWithText("cada curso se ve en el idioma en que lo escribió su autor", substring = true).assertIsDisplayed()
    }

    @Test
    fun touching_an_option_chooses_it() {
        show()
        composeRule.onNodeWithText("English").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(listOf(AppLanguage.ENGLISH), chosen)
    }

    @Test
    fun cancel_closes_without_choosing() {
        show()
        composeRule.onNodeWithText("Cancelar").performClick()
        assertEquals(1, dismissed)
        assertEquals(emptyList<AppLanguage>(), chosen)
    }

    // En inglés: el diálogo traducido, y los nombres de los idiomas, escritos en su propio idioma.
    @Test
    fun in_english_it_is_translated_but_each_language_keeps_its_own_name() {
        composeRule.setContentInEnglish {
            GradusTheme { LanguageDialog(AppLanguage.SYSTEM, onChoose = {}, onDismiss = {}) }
        }
        composeRule.onNodeWithText("App language").assertIsDisplayed()
        composeRule.onNodeWithText("System default").assertIsDisplayed()
        composeRule.onNode(hasText("Español")).assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
