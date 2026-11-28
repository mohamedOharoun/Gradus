package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.home.HomeSummary
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

// La pantalla Inicio entera (docs/tareas/inicio.md, paso 4d): cargando, con cursos, sin cursos y con cursos
// que no se han podido cargar. Datos del Figma (HomePreviewData). Se ejecuta en el emulador.
@RunWith(AndroidJUnit4::class)
class ScreenHomeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val day = LocalDate.of(2026, 10, 28)
    private val empty = HomeSummary(continueWith = null, courses = emptyList())

    private fun ready(summary: HomeSummary = HomePreviewData.summary, unavailable: Int = 0) =
        HomeScreenState.Ready(HomeUiState("Ana", "AR", summary, unavailable))

    private fun showInSpanish(state: HomeScreenState, actions: HomeActions = HomeActions()) =
        composeRule.setContentInSpanish { GradusTheme { ScreenHome(state, day, actions) } }

    // La lista es más larga que la pantalla: se desplaza hasta lo que se busca.
    private fun scrollTo(matcher: androidx.compose.ui.test.SemanticsMatcher) =
        composeRule.onNode(hasScrollAction()).performScrollToNode(matcher)

    @Test
    fun while_loading_talkback_says_what_is_loading() {
        showInSpanish(HomeScreenState.Loading)
        composeRule.onNodeWithContentDescription("Cargando tus cursos").assertIsDisplayed()
    }

    // Con cursos: saludo, «Continúa» y «Tus cursos»; ni el aviso sin cursos ni el de cursos sin cargar.
    @Test
    fun with_courses_it_shows_the_greeting_the_continue_card_and_the_list() {
        showInSpanish(ready())

        composeRule.onNodeWithText("Hola, Ana").assertIsDisplayed()
        composeRule.onNodeWithText("Continúa donde lo dejaste").assertIsDisplayed()
        scrollTo(hasText("Tus cursos"))
        composeRule.onNodeWithText("Tus cursos").assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no estás en ningún curso").assertDoesNotExist()
    }

    @Test
    fun continue_opens_its_course() {
        var opened: String? = null
        showInSpanish(ready(), HomeActions(onOpenCourse = { opened = it }))

        // La tarjeta de «Continúa» es la única que dice «Siguiente: …».
        composeRule.onNodeWithContentDescription("Siguiente: Módulo 13", substring = true).performClick()

        assertEquals("estructuras", opened)
    }

    @Test
    fun a_row_of_your_courses_opens_its_course() {
        var opened: String? = null
        showInSpanish(ready(), HomeActions(onOpenCourse = { opened = it }))

        scrollTo(hasContentDescription("Redes de computadores", substring = true))
        composeRule.onNodeWithContentDescription("Redes de computadores", substring = true).performClick()

        assertEquals("redes", opened)
    }

    // Sin cursos: el aviso y «Buscar cursos», que lleva a Buscar. Sin «Continúa» ni «Tus cursos».
    @Test
    fun without_courses_it_invites_to_search() {
        var searched = 0
        showInSpanish(ready(empty), HomeActions(onSearch = { searched++ }))

        composeRule.onNodeWithText("Todavía no estás en ningún curso").assertIsDisplayed()
        composeRule.onNodeWithText("Continúa donde lo dejaste").assertDoesNotExist()
        composeRule.onNodeWithText("Tus cursos").assertDoesNotExist()
        composeRule.onNodeWithText("Buscar cursos").performClick()

        assertEquals(1, searched)
    }

    // Inscrito pero sin poder cargar nada (sin red): no se invita a buscar, se avisa de lo que pasa.
    @Test
    fun courses_that_could_not_be_loaded_are_announced() {
        showInSpanish(ready(empty, unavailable = 2))

        composeRule.onNodeWithText("No se han podido cargar 2 cursos. Comprueba la conexión.").assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no estás en ningún curso").assertDoesNotExist()
    }

    // Con todos los cursos terminados no hay nada que «continuar», pero sí la lista.
    @Test
    fun with_every_course_finished_there_is_no_continue_card() {
        showInSpanish(ready(HomePreviewData.summary.copy(continueWith = null)))

        composeRule.onNodeWithText("Continúa donde lo dejaste").assertDoesNotExist()
        composeRule.onNodeWithText("Tus cursos").assertIsDisplayed()
    }

    @Test
    fun the_home_in_english() {
        composeRule.setContentInEnglish { GradusTheme { ScreenHome(ready(empty), day, HomeActions()) } }

        composeRule.onNodeWithText("Wednesday, October 28").assertIsDisplayed()
        composeRule.onNodeWithText("Hello, Ana").assertIsDisplayed()
        composeRule.onNodeWithText("You're not in any course yet").assertIsDisplayed()
        composeRule.onNodeWithText("Search courses").assertIsDisplayed()
    }

    // Decisión J (como el Figma): el curso de «Continúa» no se repite en «Tus cursos».
    @Test
    fun the_course_to_continue_is_not_repeated_in_your_courses() {
        showInSpanish(ready())

        composeRule.onAllNodesWithContentDescription("Estructuras de datos", substring = true).assertCountEquals(1)
    }

    // Con un solo curso, sale en «Continúa» y «Tus cursos» no aparece.
    @Test
    fun with_a_single_course_there_is_no_your_courses_section() {
        val single = HomePreviewData.summary.let { it.copy(courses = listOfNotNull(it.continueWith)) }
        showInSpanish(ready(single))

        composeRule.onNodeWithText("Continúa donde lo dejaste").assertIsDisplayed()
        composeRule.onNodeWithText("Tus cursos").assertDoesNotExist()
    }
}

