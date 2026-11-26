package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.study.ModuleStudy
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

// Estudiar un módulo con la app en inglés (docs/tareas/ajustes.md, fase 3, paso 8b): lo de la app, en inglés;
// la pregunta, sus opciones y su explicación, en el idioma del autor (decisión R).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class StudyInEnglishTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val random = Random(1)
    private val question = SingleChoice("f1", "¿Qué usa BFS?", listOf("Una cola", "Una pila"), correct = 0, explanation = "BFS va por capas.")
    private val photo = Step.Photo("foto", "Dibuja el grafo.", "Cuatro vértices.")
    private val course = Course(
        "c", "Grafos",
        listOf(
            Module("bfs", "Recorrido en anchura", steps = listOf(Step.Exercise(question)), review = listOf(question, question), mastery = Mastery(2, 1)),
            Module("foto", "Con foto", steps = listOf(photo)),
        ),
    )

    private fun show(study: ModuleStudy) = composeRule.setContentInEnglish { GradusTheme { ScreenExercise(study, ExerciseActions()) } }

    private fun start(module: String = "bfs") = ModuleStudy.start(course, module, random)!!

    @Test
    fun a_question_is_in_the_language_of_its_author_and_the_app_in_english() {
        show(start())
        composeRule.onNodeWithText("¿Qué usa BFS?").assertIsDisplayed()
        composeRule.onNodeWithText("Choose one answer.").assertIsDisplayed()
        composeRule.onNodeWithText("Check").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Step 1 of 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close and go back to the path").assertIsDisplayed()
    }

    @Test
    fun a_wrong_answer_shows_the_solution_in_english_with_the_option_as_written() {
        show(start().update { it.choose(1).check() })
        composeRule.onNodeWithText("Not correct").assertIsDisplayed()
        composeRule.onNodeWithText("The answer was “Una cola”.").assertIsDisplayed()
        composeRule.onNodeWithText("BFS va por capas.").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    // Plural en inglés y «al menos» con el mínimo.
    @Test
    fun the_mastery_test_intro_is_in_english() {
        show(start().update { it.choose(0).check() }.next(random))
        composeRule.onNodeWithText("Mastery test").assertIsDisplayed()
        composeRule.onNodeWithText("Answer 2 questions about “Recorrido en anchura”.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Start the test").assertIsDisplayed()
    }

    @Test
    fun the_photo_step_is_in_english() {
        show(start("foto"))
        composeRule.onNodeWithText("Dibuja el grafo.").assertIsDisplayed()
        composeRule.onNodeWithText("Take photo").assertIsDisplayed()
        composeRule.onNodeWithText("Submit").assertIsDisplayed()
        composeRule.onNodeWithText("Submit later").assertIsDisplayed()
    }
}
