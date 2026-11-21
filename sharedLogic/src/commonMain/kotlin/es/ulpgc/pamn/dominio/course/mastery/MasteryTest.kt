package es.ulpgc.pamn.dominio.course.mastery

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.exercise.PracticeSession
import es.ulpgc.pamn.dominio.course.exercise.Question
import kotlin.random.Random

// La prueba de dominio de un módulo (backend/docs/especificacion-cursos-v1.md): unos ejercicios
// sorteados de su reserva (`review`) y cuántos hay que acertar para dominarlo. Se responde con la
// misma práctica que la parte formativa (PracticeSession); esto solo decide qué se pregunta y si se aprueba.
data class MasteryTest(
    val moduleId: String,
    val questions: List<Question>,
    val minCorrect: Int,
) {
    fun resultFor(correctCount: Int): MasteryResult =
        if (correctCount >= minCorrect) MasteryResult.Passed else MasteryResult.Failed(missing = minCorrect - correctCount)

    fun start(random: Random): PracticeSession = PracticeSession.start(questions, random)

    // El resultado de la práctica de esta prueba; null mientras no se haya terminado.
    fun resultOf(session: PracticeSession): MasteryResult? =
        if (session.isFinished) resultFor(session.correctCount) else null
}

sealed interface MasteryResult {
    // «¡Módulo dominado!»: se desbloquea lo que dependa de él.
    data object Passed : MasteryResult

    // «Te faltó 1 para aprobar». Se puede repetir con otro sorteo.
    data class Failed(val missing: Int) : MasteryResult
}

// La prueba que toca a un módulo: la suya o, si no tiene, la del curso.
fun Course.masteryFor(module: Module): Mastery = module.mastery ?: defaultMastery

// Factory method: sortea una prueba nueva para el módulo (cada intento, un sorteo distinto).
// Recibe el Random para que los tests puedan fijar la semilla. Null si el módulo no existe o no
// tiene reserva (en los cursos del servidor no pasa: el validador exige al menos un ejercicio).
fun Course.masteryTest(moduleId: String, random: Random): MasteryTest? {
    val module = module(moduleId) ?: return null
    if (module.review.isEmpty()) return null
    val mastery = masteryFor(module)
    // Si la reserva fuese más pequeña que la prueba, se usa entera y no se piden más aciertos que preguntas.
    val questions = module.review.shuffled(random).take(mastery.questions)
    return MasteryTest(moduleId, questions, minCorrect = minOf(mastery.minCorrect, questions.size))
}
