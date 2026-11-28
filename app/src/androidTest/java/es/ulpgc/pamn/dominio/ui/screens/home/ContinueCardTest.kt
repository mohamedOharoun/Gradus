package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.home.HomeCourse
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// «Continúa donde lo dejaste» (docs/tareas/inicio.md, paso 4c), con el curso de ejemplo del Figma
// (HomePreviewData: Estructuras de datos de Ana Ruiz, 12 de 20, siguiente «Módulo 13»).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ContinueCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val course: HomeCourse = checkNotNull(HomePreviewData.summary.continueWith)
    private val spoken = "Estructuras de datos, Ana Ruiz, 12 de 20 conceptos dominados, Siguiente: Módulo 13"

    private fun showInSpanish(onContinue: () -> Unit = {}) =
        composeRule.setContentInSpanish { GradusTheme { ContinueSection(course, onContinue) } }

    @Test
    fun the_section_title_is_a_heading() {
        showInSpanish()
        composeRule.onNodeWithText("Continúa donde lo dejaste").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun the_card_shows_the_course_the_progress_and_what_comes_next() {
        showInSpanish()
        composeRule.onNodeWithText("Estructuras de datos", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ana Ruiz", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("12 de 20 conceptos dominados", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Siguiente: Módulo 13", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Continuar", useUnmergedTree = true).assertIsDisplayed()
    }

    // Toda la tarjeta es un solo botón: no hay otro «Continuar» aparte que tocar.
    @Test
    fun the_whole_card_is_one_button() {
        showInSpanish()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun talkback_reads_the_card_with_the_continue_action() {
        showInSpanish()
        composeRule.onNodeWithContentDescription(spoken)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher("su acción se llama «Continuar»") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Continuar" })
    }

    @Test
    fun tapping_the_card_continues() {
        var continued = 0
        showInSpanish(onContinue = { continued++ })

        composeRule.onNodeWithContentDescription(spoken).performClick()

        assertEquals(1, continued)
    }

    // Un curso sin autor (de ejemplo): no se queda un hueco en lo que lee TalkBack.
    @Test
    fun without_an_author_talkback_skips_it() {
        val withoutAuthor = course.copy(course = course.course.copy(publication = null))
        composeRule.setContentInSpanish { GradusTheme { ContinueCard(withoutAuthor, onContinue = {}) } }

        composeRule.onNodeWithContentDescription("Estructuras de datos, 12 de 20 conceptos dominados, Siguiente: Módulo 13").assertExists()
    }

    @Test
    fun the_card_in_english() {
        composeRule.setContentInEnglish { GradusTheme { ContinueSection(course, onContinue = {}) } }

        composeRule.onNodeWithText("Pick up where you left off").assertIsDisplayed()
        composeRule.onNodeWithText("Next: Módulo 13", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Estructuras de datos, Ana Ruiz, 12 of 20 concepts mastered, Next: Módulo 13").assertExists()
    }
}
