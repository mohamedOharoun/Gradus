package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.exercise.Question

// El modelo de un curso tal como lo define el backend (course-v1.schema.json), ya validado: sin
// ciclos y con referencias correctas (eso lo comprueba el validador del servidor, §6). Es el modelo
// de la app, independiente del JSON: un mapper lo traduce.

// Prueba de dominio: se sortean `questions` ejercicios de la reserva y hay que acertar al menos
// `minCorrect` (especificación §4.3).
data class Mastery(val questions: Int, val minCorrect: Int) {
    init {
        require(questions >= 1) { "La prueba de dominio necesita al menos un ejercicio." }
        require(minCorrect in 1..questions) { "Los aciertos necesarios deben estar entre 1 y $questions." }
    }
}

// Un paso de la parte formativa de un módulo: una lección para leer o un ejercicio para responder.
sealed interface Step {
    val id: String

    // Texto en Markdown.
    data class Lesson(override val id: String, val text: String) : Step

    data class Exercise(val question: Question) : Step {
        override val id: String get() = question.id
    }
}

// Un módulo es un nodo del grafo. `requires`: ids de sus prerrequisitos (aristas A → B).
// `steps`: la parte formativa, en orden. `review`: la reserva de la que sale la prueba de dominio.
// `mastery`: su propia prueba; si es null, se usa la del curso (`Course.defaultMastery`).
data class Module(
    val id: String,
    val title: String,
    val requires: List<String> = emptyList(),
    val steps: List<Step> = emptyList(),
    val review: List<Question> = emptyList(),
    val mastery: Mastery? = null,
) {
    // Los ejercicios de la parte formativa, sin las lecciones.
    val exercises: List<Question> get() = steps.filterIsInstance<Step.Exercise>().map { it.question }
}

// Quién publicó el curso, qué versión es y cuándo (los metadatos de GET /api/courses/{id}). Se
// enseñan en la ficha del curso. `publishedAt` va como llega («2026-11-18T08:35:00Z»): para el
// mes y el año, PublishedMonth.parse.
data class Publication(val author: Author, val version: Int, val publishedAt: String)

// `publication`: null en los cursos escritos en el código (ejemplo, tests) y si el servidor no
// mandara esos datos.
data class Course(
    val id: String,
    val title: String,
    val modules: List<Module>,
    val description: String = "",
    val language: String = "es",
    val tags: List<String> = emptyList(),
    val defaultMastery: Mastery = Mastery(questions = 4, minCorrect = 3),
    val publication: Publication? = null,
) {
    private val byId: Map<String, Module> = modules.associateBy { it.id }

    fun module(id: String): Module? = byId[id]

    // Las preguntas para practicar un módulo: los ejercicios de su parte formativa.
    fun questionsFor(moduleId: String): List<Question> = module(moduleId)?.exercises.orEmpty()
}
