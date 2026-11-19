package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.exercise.Question

// Un concepto es un nodo del grafo. `requires` son los ids de sus prerrequisitos (aristas A → B).
data class Concept(
    val id: String,
    val title: String,
    val requires: List<String> = emptyList(),
)

// Un curso ya validado (sin ciclos y con referencias correctas: eso lo comprueba el validador, §6).
// Es el modelo de la app, independiente del JSON del backend: un mapper lo traducirá.
data class Course(
    val id: String,
    val title: String,
    val concepts: List<Concept>,
    val questions: List<Question> = emptyList(),
) {
    private val byId: Map<String, Concept> = concepts.associateBy { it.id }

    fun concept(id: String): Concept? = byId[id]

    // Las preguntas para practicar un concepto, en el orden en que vienen en el curso.
    fun questionsFor(conceptId: String): List<Question> = questions.filter { it.conceptId == conceptId }
}
