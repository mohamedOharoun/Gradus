package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.exercise.Question

// Un módulo es un nodo del grafo. `requires` son los ids de sus prerrequisitos (aristas A → B).
data class Module(
    val id: String,
    val title: String,
    val requires: List<String> = emptyList(),
)

// Un curso ya validado (sin ciclos y con referencias correctas: eso lo comprueba el validador, §6).
// Es el modelo de la app, independiente del JSON del backend: un mapper lo traducirá.
data class Course(
    val id: String,
    val title: String,
    val modules: List<Module>,
    val questions: List<Question> = emptyList(),
) {
    private val byId: Map<String, Module> = modules.associateBy { it.id }

    fun module(id: String): Module? = byId[id]

    // Las preguntas para practicar un módulo, en el orden en que vienen en el curso.
    fun questionsFor(moduleId: String): List<Question> = questions.filter { it.moduleId == moduleId }
}
