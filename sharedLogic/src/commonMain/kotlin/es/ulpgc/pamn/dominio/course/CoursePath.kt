package es.ulpgc.pamn.dominio.course

// Un módulo colocado en el camino. `row` cuenta desde arriba (0) y `x` va de 0 (izquierda) a 1
// (derecha). Son fracciones, no dp: la interfaz las multiplica por el ancho que tenga la pantalla.
data class PathNode(val module: Module, val row: Int, val x: Float)

// Una línea del camino: del prerrequisito `from` al módulo `to` que lo necesita.
// `via`: si la línea cruza filas intermedias, la x por la que pasa en cada una (un hueco entre
// nodos, para no pasar por detrás de ninguno). Vacía si las dos filas son seguidas.
data class PathEdge(val from: String, val to: String, val via: List<Float> = emptyList())

data class CoursePath(val nodes: List<PathNode>, val edges: List<PathEdge>) {
    private val byId: Map<String, PathNode> = nodes.associateBy { it.module.id }

    val rowCount: Int = (nodes.maxOfOrNull { it.row } ?: -1) + 1

    fun node(moduleId: String): PathNode? = byId[moduleId]
}
