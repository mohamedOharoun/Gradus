package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.mastery.MasteryResult
import es.ulpgc.pamn.dominio.course.mastery.MasteryTest
import es.ulpgc.pamn.dominio.course.study.StudyStage
import es.ulpgc.pamn.dominio.ui.components.joinedWithAnd
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// Los textos del estudio de un módulo (docs/tareas/ajustes.md, fase 3, paso 8b). Las opciones, las parejas
// y el título del módulo los escribe el autor: van como datos, sin traducir (decisión R).
class ExerciseTextsTest {

    private val single = SingleChoice("s", "¿Qué usa BFS?", listOf("Una pila", "Una cola"), correct = 1)
    private val multiple = MultipleChoice("m", "¿Cuáles son lineales?", listOf("Lista", "Árbol", "Cola"), correct = setOf(2, 0))
    private val multipleOne = MultipleChoice("m1", "¿Cuál?", listOf("Lista", "Árbol"), correct = setOf(1))
    private val matching = Matching("p", "Une", listOf(MatchPair("BFS", "Cola"), MatchPair("DFS", "Pila")))

    // ── Qué hay que hacer ──

    @Test
    fun `each kind of question says what to do`() {
        assertEquals(UiText.of(R.string.exercise_hint_single), single.hint())
        assertEquals(UiText.of(R.string.exercise_hint_multiple), multiple.hint())
        assertEquals(UiText.of(R.string.exercise_hint_matching), matching.hint())
    }

    // ── La solución, al fallar ──

    @Test
    fun `the solution of a single choice is its right option`() {
        assertEquals(UiText.of(R.string.exercise_solution_single, "Una cola"), single.solution())
    }

    @Test
    fun `a multiple choice with one right option says it in singular`() {
        assertEquals(UiText.of(R.string.exercise_solution_multiple_one, "Árbol"), multipleOne.solution())
    }

    // En el orden de las opciones, cada una entre comillas (las de cada idioma) y unidas con «y».
    @Test
    fun `a multiple choice lists its right options in order`() {
        val quoted = listOf(UiText.of(R.string.quoted, "Lista"), UiText.of(R.string.quoted, "Cola"))
        assertEquals(UiText.of(R.string.exercise_solution_multiple, joinedWithAnd(quoted)), multiple.solution())
    }

    @Test
    fun `a matching lists its pairs`() {
        assertEquals(UiText.of(R.string.exercise_solution_pairs, "BFS → Cola, DFS → Pila"), matching.solution())
    }

    // ── La prueba de dominio ──

    @Test
    fun `before the test it says how many questions and that all must be right`() {
        val test = MasteryTest("bfs", listOf(single, multiple), minCorrect = 2)
        assertEquals(
            UiText.of(R.string.test_intro_all, UiText.plural(R.plurals.test_questions, 2), "Grafos"),
            testIntroText("Grafos", test),
        )
    }

    @Test
    fun `before the test it says the minimum when not all must be right`() {
        val test = MasteryTest("bfs", listOf(single, multiple, matching), minCorrect = 2)
        assertEquals(
            UiText.of(R.string.test_intro_min, UiText.plural(R.plurals.test_questions, 3), "Grafos", 2),
            testIntroText("Grafos", test),
        )
    }

    @Test
    fun `passing says the module is mastered`() {
        val passed = StudyStage.Finished(correctCount = 3, total = 3, result = MasteryResult.Passed)
        assertEquals(UiText.of(R.string.test_passed_title), passed.title())
        assertEquals(UiText.of(R.string.test_passed_body, 3, 3, "Grafos"), passed.body("Grafos"))
    }

    // «Te faltó 1» / «Te faltaron 2»: un plural.
    @Test
    fun `failing says how many were missing`() {
        val failed = StudyStage.Finished(correctCount = 1, total = 3, result = MasteryResult.Failed(missing = 2))
        assertEquals(UiText.plural(R.plurals.test_failed_title, 2), failed.title())
        assertEquals(UiText.of(R.string.test_failed_body, 1, 3), failed.body("Grafos"))
    }
}
