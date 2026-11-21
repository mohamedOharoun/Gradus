package es.ulpgc.pamn.dominio.course.study

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.course.exercise.PracticeSession
import es.ulpgc.pamn.dominio.course.mastery.MasteryResult
import es.ulpgc.pamn.dominio.course.mastery.MasteryTest
import es.ulpgc.pamn.dominio.course.mastery.masteryTest
import kotlin.random.Random

// En qué punto del estudio de un módulo se está. El orden es siempre:
// Learning → TestIntro → Testing → Finished (y, si se suspende, otra vez Testing).
// Un módulo sin reserva acaba en Done después de sus pasos.
sealed interface StudyStage {
    data class Learning(val steps: StepSession) : StudyStage

    // Antes de empezar la prueba: cuántas preguntas tiene y cuántas hay que acertar.
    data class TestIntro(val test: MasteryTest) : StudyStage

    data class Testing(val test: MasteryTest, val practice: PracticeSession) : StudyStage

    data class Finished(val correctCount: Int, val total: Int, val result: MasteryResult) : StudyStage

    data object Done : StudyStage
}

// Estudiar un módulo de principio a fin. Máquina de estados inmutable: cada acción devuelve otro
// ModuleStudy; las que no tocan en la etapa actual lo devuelven igual. Las acciones que sortean
// (pasar a la prueba, repetirla) reciben el Random para que los tests puedan fijar la semilla.
data class ModuleStudy(val course: Course, val module: Module, val stage: StudyStage) {

    companion object {
        // Factory method. Null si el módulo no existe.
        fun start(course: Course, moduleId: String, random: Random): ModuleStudy? {
            val module = course.module(moduleId) ?: return null
            val study = ModuleStudy(course, module, StudyStage.Learning(StepSession.start(module.steps, random)))
            return study.afterStepsIfFinished(random)
        }
    }

    // El ejercicio que se está respondiendo ahora (null en una lección o fuera de los ejercicios).
    val currentExercise: ExerciseState?
        get() = when (stage) {
            is StudyStage.Learning -> (stage.steps.current as? StepState.Answering)?.exercise
            is StudyStage.Testing -> stage.practice.current
            else -> null
        }

    val canContinue: Boolean
        get() = when (stage) {
            is StudyStage.Learning -> stage.steps.canContinue
            is StudyStage.Testing -> stage.practice.canContinue
            else -> false
        }

    val isPassed: Boolean get() = (stage as? StudyStage.Finished)?.result == MasteryResult.Passed

    fun update(change: (ExerciseState) -> ExerciseState): ModuleStudy = when (stage) {
        is StudyStage.Learning -> copy(stage = stage.copy(steps = stage.steps.update(change)))
        is StudyStage.Testing -> copy(stage = stage.copy(practice = stage.practice.update(change)))
        else -> this
    }

    // «Continuar»: al siguiente paso o pregunta. Al acabar los pasos se sortea la prueba; al acabar
    // la prueba, se da el resultado.
    fun next(random: Random): ModuleStudy = when (stage) {
        is StudyStage.Learning -> copy(stage = stage.copy(steps = stage.steps.next())).afterStepsIfFinished(random)
        is StudyStage.Testing -> {
            val practice = stage.practice.next()
            val result = stage.test.resultOf(practice)
            copy(stage = if (result == null) stage.copy(practice = practice) else StudyStage.Finished(practice.correctCount, practice.total, result))
        }
        else -> this
    }

    fun startTest(random: Random): ModuleStudy =
        if (stage is StudyStage.TestIntro) copy(stage = StudyStage.Testing(stage.test, stage.test.start(random))) else this

    // «Repetir la prueba» tras suspender: un sorteo nuevo, sin volver a las lecciones.
    fun retry(random: Random): ModuleStudy {
        val finished = stage as? StudyStage.Finished ?: return this
        if (finished.result == MasteryResult.Passed) return this
        val test = course.masteryTest(module.id, random) ?: return this
        return copy(stage = StudyStage.Testing(test, test.start(random)))
    }

    private fun afterStepsIfFinished(random: Random): ModuleStudy {
        val learning = stage as? StudyStage.Learning ?: return this
        if (!learning.steps.isFinished) return this
        val test = course.masteryTest(module.id, random)
        return copy(stage = if (test == null) StudyStage.Done else StudyStage.TestIntro(test))
    }
}
