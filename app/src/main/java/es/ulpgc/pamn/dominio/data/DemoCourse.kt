package es.ulpgc.pamn.dominio.data

import es.ulpgc.pamn.dominio.course.Concept
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice

// Curso de ejemplo para las previews y el emulador, hasta que llegue el backend de cursos.
// Tiene de todo: filas de un concepto (zigzag), un nivel de 3 y otro de 4 (que se parte en 2 + 2).
val demoCourse = Course(
    id = "estructuras-de-datos",
    title = "Estructuras de datos",
    concepts = listOf(
        Concept("conjuntos", "Conjuntos"),
        Concept("relaciones", "Relaciones", requires = listOf("conjuntos")),
        Concept("grafos", "Grafos", requires = listOf("relaciones")),
        Concept("arboles", "Árboles", requires = listOf("relaciones")),
        Concept("hash", "Tablas hash", requires = listOf("relaciones")),
        Concept("bfs", "Búsqueda en anchura (BFS)", requires = listOf("grafos")),
        Concept("dfs", "Búsqueda en profundidad (DFS)", requires = listOf("grafos", "arboles")),
        Concept("monticulos", "Montículos", requires = listOf("arboles")),
        Concept("colisiones", "Colisiones", requires = listOf("hash")),
        Concept("dijkstra", "Caminos mínimos", requires = listOf("bfs", "monticulos")),
    ),
    questions = demoQuestions(),
)

// Lo que el estudiante de ejemplo ya domina. Más adelante saldrá del registro de respuestas (Room).
val demoMastered = setOf("conjuntos", "relaciones", "arboles")

// Preguntas de los conceptos que ya se pueden practicar (dominados y frontera), de los tres tipos.
private fun demoQuestions() = listOf(
    SingleChoice(
        "conjuntos-1", "conjuntos", "¿Cuántos elementos distintos tiene el conjunto {1, 2, 2, 3}?",
        options = listOf("3", "4", "2"), correct = 0,
    ),
    SingleChoice(
        "relaciones-1", "relaciones", "Una relación R es simétrica si…",
        options = listOf("si a R b, entonces b R a", "si a R b y b R c, entonces a R c", "a R a para todo a"),
        correct = 0,
    ),
    MultipleChoice(
        "arboles-1", "arboles", "Marca lo que cumple todo árbol:",
        options = listOf("Tiene una raíz", "Tiene algún ciclo", "Entre dos nodos hay un único camino"),
        correct = setOf(0, 2),
    ),
    Matching(
        "arboles-2", "arboles", "Une cada término con su significado:",
        pairs = listOf(
            MatchPair("Raíz", "Nodo sin padre"),
            MatchPair("Hoja", "Nodo sin hijos"),
            MatchPair("Altura", "Camino más largo de la raíz a una hoja"),
        ),
    ),
    SingleChoice(
        "grafos-1", "grafos", "¿Qué es un grafo?",
        options = listOf("Vértices unidos por aristas", "Una lista ordenada de elementos", "Una tabla de dos columnas"),
        correct = 0,
    ),
    MultipleChoice(
        "grafos-2", "grafos", "Marca las afirmaciones verdaderas:",
        options = listOf(
            "Un grafo puede tener ciclos",
            "En un grafo, todas las aristas tienen dirección",
            "Un árbol es un tipo de grafo",
        ),
        correct = setOf(0, 2),
    ),
    Matching(
        "grafos-3", "grafos", "Une cada término con su significado:",
        pairs = listOf(
            MatchPair("Vértice", "Un nodo del grafo"),
            MatchPair("Arista", "La unión entre dos nodos"),
            MatchPair("Grado", "Cuántas aristas tiene un nodo"),
        ),
    ),
    SingleChoice(
        "hash-1", "hash", "¿Qué hace la función hash de una tabla hash?",
        options = listOf("Convierte una clave en una posición", "Ordena los elementos", "Borra los repetidos"),
        correct = 0,
    ),
    Matching(
        "hash-2", "hash", "Une cada término con su significado:",
        pairs = listOf(
            MatchPair("Clave", "El dato con el que se busca"),
            MatchPair("Colisión", "Dos claves en la misma posición"),
            MatchPair("Factor de carga", "Elementos entre tamaño de la tabla"),
        ),
    ),
    SingleChoice(
        "monticulos-1", "monticulos", "En un montículo de mínimos, ¿dónde está el elemento menor?",
        options = listOf("En la raíz", "En la última hoja", "En cualquier sitio"), correct = 0,
    ),
    MultipleChoice(
        "monticulos-2", "monticulos", "Marca lo que es cierto de un montículo binario:",
        options = listOf(
            "Es un árbol binario casi completo",
            "Se puede guardar en un array",
            "Está ordenado como un árbol binario de búsqueda",
        ),
        correct = setOf(0, 1),
    ),
)
