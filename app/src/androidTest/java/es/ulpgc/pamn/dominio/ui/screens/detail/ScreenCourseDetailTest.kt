package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// La ficha de un curso (docs/tareas/ficha-curso-ui.md, tarea C5): vistas no inscrito e inscrito,
// accesibilidad/TalkBack, menú de opciones, carga y errores. El diálogo de baja va en
// UnenrollCourseDialogTest. Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
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
        composeRule.setContentInSpanish { GradusTheme { ScreenCourseDetail(state, actions) } }

    // ── Vista no inscrito (enrolled = false) ──

    @Test
    fun unenrolled_shows_course_info_stats_modules_and_tags() {
        show(CourseDetailState.Ready(course, enrolled = false))

        composeRule.onNodeWithText("Introducción a los grafos").assert(isHeading).assertIsDisplayed()
        composeRule.onNodeWithText("por Ana Ruiz").assertIsDisplayed()

        // StatColumn usa clearAndSetSemantics para TalkBack: los textos visuales se comprueban
        // en el árbol sin fusionar (hasParent distingue el «2» de la estadística del módulo 2).
        composeRule.onNode(hasText("2") and hasParent(hasContentDescription("2 módulos")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("módulos", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("nov 2026", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("publicado", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithText("Cómo se guarda un grafo en memoria y cómo se recorre en anchura.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Qué vas a aprender").performScrollTo().assert(isHeading).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1, Representar un grafo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2, Recorrido en anchura (BFS)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("grafos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("algoritmos").performScrollTo().assertIsDisplayed()
    }

    // «nov 2026» se ve; TalkBack oye el mes entero.
    @Test
    fun unenrolled_talkback_reads_modules_and_full_month() {
        show(CourseDetailState.Ready(course, enrolled = false))

        composeRule.onNodeWithContentDescription("2 módulos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("publicado en noviembre de 2026").assertIsDisplayed()
    }

    @Test
    fun unenrolled_hides_options_menu_and_progress_block() {
        show(CourseDetailState.Ready(course, enrolled = false))

        composeRule.onNodeWithContentDescription("Más opciones").assertDoesNotExist()
        composeRule.onNodeWithText("Tu progreso").assertDoesNotExist()
    }

    @Test
    fun enroll_button_is_big_enough_and_reports_the_tap() {
        var enrolls = 0
        show(CourseDetailState.Ready(course, enrolled = false), CourseDetailActions(onEnroll = { enrolls++ }))

        composeRule.onNodeWithText("Inscribirse al curso").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, enrolls)
    }

    @Test
    fun while_enrolling_the_button_is_disabled_and_ignores_taps() {
        var enrolls = 0
        show(
            CourseDetailState.Ready(course, enrolled = false, enrolling = true),
            CourseDetailActions(onEnroll = { enrolls++ }),
        )

        composeRule.onNodeWithText("Inscribiendo…").assertIsDisplayed().assertIsNotEnabled().performClick()

        assertEquals(0, enrolls)
    }

    @Test
    fun a_course_without_publication_hides_author_and_published_stat() {
        show(CourseDetailState.Ready(course.copy(publication = null), enrolled = false))

        composeRule.onNodeWithText("por", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("publicado", substring = true, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("2 módulos").assertIsDisplayed()
    }

    // ── Vista ya inscrito (enrolled = true) ──

    @Test
    fun enrolled_shows_published_line_progress_mastered_module_and_go_button() {
        var started = 0
        show(
            CourseDetailState.Ready(course, mastered = setOf("repr"), enrolled = true),
            CourseDetailActions(onStart = { started++ }),
        )

        composeRule.onNodeWithText("Publicado en nov 2026")
            .assertIsDisplayed()
            .assert(hasContentDescription("publicado en noviembre de 2026"))
        composeRule.onNodeWithText("Tu progreso").assertIsDisplayed()
        composeRule.onNodeWithText("50 %").assertIsDisplayed()
        composeRule.onNodeWithText("1 de 2 módulos dominados").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("1, Representar un grafo")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "dominado"))

        composeRule.onNodeWithText("Ir al curso").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, started)
    }

    @Test
    fun options_menu_pause_closes_without_error() {
        var openedDialog = 0
        show(
            CourseDetailState.Ready(course, enrolled = true),
            CourseDetailActions(onOpenUnenrollDialog = { openedDialog++ }),
        )

        composeRule.onNodeWithContentDescription("Más opciones")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.onNodeWithText("Pausar curso").assertIsDisplayed()
        composeRule.onNodeWithText("Darse de baja").assertIsDisplayed()

        composeRule.onNodeWithText("Pausar curso").performClick()

        composeRule.onNodeWithText("Pausar curso").assertDoesNotExist()
        composeRule.onNodeWithText("Darse de baja").assertDoesNotExist()
        assertEquals(0, openedDialog)
    }

    @Test
    fun options_menu_unenroll_closes_menu_and_opens_dialog() {
        var openedDialog = 0
        show(
            CourseDetailState.Ready(course, enrolled = true),
            CourseDetailActions(onOpenUnenrollDialog = { openedDialog++ }),
        )

        composeRule.onNodeWithContentDescription("Más opciones").performClick()
        composeRule.onNodeWithText("Darse de baja").performClick()

        composeRule.onNodeWithText("Darse de baja").assertDoesNotExist()
        assertEquals(1, openedDialog)
    }

    // ── Navegación, carga y problemas ──

    @Test
    fun the_back_button_is_big_enough_and_reports_the_tap() {
        var back = 0
        show(CourseDetailState.Ready(course), CourseDetailActions(onBack = { back++ }))

        composeRule.onNodeWithContentDescription("Volver")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, back)
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
