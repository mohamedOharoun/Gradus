package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

// La cabecera de Inicio (docs/tareas/inicio.md, paso 4a). Un día fijo: el test no depende del reloj.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val wednesday = LocalDate.of(2026, 10, 28)

    private fun isHeading() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun the_header_shows_the_date_and_greets_by_name() {
        composeRule.setContentInSpanish { GradusTheme { HomeHeader(wednesday, name = "Ana", initials = "AR") } }

        composeRule.onNodeWithText("Miércoles, 28 de octubre").assertIsDisplayed()
        composeRule.onNodeWithText("Hola, Ana").assertIsDisplayed()
    }

    // TalkBack salta al saludo como título de la pantalla.
    @Test
    fun the_greeting_is_the_heading() {
        composeRule.setContentInSpanish { GradusTheme { HomeHeader(wednesday, name = "Ana", initials = "AR") } }

        composeRule.onNodeWithText("Hola, Ana").assert(isHeading())
    }

    // Las iniciales se ven pero TalkBack no las lee («A R»): el nombre ya está en el saludo. TalkBack usa el
    // árbol fusionado; en el sin fusionar el texto sigue estando, porque se pinta.
    @Test
    fun talkback_does_not_read_the_initials() {
        composeRule.setContentInSpanish { GradusTheme { HomeHeader(wednesday, name = "Ana", initials = "AR") } }

        composeRule.onNodeWithText("AR").assertDoesNotExist()
    }

    @Test
    fun without_a_name_it_just_says_hello() {
        composeRule.setContentInSpanish { GradusTheme { HomeHeader(wednesday, name = null, initials = "?") } }

        composeRule.onNodeWithText("Hola").assertIsDisplayed()
    }

    // En inglés cambian el orden de la fecha y el saludo; el nombre, no.
    @Test
    fun the_header_in_english() {
        composeRule.setContentInEnglish { GradusTheme { HomeHeader(wednesday, name = "Ana", initials = "AR") } }

        composeRule.onNodeWithText("Wednesday, October 28").assertIsDisplayed()
        composeRule.onNodeWithText("Hello, Ana").assertIsDisplayed()
    }
}
