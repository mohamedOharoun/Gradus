package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// La ficha de un curso (ficha-curso.md, tarea 3): lo que se ve, lo que lee TalkBack y los botones.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenCourseDetailTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Escrito al revés que el camino: la lista debe salir en el orden del camino.
    private val course = Course(
        id = "g",
        title = "Introducción a los grafos",
        modules = listOf(
            Module("bfs", "Recorrido en anchura (BFS)", requires = listOf("repr")),
            Module("repr", "Representar un grafo"),
        ),
        description = "Cómo se guarda un grafo en memoria y cómo se recorre en anchura.",
        tags = listOf("grafos", "algoritmos"),
        publication = Publication(Author("u1", "Ana Ruiz"), version = 2, publishedAt = "2026-11-18T08:35:00Z"),
    )

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun show(state: CourseDetailState, actions: CourseDetailActions = CourseDetailActions()) =
        composeRule.setContent { GradusTheme { ScreenCourseDetail(state, actions) } }

    @Test
    fun the_course_data_is_shown_with_its_title_as_heading() {
        show(CourseDetailState.Ready(course))

        composeRule.onNodeWithText("Introducción a los grafos").assert(isHeading)
        composeRule.onNodeWithText("por Ana Ruiz").assertIsDisplayed()
        composeRule.onNodeWithText("grafos").assertIsDisplayed()
        composeRule.onNodeWithText("algoritmos").assertIsDisplayed()
        composeRule.onNodeWithText("Cómo se guarda un grafo en memoria y cómo se recorre en anchura.").assertIsDisplayed()
    }

    // «nov 2026» se ve; TalkBack oye el mes entero.
    @Test
    fun TalkBack_hears_the_modules_and_the_whole_month() {
        show(CourseDetailState.Ready(course))

        composeRule.onNodeWithContentDescription("2 módulos, publicado en noviembre de 2026").assertIsDisplayed()
    }

    @Test
    fun what_you_will_learn_is_a_heading_and_follows_the_path_order() {
        show(CourseDetailState.Ready(course))

        composeRule.onNodeWithText("Qué vas a aprender").assert(isHeading)
        composeRule.onNodeWithContentDescription("1, Representar un grafo").assertExists()
        composeRule.onNodeWithContentDescription("2, Recorrido en anchura (BFS)").assertExists()
    }

    @Test
    fun a_mastered_module_says_so_and_the_button_continues() {
        show(CourseDetailState.Ready(course, mastered = setOf("repr")))

        composeRule.onNodeWithContentDescription("1, Representar un grafo")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "dominado"))
        composeRule.onNodeWithText("1 de 2 dominados").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar el curso").assertIsDisplayed()
    }

    @Test
    fun the_start_button_is_big_enough_and_reports_the_tap() {
        var started = 0
        show(CourseDetailState.Ready(course), CourseDetailActions(onStart = { started++ }))

        composeRule.onNodeWithText("Empezar el curso").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, started)
    }

    @Test
    fun the_back_button_is_big_enough_and_reports_the_tap() {
        var back = 0
        show(CourseDetailState.Ready(course), CourseDetailActions(onBack = { back++ }))

        composeRule.onNodeWithContentDescription("Volver").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, back)
    }

    @Test
    fun a_course_without_publication_has_no_author_line() {
        show(CourseDetailState.Ready(course.copy(publication = null)))

        composeRule.onNodeWithText("por", substring = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("2 módulos").assertIsDisplayed()
    }

    @Test
    fun while_downloading_it_says_so() {
        show(CourseDetailState.Loading)

        composeRule.onNodeWithContentDescription("Descargando el curso").assertIsDisplayed()
    }

    @Test
    fun without_connection_retry_reports_the_tap() {
        var retries = 0
        show(CourseDetailState.Problem(CourseProblem.UNREACHABLE), CourseDetailActions(onRetry = { retries++ }))

        composeRule.onNodeWithText("Sin conexión").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }
}
