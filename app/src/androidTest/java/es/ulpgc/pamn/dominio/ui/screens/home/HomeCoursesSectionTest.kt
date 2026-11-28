package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// «Tus cursos» de Inicio (docs/tareas/inicio.md, paso 4b), con los cursos de ejemplo del Figma
// (HomePreviewData: Kotlin desde cero 7/20, Álgebra lineal 16/20, Redes de computadores 2/18).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeCoursesSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val kotlinRow = "Kotlin desde cero, Equipo PAMN, 7 de 20 conceptos dominados"

    private fun showInSpanish(onOpen: (String) -> Unit = {}) =
        composeRule.setContentInSpanish { GradusTheme { HomeCoursesSection(HomePreviewData.courses, onOpenCourse = onOpen) } }

    @Test
    fun the_section_title_is_a_heading() {
        showInSpanish()
        composeRule.onNodeWithText("Tus cursos").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    // Lo que se ve en cada fila: título, «autor · dominados de total» y el porcentaje (con espacio fijo).
    @Test
    fun each_row_shows_the_title_the_author_the_count_and_the_percent() {
        showInSpanish()
        composeRule.onNodeWithText("Kotlin desde cero", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Equipo PAMN · 7 de 20", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("35 %", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun there_is_one_button_per_course() {
        showInSpanish()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(3)
    }

    // TalkBack: un solo botón por curso, con la frase entera y la acción «Abrir curso».
    @Test
    fun talkback_reads_each_row_as_one_button() {
        showInSpanish()
        composeRule.onNodeWithContentDescription(kotlinRow)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher("su acción se llama «Abrir curso»") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Abrir curso" })
    }

    @Test
    fun the_rows_are_big_enough_to_tap() {
        showInSpanish()
        composeRule.onNodeWithContentDescription(kotlinRow).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun tapping_a_row_says_which_course() {
        var opened: String? = null
        showInSpanish(onOpen = { opened = it })

        composeRule.onNodeWithContentDescription(kotlinRow).performClick()

        assertEquals("kotlin", opened)
    }

    // Con un solo concepto en total, TalkBack dice «concepto» en singular.
    @Test
    fun talkback_uses_the_singular_with_one_concept() {
        val single = HomePreviewData.courses.first().copy(masteredCount = 0, total = 1, percent = 0)
        composeRule.setContentInSpanish { GradusTheme { HomeCoursesSection(listOf(single), onOpenCourse = {}) } }

        composeRule.onNodeWithContentDescription("Kotlin desde cero, Equipo PAMN, 0 de 1 concepto dominado").assertExists()
    }

    // En inglés cambian las palabras y el «%» va pegado; el título y el autor, no.
    @Test
    fun the_section_in_english() {
        composeRule.setContentInEnglish { GradusTheme { HomeCoursesSection(HomePreviewData.courses, onOpenCourse = {}) } }

        composeRule.onNodeWithText("Your courses").assertIsDisplayed()
        composeRule.onNodeWithText("Equipo PAMN · 7 of 20", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("35%", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Kotlin desde cero, Equipo PAMN, 7 of 20 concepts mastered").assertExists()
    }
}
