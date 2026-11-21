package es.ulpgc.pamn.dominio.data

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.Question
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice

// Curso de ejemplo solo para previews y tests (los cursos de verdad llegan del backend).
// Tiene de todo: filas de un módulo (zigzag), un nivel de 3 y otro de 4 (que se parte en 2 + 2),
// lecciones, ejercicios de los tres tipos, explicaciones y, en «grafos», reserva y prueba de dominio.
val demoCourse = Course(
    id = "estructuras-de-datos",
    title = "Estructuras de datos",
    description = "Conjuntos, grafos, árboles y tablas hash, paso a paso.",
    tags = listOf("estructuras de datos", "algoritmos"),
    modules = listOf(
        Module(
            "conjuntos", "Conjuntos",
            steps = listOf(
                Step.Lesson("conjuntos-l1", "Un **conjunto** es una colección sin elementos repetidos."),
                exercise(SingleChoice(
                    "conjuntos-1", "¿Cuántos elementos distintos tiene el conjunto {1, 2, 2, 3}?",
                    options = listOf("3", "4", "2"), correct = 0,
                    explanation = "El 2 repetido cuenta una sola vez: {1, 2, 3}.",
                )),
            ),
        ),
        Module(
            "relaciones", "Relaciones", requires = listOf("conjuntos"),
            steps = listOf(
                exercise(SingleChoice(
                    "relaciones-1", "Una relación R es simétrica si…",
                    options = listOf("si a R b, entonces b R a", "si a R b y b R c, entonces a R c", "a R a para todo a"),
                    correct = 0,
                )),
            ),
        ),
        Module(
            "grafos", "Grafos", requires = listOf("relaciones"),
            steps = listOf(
                Step.Lesson("grafos-l1", "Un **grafo** son *vértices* unidos por *aristas*."),
                exercise(SingleChoice(
                    "grafos-1", "¿Qué es un grafo?",
                    options = listOf("Vértices unidos por aristas", "Una lista ordenada de elementos", "Una tabla de dos columnas"),
                    correct = 0,
                    explanation = "Un grafo es un conjunto de **vértices** y de **aristas** que los unen.",
                )),
                exercise(MultipleChoice(
                    "grafos-2", "Marca las afirmaciones verdaderas:",
                    options = listOf(
                        "Un grafo puede tener ciclos",
                        "En un grafo, todas las aristas tienen dirección",
                        "Un árbol es un tipo de grafo",
                    ),
                    correct = setOf(0, 2),
                    explanation = "Las aristas solo tienen dirección en los grafos *dirigidos*.",
                )),
                exercise(Matching(
                    "grafos-3", "Une cada término con su significado:",
                    pairs = listOf(
                        MatchPair("Vértice", "Un nodo del grafo"),
                        MatchPair("Arista", "La unión entre dos nodos"),
                        MatchPair("Grado", "Cuántas aristas tiene un nodo"),
                    ),
                )),
            ),
            review = listOf(
                SingleChoice(
                    "grafos-r1", "En un grafo no dirigido, ¿cuánto suman los grados de todos los vértices?",
                    options = listOf("El doble de aristas", "El número de aristas", "El número de vértices"), correct = 0,
                    explanation = "Cada arista suma uno al grado de **cada uno** de sus dos extremos.",
                ),
                SingleChoice(
                    "grafos-r2", "¿Qué es un grafo dirigido?",
                    options = listOf("Sus aristas tienen sentido", "No tiene ciclos", "Tiene raíz"), correct = 0,
                ),
                MultipleChoice(
                    "grafos-r3", "Marca las representaciones de un grafo:",
                    options = listOf("Matriz de adyacencia", "Lista de adyacencia", "Pila"), correct = setOf(0, 1),
                ),
            ),
            mastery = Mastery(questions = 3, minCorrect = 2),
        ),
        Module(
            "arboles", "Árboles", requires = listOf("relaciones"),
            steps = listOf(
                exercise(MultipleChoice(
                    "arboles-1", "Marca lo que cumple todo árbol:",
                    options = listOf("Tiene una raíz", "Tiene algún ciclo", "Entre dos nodos hay un único camino"),
                    correct = setOf(0, 2),
                )),
                exercise(Matching(
                    "arboles-2", "Une cada término con su significado:",
                    pairs = listOf(
                        MatchPair("Raíz", "Nodo sin padre"),
                        MatchPair("Hoja", "Nodo sin hijos"),
                        MatchPair("Altura", "Camino más largo de la raíz a una hoja"),
                    ),
                )),
            ),
        ),
        Module(
            "hash", "Tablas hash", requires = listOf("relaciones"),
            steps = listOf(
                exercise(SingleChoice(
                    "hash-1", "¿Qué hace la función hash de una tabla hash?",
                    options = listOf("Convierte una clave en una posición", "Ordena los elementos", "Borra los repetidos"),
                    correct = 0,
                )),
                exercise(Matching(
                    "hash-2", "Une cada término con su significado:",
                    pairs = listOf(
                        MatchPair("Clave", "El dato con el que se busca"),
                        MatchPair("Colisión", "Dos claves en la misma posición"),
                        MatchPair("Factor de carga", "Elementos entre tamaño de la tabla"),
                    ),
                )),
            ),
        ),
        Module("bfs", "Búsqueda en anchura (BFS)", requires = listOf("grafos")),
        Module("dfs", "Búsqueda en profundidad (DFS)", requires = listOf("grafos", "arboles")),
        Module(
            "monticulos", "Montículos", requires = listOf("arboles"),
            steps = listOf(
                exercise(SingleChoice(
                    "monticulos-1", "En un montículo de mínimos, ¿dónde está el elemento menor?",
                    options = listOf("En la raíz", "En la última hoja", "En cualquier sitio"), correct = 0,
                )),
                exercise(MultipleChoice(
                    "monticulos-2", "Marca lo que es cierto de un montículo binario:",
                    options = listOf(
                        "Es un árbol binario casi completo",
                        "Se puede guardar en un array",
                        "Está ordenado como un árbol binario de búsqueda",
                    ),
                    correct = setOf(0, 1),
                )),
            ),
        ),
        Module("colisiones", "Colisiones", requires = listOf("hash")),
        Module("dijkstra", "Caminos mínimos", requires = listOf("bfs", "monticulos")),
    ),
)

// Lo que el estudiante de ejemplo ya domina (para las previews).
val demoMastered = setOf("conjuntos", "relaciones", "arboles")

private fun exercise(question: Question) = Step.Exercise(question)
