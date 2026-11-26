package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.data.demoSearchResults
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import es.ulpgc.pamn.dominio.ui.testing.inSpanish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Lo que TalkBack lee de la tarjeta de un curso y cómo se toca: es un solo elemento pulsable con la
// frase entera, las etiquetas pasan de 3 a «+N» y mide al menos 48 dp. Los textos en sí
// (CourseTexts.kt) se prueban en el ordenador (CourseTextsTest). Se ejecuta en el emulador:
// ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class CourseCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val course = demoSearchResults.first()

    private fun show(course: CourseSummary, onClick: () -> Unit = {}) =
        composeRule.setContentInSpanish { GradusTheme { CourseCard(course, onClick) } }

    // La tarjeta es la única cosa pulsable. Se busca en el árbol «fusionado», el que ve TalkBack.
    private fun card() = composeRule.onNode(hasClickAction())

    // Los textos sueltos no están en el árbol fusionado (clearAndSetSemantics los esconde a TalkBack),
    // pero sí se pintan: se buscan en el árbol sin fusionar.
    private fun visibleText(text: String) = composeRule.onNodeWithText(text, useUnmergedTree = true)

    @Test
    fun the_card_is_a_single_clickable_element() {
        show(course)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun talkback_reads_the_whole_sentence_with_the_full_month() {
        show(course)

        card().assert(hasContentDescription(course.spokenText().inSpanish()))
        card().assert(hasContentDescription("publicado en octubre de 2026", substring = true))
    }

    @Test
    fun talkback_does_not_mention_the_date_when_it_is_not_understood() {
        show(course.copy(publishedAt = "pronto"))

        card().assert(hasContentDescription("publicado", substring = true).not())
    }

    @Test
    fun the_card_is_a_button_with_the_action_open_course() {
        show(course)

        card()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher("la acción de pulsar se llama «Abrir curso»") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Abrir curso"
                },
            )
    }

    @Test
    fun tapping_the_card_reports_the_click() {
        var clicks = 0
        show(course, onClick = { clicks++ })

        card().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun tapping_a_text_inside_the_card_also_reports_the_click() {
        var clicks = 0
        show(course, onClick = { clicks++ })

        visibleText("Estructuras de datos").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun the_card_is_at_least_48dp_tall() {
        show(course)

        card().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun with_five_tags_three_are_shown_and_plus_two() {
        show(course.copy(tags = listOf("grafos", "árboles", "algoritmos", "kotlin", "android")))

        visibleText("grafos").assertExists()
        visibleText("árboles").assertExists()
        visibleText("algoritmos").assertExists()
        visibleText("kotlin").assertDoesNotExist()
        visibleText("android").assertDoesNotExist()
        visibleText("+2").assertExists()
    }

    @Test
    fun with_three_tags_there_is_no_plus_label() {
        show(course.copy(tags = listOf("grafos", "árboles", "algoritmos")))

        composeRule.onNode(hasText("+", substring = true), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun with_no_tags_the_card_still_shows_title_author_and_meta() {
        show(course.copy(tags = emptyList()))

        visibleText("Estructuras de datos").assertExists()
        visibleText("por Ana Ruiz").assertExists()
        visibleText("20 módulos · oct 2026").assertExists()
    }

    @Test
    fun the_meta_line_has_only_the_modules_when_the_date_is_not_understood() {
        show(course.copy(publishedAt = "pronto"))

        visibleText("20 módulos").assertExists()
    }

    @Test
    fun the_card_shows_title_author_and_meta_but_not_the_description() {
        show(course)

        visibleText("Estructuras de datos").assertExists()
        visibleText("por Ana Ruiz").assertExists()
        visibleText("20 módulos · oct 2026").assertExists()
        visibleText(course.description).assertDoesNotExist()
    }
}
