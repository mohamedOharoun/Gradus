package es.ulpgc.pamn.dominio.course

import kotlin.math.abs

// Coloca los módulos de un curso como un camino de arriba abajo. Es una función pura: el mismo
// curso da siempre el mismo camino. No depende de lo dominado (eso solo cambia los colores).
//
// 1. Nivel: 0 sin prerrequisitos; si no, uno más que su prerrequisito más profundo.
// 2. Orden dentro del nivel: según la posición media de sus prerrequisitos, para que las líneas
//    se crucen poco (heurística del baricentro). El primer nivel sigue el orden del curso.
// 3. Filas: un nivel con más de MAX_PER_ROW módulos se parte en varias filas equilibradas
//    (4 = 2 + 2, 5 = 3 + 2), porque en un móvil no caben más de 3 nodos a lo ancho.
// 4. Posición x: cada módulo en el centro de su parte del ancho; si la fila tiene uno solo,
//    zigzag como en Duolingo.
// 5. Líneas: si una cruza filas intermedias, pasa por el hueco libre más cercano de cada una.
fun Course.pathLayout(): CoursePath {
    val levels = levelsOf(this)
    val byLevel = modules.groupBy { levels.getValue(it.id) }
    val xs = mutableMapOf<String, Float>()
    var nextRow = 0
    // Los niveles, de arriba abajo. (`toSortedMap` solo existe en la JVM: en commonMain no se puede usar.)
    val nodes = byLevel.keys.sorted().flatMap { level ->
        byLevel.getValue(level)
            .sortedByPrerequisites(xs)
            .balancedChunks(MAX_PER_ROW)
            .flatMap { row -> placeRow(nextRow++, row).onEach { xs[it.module.id] = it.x } }
    }
    val placed = CoursePath(nodes, edges = emptyList())
    val nodeXsByRow = nodes.groupBy({ it.row }, { it.x })
    val edges = modules.flatMap { module ->
        module.requires.filter { module(it) != null }.map { from ->
            PathEdge(from = from, to = module.id, via = waypoints(placed.node(from)!!, placed.node(module.id)!!, nodeXsByRow))
        }
    }
    return CoursePath(nodes, edges)
}

// Distancia mínima (fracción del ancho) entre una línea y el centro de un nodo: medio nodo en un
// móvil (52 dp de 328). Más cerca, la línea pasaría por detrás del nodo o de su título.
private const val CLEARANCE = 0.17f

// Para cada fila entre `from` y `to`: la x de la recta entre los dos si está libre; si no, el
// hueco libre más cercano (el punto medio entre dos nodos, o entre un nodo y el borde).
private fun waypoints(from: PathNode, to: PathNode, nodeXsByRow: Map<Int, List<Float>>): List<Float> =
    ((from.row + 1) until to.row).map { row ->
        val progress = (row - from.row).toFloat() / (to.row - from.row)
        val straight = from.x + (to.x - from.x) * progress
        freeX(straight, nodeXsByRow[row].orEmpty().sorted())
    }

private fun freeX(ideal: Float, nodeXs: List<Float>): Float {
    fun isFree(x: Float) = nodeXs.all { abs(it - x) >= CLEARANCE }
    if (isFree(ideal)) return ideal
    val gaps = (listOf(0f) + nodeXs + 1f).zipWithNext { left, right -> (left + right) / 2 }
    return gaps.filter(::isFree).minByOrNull { abs(it - ideal) } ?: ideal
}

private const val MAX_PER_ROW = 3

// Por la x media de sus prerrequisitos (ya colocados en `placedXs`). `sortedBy` es estable: si
// empatan, se respeta el orden del curso.
private fun List<Module>.sortedByPrerequisites(placedXs: Map<String, Float>): List<Module> =
    sortedBy { module -> module.requires.mapNotNull { placedXs[it] }.averageOrZero() }

// Trozos de como mucho `max`, lo más iguales posible: 4 → [2, 2] y no [3, 1].
private fun <T> List<T>.balancedChunks(max: Int): List<List<T>> {
    if (isEmpty()) return emptyList()
    val rows = (size + max - 1) / max
    return chunked((size + rows - 1) / rows)
}

private fun placeRow(row: Int, modules: List<Module>): List<PathNode> =
    modules.mapIndexed { index, module -> PathNode(module, row, xFor(row, index, modules.size)) }

// Centro, derecha, centro, izquierda… (fracciones del ancho).
private val ZIGZAG = listOf(0.5f, 0.7f, 0.5f, 0.3f)

private fun xFor(row: Int, index: Int, count: Int): Float =
    if (count == 1) ZIGZAG[row % ZIGZAG.size] else (index + 0.5f) / count

private fun List<Float>.averageOrZero(): Float = if (isEmpty()) 0f else sum() / size

// El nivel de cada módulo, recordando las ya calculadas (memoización). `visiting` detecta ciclos.
private fun levelsOf(course: Course): Map<String, Int> {
    val levels = mutableMapOf<String, Int>()
    val visiting = mutableSetOf<String>()

    fun levelOf(id: String): Int = levels[id] ?: run {
        require(visiting.add(id)) { "El curso tiene un ciclo en el módulo «$id»." }
        val prerequisites = course.module(id)?.requires.orEmpty().filter { course.module(it) != null }
        val level = prerequisites.maxOfOrNull { levelOf(it) + 1 } ?: 0
        visiting.remove(id)
        level.also { levels[id] = it }
    }

    course.modules.forEach { levelOf(it.id) }
    return levels
}
