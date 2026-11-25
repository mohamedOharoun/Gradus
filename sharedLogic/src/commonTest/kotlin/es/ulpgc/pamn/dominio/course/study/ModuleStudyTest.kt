package es.ulpgc.pamn.dominio.course.study

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import es.ulpgc.pamn.dominio.course.mastery.MasteryResult
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission
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

    // ── Paso con foto (docs/tareas/ejercicio-foto.md, decisiones R y S) ──

    private val photo = Step.Photo("foto", "Dibuja el grafo y entrégalo.", "Cuatro vértices.")
    private val withPhoto = Course(
        "c", "Curso",
        listOf(
            Module("m", "Con foto", steps = listOf(photo, lesson), review = listOf(question("r1")), mastery = Mastery(1, 1)),
            Module("solo", "Solo la foto", steps = listOf(photo)),
        ),
    )

    private fun startWithPhoto(moduleId: String = "m") = ModuleStudy.start(withPhoto, moduleId, random)!!

    @Test
    fun `a photo step is studied as its own kind of step`() {
        val stage = startWithPhoto().stage
        assertIs<StudyStage.Learning>(stage)
        assertEquals(StepState.Photo(PhotoStep(photo)), stage.steps.current)
    }

    // Hay que decidir algo: entregarla (tarea 5) o dejarla para más tarde. No se pasa de largo.
    @Test
    fun `a photo step cannot be passed without deciding`() {
        val study = startWithPhoto()
        assertFalse(study.canContinue)
        assertEquals(study, study.next(random))
    }

    // Decisión S: sin sesión o sin cámara, el alumno no se queda bloqueado.
    @Test
    fun `leaving the photo for later lets the student go on`() {
        val later = startWithPhoto().submitPhotoLater()
        assertTrue(later.canContinue)
        val next = later.next(random).stage
        assertIs<StudyStage.Learning>(next)
        assertEquals(StepState.Reading(lesson), next.steps.current)
    }

    // La foto no es una pregunta: no hay respuesta que elegir ni que comprobar.
    @Test
    fun `there is no exercise to answer at a photo step`() {
        val study = startWithPhoto()
        assertNull(study.currentExercise)
        assertEquals(study, study.update { it.choose(0).check() })
    }

    // La prueba de dominio sale solo de la reserva: una foto en los pasos no la cambia.
    @Test
    fun `after the steps the mastery test comes from the review pool as usual`() {
        val study = startWithPhoto().submitPhotoLater().next(random).next(random)
        assertIs<StudyStage.TestIntro>(study.stage)
        assertEquals(1, study.stage.test.questions.size)
    }

    @Test
    fun `a module with only a photo step ends when it is left for later`() {
        assertEquals(StudyStage.Done, startWithPhoto("solo").submitPhotoLater().next(random).stage)
    }

    @Test
    fun `the current photo step is known while it is on screen`() {
        assertEquals(PhotoStep(photo), startWithPhoto().currentPhoto)
        assertNull(start("bfs").currentPhoto)
    }

    // La foto se cambia por el id del paso: una respuesta de la red que llega tarde va al suyo.
    @Test
    fun `a photo step is changed by its id`() {
        val study = startWithPhoto().updatePhoto("foto") { it.take("a.jpg") }
        assertEquals(PhotoTask.Ready("a.jpg"), study.currentPhoto!!.task)
    }

    @Test
    fun `changing a photo step that does not exist changes nothing`() {
        val study = startWithPhoto()
        assertEquals(study, study.updatePhoto("no-existe") { it.take("a.jpg") })
        val atLesson = start("bfs")
        assertEquals(atLesson, atLesson.updatePhoto("l1") { it.take("a.jpg") })
    }

    @Test
    fun `a photo step is only changed while learning`() {
        val intro = start("dfs")
        assertEquals(intro, intro.updatePhoto("foto") { it.take("a.jpg") })
    }

    // Entregada, se ve la solución y se puede seguir (decisión C).
    @Test
    fun `once the photo is sent the student goes on`() {
        val sent = startWithPhoto().updatePhoto("foto") {
            it.take("a.jpg").send().finished(SubmitOutcome.Submitted(Submission("s1", "2026-11-25T10:00:00Z")))
        }
        assertTrue(sent.canContinue)
        assertEquals(StepState.Reading(lesson), (sent.next(random).stage as StudyStage.Learning).steps.current)
    }

    // Mientras se entrega, «más tarde» no hace nada: así el ViewModel no avanza.
    @Test
    fun `while the photo is being sent it is not left for later`() {
        val sending = startWithPhoto().updatePhoto("foto") { it.take("a.jpg").send() }
        assertEquals(sending, sending.submitPhotoLater())
        assertFalse(sending.canContinue)
    }

    // ── Los ficheros de las fotos (decisión K: la app borra los que dejan de usarse) ──

    @Test
    fun `the photo files in use are those of the photo steps`() {
        assertEquals(emptySet(), startWithPhoto().photoPaths)
        val ready = startWithPhoto().updatePhoto("foto") { it.take("a.jpg") }
        assertEquals(setOf("a.jpg"), ready.photoPaths)
    }

    @Test
    fun `a photo file stops being in use once it is sent or left for later`() {
        val ready = startWithPhoto().updatePhoto("foto") { it.take("a.jpg") }
        val sent = ready.updatePhoto("foto") {
            it.send().finished(SubmitOutcome.Submitted(Submission("s1", "2026-11-25T10:00:00Z")))
        }
        assertEquals(emptySet(), sent.photoPaths)
        assertEquals(emptySet(), ready.submitPhotoLater().photoPaths)
    }

    // Después de los pasos (la prueba, el resultado) ya no queda ninguna foto en uso.
    @Test
    fun `after the steps no photo file is in use`() {
        val later = startWithPhoto().updatePhoto("foto") { it.take("a.jpg") }.submitPhotoLater().next(random).next(random)
        assertIs<StudyStage.TestIntro>(later.stage)
        assertEquals(emptySet(), later.photoPaths)
    }

    @Test
    fun `leaving for later anywhere but at a photo step changes nothing`() {
        val atLesson = start("bfs")
        assertEquals(atLesson, atLesson.submitPhotoLater())
        val atTest = start("dfs")
        assertEquals(atTest, atTest.submitPhotoLater())
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
