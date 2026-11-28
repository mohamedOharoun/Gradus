package es.ulpgc.pamn.dominio.course.exercise

// Lo que responde el estudiante, un tipo por cada tipo de pregunta. Usa índices, no textos:
// así no importa en qué orden se enseñen las opciones.
sealed interface Answer {
    data class Single(val choice: Int) : Answer

    data class Multiple(val choices: Set<Int>) : Answer

    // Índice de la izquierda → índice de la derecha (los dos, según el orden de `Matching.pairs`).
    data class Matches(val pairs: Map<Int, Int>) : Answer
}
