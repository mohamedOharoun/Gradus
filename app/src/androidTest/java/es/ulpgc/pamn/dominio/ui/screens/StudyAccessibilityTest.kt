package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.study.ModuleStudy
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.course.statesFor
import es.ulpgc.pamn.dominio.ui.components.CourseMap
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblemContent
import es.ulpgc.pamn.dominio.ui.screens.exercise.ExerciseActions
import es.ulpgc.pamn.dominio.ui.screens.exercise.ScreenExercise
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

// Lo que TalkBack lee al estudiar un curso (tarea 9 de cursos-backend-app.md): encabezados,
// estados, avisos que se leen solos y zonas táctiles de 48 dp. Se ejecuta en el emulador:
// ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class StudyAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val random = Random(1)
    private val question = SingleChoice("f1", "¿Qué usa BFS?", listOf("Una cola", "Una pila"), correct = 0, explanation = "BFS va por **capas**.")
    private val module = Module(
        "bfs", "Recorrido en anchura",
        steps = listOf(Step.Lesson("l1", "BFS usa una cola:\n\n- **Encolar** s\n- Sacar u"), Step.Exercise(question)),
        review = listOf(question),
        mastery = Mastery(questions = 1, minCorrect = 1),
    )
    private val course = Course("c", "Grafos", listOf(module))

    private fun show(study: ModuleStudy) = composeRule.setContent { GradusTheme { ScreenExercise(study, ExerciseActions()) } }

    private fun start() = ModuleStudy.start(course, "bfs", random)!!

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
    private val hasLiveRegion = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)
    private val hasState = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

    @Test
    fun a_lesson_has_a_heading_says_which_step_it_is_and_reads_each_list_item_whole() {
        show(start())

        composeRule.onNodeWithText("Recorrido en anchura").assert(isHeading)
        composeRule.onNodeWithContentDescription("Paso 1 de 2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cerrar y volver al camino").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        // El punto y el texto, juntos en un solo nodo.
        composeRule.onNode(hasText("•") and hasText("Encolar", substring = true)).assertIsDisplayed()
    }

    @Test
    fun after_checking_an_exercise_the_result_and_the_explanation_are_read_aloud() {
        show(start().next(random).update { it.choose(1).check() })

        composeRule.onNode(hasText("No es correcto") and hasLiveRegion).assertIsDisplayed()
        composeRule.onNodeWithText("BFS va por capas.").assertIsDisplayed()
        // Cada opción dice si estaba bien o mal (no solo con colores).
        composeRule.onNodeWithText("Una cola").assert(hasState)
        composeRule.onNodeWithText("Una pila").assert(hasState)
    }

    @Test
    fun the_test_says_which_question_it_is() {
        show(start().next(random).update { it.choose(0).check() }.next(random).startTest(random))

        composeRule.onNodeWithContentDescription("Pregunta 1 de 1").assertIsDisplayed()
        composeRule.onNodeWithText("¿Qué usa BFS?").assert(isHeading)
    }

    @Test
    fun the_result_is_a_heading_read_aloud_with_big_buttons() {
        val failed = start().next(random).update { it.choose(0).check() }.next(random)
            .startTest(random).update { it.choose(1).check() }.next(random)
        show(failed)

        composeRule.onNodeWithText("Te faltó 1 para aprobar").assert(isHeading).assert(hasLiveRegion)
        composeRule.onNodeWithText("Repetir la prueba").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Volver al camino").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun a_problem_downloading_is_read_aloud_and_its_buttons_are_big_enough() {
        composeRule.setContent { GradusTheme { CourseProblemContent(CourseProblem.UNREACHABLE, onRetry = {}, onBack = {}) } }

        // La región viva es el bloque del aviso: TalkBack lee todo lo que tiene dentro.
        composeRule.onNode(hasLiveRegion and hasAnyDescendant(hasText("Sin conexión"))).assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertHeightIsAtLeast(48.dp)
        // El botón atrás se ve de 44 dp, pero su zona táctil llega a 48.
        composeRule.onNodeWithContentDescription("Volver").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun every_module_of_the_path_says_its_state() {
        composeRule.setContent {
            GradusTheme { CourseMap(demoCourse, demoCourse.statesFor(demoMastered), onModuleClick = {}) }
        }

        listOf("Conjuntos", "Grafos", "Colisiones").forEach { title ->
            composeRule.onNodeWithText(title).assert(hasState)
        }
    }
}
