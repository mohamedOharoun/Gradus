package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.ui.screens.detail.CourseDetailActions
import es.ulpgc.pamn.dominio.ui.screens.detail.CourseDetailState
import es.ulpgc.pamn.dominio.ui.screens.detail.ScreenCourseDetail
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// El camino y la ficha de un curso con la app en inglés (docs/tareas/ajustes.md, fase 3, paso 8a): lo de la
// app sale en inglés y lo que escribió el autor se queda en su idioma (decisión R). En español ya lo
// comprueban sus propios tests. Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class CourseInEnglishTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Un curso escrito en español.
    private val course = Course(
        id = "g",
        title = "Introducción a los grafos",
        modules = listOf(Module("bfs", "Recorrido en anchura (BFS)", requires = listOf("repr")), Module("repr", "Representar un grafo")),
        description = "Cómo se guarda un grafo en memoria.",
        publication = Publication(Author("u1", "Ana Ruiz"), version = 2, publishedAt = "2026-11-18T08:35:00Z"),
    )

    @Test
    fun the_course_page_is_in_english_but_the_course_keeps_its_language() {
        composeRule.setContentInEnglish {
            GradusTheme {
                ScreenCourseDetail(
                    CourseDetailState.Ready(course, mastered = setOf("repr"), enrolled = true),
                    CourseDetailActions(),
                )
            }
        }

        // De la app: en inglés.
        composeRule.onNodeWithText("by Ana Ruiz").assertIsDisplayed()
        composeRule.onNodeWithText("Published in Nov 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Your progress").assertIsDisplayed()
        composeRule.onNodeWithText("50%").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2 modules mastered").assertIsDisplayed()
        composeRule.onNodeWithText("What you'll learn").assertExists()
        composeRule.onNodeWithText("Go to course").assertIsDisplayed()
        // Del autor: tal cual, en español.
        composeRule.onNodeWithText("Introducción a los grafos").assertIsDisplayed()
        composeRule.onNodeWithText("Cómo se guarda un grafo en memoria.").assertExists()
        // En el orden del camino: «Representar un grafo» es requisito de BFS, así que va primero.
        composeRule.onNodeWithContentDescription("2, Recorrido en anchura (BFS)").assertExists()
    }

    @Test
    fun the_unenrolled_course_page_is_in_english() {
        composeRule.setContentInEnglish {
            GradusTheme {
                ScreenCourseDetail(
                    CourseDetailState.Ready(course, enrolled = false),
                    CourseDetailActions(),
                )
            }
        }

        composeRule.onNodeWithText("Enroll in course").assertIsDisplayed()
        composeRule.onNodeWithText("modules", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("published", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 modules").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("published in November 2026").assertIsDisplayed()
    }

    @Test
    fun the_module_sheet_is_in_english() {
        composeRule.setContentInEnglish { GradusTheme { ModuleSheetContent("Representar un grafo", ModuleState.MASTERED, 3, ModuleSheetActions()) } }

        composeRule.onNodeWithText("Representar un grafo").assertIsDisplayed()
        composeRule.onNodeWithText("Mastered").assertIsDisplayed()
        composeRule.onNodeWithText("Review").assertIsDisplayed()
        // TalkBack: el plural en inglés.
        composeRule.onNodeWithContentDescription("My notes, 3 photos").assertIsDisplayed()
    }

    @Test
    fun one_photo_is_singular() {
        composeRule.setContentInEnglish { GradusTheme { ModuleSheetContent("BFS", ModuleState.FRONTIER, 1, ModuleSheetActions()) } }
        composeRule.onNodeWithContentDescription("My notes, 1 photo").assertIsDisplayed()
        composeRule.onNodeWithText("Study").assertIsDisplayed()
    }

    @Test
    fun a_problem_opening_the_course_is_in_english() {
        composeRule.setContentInEnglish { GradusTheme { CourseProblemContent(CourseProblem.UNREACHABLE, onRetry = {}, onBack = {}) } }
        composeRule.onNodeWithText("No connection").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
