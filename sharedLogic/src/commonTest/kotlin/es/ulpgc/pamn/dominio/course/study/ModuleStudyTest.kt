package es.ulpgc.pamn.dominio.course.study

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.mastery.MasteryResult
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Estudiar un módulo: la parte formativa (lecciones y ejercicios, en orden), la prueba de dominio
// sorteada de la reserva y su resultado.
class ModuleStudyTest {

    private fun question(id: String) = SingleChoice(id, "¿$id?", listOf("Bien", "Mal"), correct = 0)

    private val lesson = Step.Lesson("l1", "Un **grafo**…")
    private val bfs = Module(
        "bfs", "BFS",
        steps = listOf(lesson, Step.Exercise(question("f1"))),
        review = (1..3).map { question("r$it") },
        mastery = Mastery(questions = 2, minCorrect = 2),
    )
    private val noSteps = Module("dfs", "DFS", review = listOf(question("d1")), mastery = Mastery(1, 1))
    private val noReview = Module("intro", "Intro", steps = listOf(lesson))
    private val course = Course("c", "Curso", listOf(bfs, noSteps, noReview))
    private val random = Random(1)

    private fun start(moduleId: String) = ModuleStudy.start(course, moduleId, random)!!

    // Responde el ejercicio actual (bien: opción 0; mal: opción 1), comprueba y sigue.
    private fun ModuleStudy.answer(right: Boolean) = update { it.choose(if (right) 0 else 1).check() }.next(random)

    private fun ModuleStudy.inTest(): ModuleStudy {
        var study = this
        while (study.stage !is StudyStage.TestIntro) study = study.update { it.choose(0).check() }.next(random)
        return study.startTest(random)
    }

    // ── Parte formativa ──

    @Test
    fun `it starts with the first step of the module`() {
        val stage = start("bfs").stage
        assertIs<StudyStage.Learning>(stage)
        assertEquals(StepState.Reading(lesson), stage.steps.current)
    }

    @Test
    fun `a lesson can be passed at once, an exercise only after checking it`() {
        val study = start("bfs")
        assertTrue(study.canContinue)
        val atExercise = study.next(random)
        assertFalse(atExercise.canContinue)
        assertTrue(atExercise.update { it.choose(1).check() }.canContinue)
    }

    @Test
    fun `a wrong answer in the steps does not stop you`() {
        val afterSteps = start("bfs").next(random).answer(right = false)
        assertIs<StudyStage.TestIntro>(afterSteps.stage)
    }

    @Test
    fun `the current exercise is the one being answered`() {
        assertNull(start("bfs").currentExercise)
        assertEquals("f1", start("bfs").next(random).currentExercise?.question?.id)
    }

    // ── Prueba de dominio ──

    @Test
    fun `after the steps comes the mastery test, with the questions and right answers it asks for`() {
        val intro = start("bfs").next(random).answer(right = true).stage
        assertIs<StudyStage.TestIntro>(intro)
        assertEquals(2, intro.test.questions.size)
        assertEquals(2, intro.test.minCorrect)
        assertTrue(intro.test.questions.all { it.id.startsWith("r") })
    }

    @Test
    fun `a module without steps goes straight to the test`() {
        assertIs<StudyStage.TestIntro>(start("dfs").stage)
    }

    @Test
    fun `starting the test asks its questions one by one`() {
        val testing = start("dfs").startTest(random).stage
        assertIs<StudyStage.Testing>(testing)
        assertEquals("d1", testing.practice.current?.question?.id)
    }

    @Test
    fun `answering enough questions right passes`() {
        val finished = start("bfs").inTest().answer(right = true).answer(right = true)
        assertEquals(StudyStage.Finished(correctCount = 2, total = 2, result = MasteryResult.Passed), finished.stage)
        assertTrue(finished.isPassed)
    }

    @Test
    fun `too few right answers fails, saying how many were missing`() {
        val finished = start("bfs").inTest().answer(right = true).answer(right = false)
        assertEquals(MasteryResult.Failed(missing = 1), (finished.stage as StudyStage.Finished).result)
        assertFalse(finished.isPassed)
    }

    // Repetir la prueba sortea otra vez y no obliga a repetir las lecciones.
    @Test
    fun `a failed test can be repeated with a new draw`() {
        val failed = start("bfs").inTest().answer(right = false).answer(right = false)
        val again = failed.retry(random)
        assertIs<StudyStage.Testing>(again.stage)
        assertEquals(2, again.stage.practice.total)
    }

    @Test
    fun `a passed test cannot be repeated`() {
        val passed = start("dfs").startTest(random).answer(right = true)
        assertEquals(passed, passed.retry(random))
    }

    // ── Sin prueba ──

    @Test
    fun `a module without review pool just ends after its steps`() {
        assertEquals(StudyStage.Done, start("intro").next(random).stage)
    }

    @Test
    fun `there is nothing to study in a module that does not exist`() {
        assertNull(ModuleStudy.start(course, "no-existe", random))
    }

    // ── Acciones fuera de lugar ──

    @Test
    fun `actions that do not fit the stage change nothing`() {
        val learning = start("bfs")
        assertEquals(learning, learning.startTest(random))
        assertEquals(learning, learning.update { it.choose(0) })
        val intro = start("dfs")
        assertEquals(intro, intro.next(random))
    }
}
