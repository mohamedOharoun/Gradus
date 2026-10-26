package es.ulpgc.pamn.dominio.data

import es.ulpgc.pamn.dominio.model.ConceptoUi
import es.ulpgc.pamn.dominio.model.Estado

val conceptosDemo = listOf(
    ConceptoUi("conjuntos", "Conjuntos", Estado.DOMINADO, 1f),
    ConceptoUi("relaciones", "Relaciones", Estado.DOMINADO, 1f),
    ConceptoUi("grafos", "Grafos", Estado.FRONTERA, 0.6f),
    ConceptoUi("arboles", "Árboles", Estado.FRONTERA, 0.2f),
    ConceptoUi("bfs", "Búsqueda en anchura (BFS)", Estado.BLOQUEADO, 0f),
    ConceptoUi("dfs", "Búsqueda en profundidad (DFS)", Estado.BLOQUEADO, 0f),
)