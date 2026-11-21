package es.ulpgc.pamn.dominio.data

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary

// Los 3 cursos de la pantalla «Buscar» del Figma, solo para previews y tests (los resultados de
// verdad llegan del backend). Sin valoraciones ni «Inscrito»: no existen todavía.
val demoSearchResults = listOf(
    CourseSummary(
        id = "estructuras-de-datos",
        version = 1,
        author = Author(id = "ana-ruiz", name = "Ana Ruiz"),
        publishedAt = "2026-10-03T08:35:00Z",
        title = "Estructuras de datos",
        description = "Conjuntos, grafos, árboles y tablas hash, paso a paso.",
        language = "es",
        tags = listOf("grafos", "árboles", "algoritmos"),
        moduleCount = 20,
    ),
    CourseSummary(
        id = "teoria-de-grafos",
        version = 1,
        author = Author(id = "jorge-medina", name = "Jorge Medina"),
        publishedAt = "2026-09-12T10:00:00Z",
        title = "Teoría de grafos para principiantes",
        description = "Vértices, aristas y caminos desde cero.",
        language = "es",
        tags = listOf("grafos", "matemáticas"),
        moduleCount = 14,
    ),
    CourseSummary(
        id = "busqueda-en-grafos",
        version = 1,
        author = Author(id = "lucia-santana", name = "Lucía Santana"),
        publishedAt = "2026-03-20T09:00:00Z",
        title = "Búsqueda en grafos: BFS y DFS",
        description = "Recorrer un grafo en anchura y en profundidad.",
        language = "es",
        tags = listOf("grafos", "algoritmos"),
        moduleCount = 11,
    ),
)
