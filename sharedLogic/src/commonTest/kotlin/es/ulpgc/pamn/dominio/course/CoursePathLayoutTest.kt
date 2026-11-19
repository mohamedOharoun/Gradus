package es.ulpgc.pamn.dominio.course

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Dónde va cada concepto del camino: fila (de arriba abajo) y posición horizontal (de 0 a 1).
class CoursePathLayoutTest {

    private fun course(vararg concepts: Concept) = Course("c", "Curso", concepts.toList())

    private fun CoursePath.rowOf(id: String) = node(id)!!.row
    private fun CoursePath.xOf(id: String) = node(id)!!.x

    // ── Filas ──

    @Test
    fun `concepts without prerequisites go in the first row`() {
        val path = course(Concept("a", "A"), Concept("b", "B")).pathLayout()
        assertEquals(0, path.rowOf("a"))
        assertEquals(0, path.rowOf("b"))
    }

    @Test
    fun `in a chain each concept goes one row below the previous one`() {
        val path = course(
            Concept("a", "A"),
            Concept("b", "B", requires = listOf("a")),
            Concept("c", "C", requires = listOf("b")),
        ).pathLayout()
        assertEquals(listOf(0, 1, 2), listOf("a", "b", "c").map { path.rowOf(it) })
        assertEquals(3, path.rowCount)
    }

    // Con prerrequisitos a distintas alturas, manda el más lejano: así nunca queda por encima de uno.
    @Test
    fun `a concept goes one row below its deepest prerequisite`() {
        val path = course(
            Concept("a", "A"),
            Concept("b", "B", requires = listOf("a")),
            Concept("c", "C", requires = listOf("a", "b")),
        ).pathLayout()
        assertEquals(2, path.rowOf("c"))
    }

    @Test
    fun `a diamond has the two branches in the same row`() {
        val path = diamond.pathLayout()
        assertEquals(listOf(0, 1, 1, 2), listOf("a", "b", "c", "d").map { path.rowOf(it) })
    }

    @Test
    fun `the order of the concepts in the course does not change the rows`() {
        val reversed = Course("c", "Curso", diamond.concepts.reversed())
        assertEquals(diamond.pathLayout().nodes.associate { it.concept.id to it.row },
            reversed.pathLayout().nodes.associate { it.concept.id to it.row })
    }

    // ── Posición horizontal ──

    // Cada concepto, en el centro de su parte del ancho: con 3, al 1/6, 3/6 y 5/6.
    // Así caben 3 nodos sin que choquen sus títulos.
    @Test
    fun `several concepts of a row are spread evenly`() {
        val path = course(Concept("a", "A"), Concept("b", "B"), Concept("c", "C")).pathLayout()
        listOf("a" to 1f / 6, "b" to 0.5f, "c" to 5f / 6).forEach { (id, x) ->
            assertEquals(x, path.xOf(id), absoluteTolerance = 0.0001f, message = id)
        }
    }

    // ── Niveles anchos: como mucho 3 por fila ──

    private fun wide(count: Int) = course(*Array(count) { Concept("c$it", "C$it") })
    private fun CoursePath.rowSizes() = nodes.groupBy { it.row }.toList().sortedBy { it.first }.map { it.second.size }

    @Test
    fun `a level with up to 3 concepts stays in one row`() {
        assertEquals(listOf(3), wide(3).pathLayout().rowSizes())
    }

    @Test
    fun `a level with 4 concepts is split in two rows of 2`() {
        assertEquals(listOf(2, 2), wide(4).pathLayout().rowSizes())
    }

    @Test
    fun `a level with 5 concepts is split in rows of 3 and 2`() {
        assertEquals(listOf(3, 2), wide(5).pathLayout().rowSizes())
    }

    @Test
    fun `a level with 7 concepts never has more than 3 in a row`() {
        assertTrue(wide(7).pathLayout().rowSizes().all { it <= 3 })
    }

    @Test
    fun `after a split level, the next level goes below all of its rows`() {
        val concepts = (0 until 4).map { Concept("top$it", "Arriba $it") } +
            Concept("bottom", "Abajo", requires = listOf("top0"))
        val path = course(*concepts.toTypedArray()).pathLayout()
        assertEquals(2, path.rowOf("bottom"))
    }

    // El zigzag de Duolingo: centro, derecha, centro, izquierda… en las filas de un solo concepto.
    @Test
    fun `rows with a single concept zigzag`() {
        val chain = course(
            Concept("a", "A"),
            Concept("b", "B", requires = listOf("a")),
            Concept("c", "C", requires = listOf("b")),
            Concept("d", "D", requires = listOf("c")),
            Concept("e", "E", requires = listOf("d")),
        ).pathLayout()
        assertEquals(listOf(0.5f, 0.7f, 0.5f, 0.3f, 0.5f), listOf("a", "b", "c", "d", "e").map { chain.xOf(it) })
    }

    // Cada concepto se coloca cerca de sus prerrequisitos para que las líneas se crucen lo menos posible.
    @Test
    fun `a row is ordered by where its prerequisites are, not by the order of the course`() {
        val path = course(
            Concept("left", "Izquierda"),
            Concept("right", "Derecha"),
            Concept("underRight", "Bajo derecha", requires = listOf("right")),
            Concept("underLeft", "Bajo izquierda", requires = listOf("left")),
        ).pathLayout()
        assertTrue(path.xOf("underLeft") < path.xOf("underRight"))
    }

    // ── Líneas ──

    @Test
    fun `there is a line from each prerequisite to the concept that needs it`() {
        assertEquals(
            setOf("a" to "b", "a" to "c", "b" to "d", "c" to "d"),
            diamond.pathLayout().edges.map { it.from to it.to }.toSet(),
        )
    }

    // ── Líneas largas: las que cruzan filas pasan por un hueco, nunca por detrás de un nodo ──

    @Test
    fun `a line between consecutive rows has no waypoints`() {
        assertTrue(diamond.pathLayout().edges.all { it.via.isEmpty() })
    }

    // a (fila 0, x 0.5) → c (fila 2, x 0.5) cruza la fila 1, donde b está en 0.7: no choca.
    @Test
    fun `a long line that does not hit any node goes straight`() {
        val path = course(
            Concept("a", "A"),
            Concept("b", "B", requires = listOf("a")),
            Concept("c", "C", requires = listOf("a", "b")),
        ).pathLayout()
        val long = path.edges.single { it.from == "a" && it.to == "c" }
        assertEquals(1, long.via.size)
        assertEquals(0.5f, long.via.single(), absoluteTolerance = 0.0001f)
    }

    // r (fila 0, x 0.75) → y (fila 2, x 0.5): en la fila 1 pasaría por 0.625, encima de x (0.7).
    // El hueco libre más cercano es el del medio entre el borde izquierdo y x: 0.35.
    @Test
    fun `a long line that would hit a node goes through the nearest gap`() {
        val path = course(
            Concept("l", "L"),
            Concept("r", "R"),
            Concept("x", "X", requires = listOf("l")),
            Concept("y", "Y", requires = listOf("r", "x")),
        ).pathLayout()
        val long = path.edges.single { it.from == "r" && it.to == "y" }
        assertEquals(0.35f, long.via.single(), absoluteTolerance = 0.0001f)
    }

    @Test
    fun `a long line has one waypoint per row it crosses`() {
        val path = course(
            Concept("a", "A"),
            Concept("b", "B", requires = listOf("a")),
            Concept("c", "C", requires = listOf("b")),
            Concept("d", "D", requires = listOf("a", "c")),
        ).pathLayout()
        assertEquals(2, path.edges.single { it.from == "a" && it.to == "d" }.via.size)
    }

    @Test
    fun `no waypoint of the example course is on top of a node`() {
        val path = example.pathLayout()
        path.edges.forEach { edge ->
            val from = path.node(edge.from)!!
            edge.via.forEachIndexed { index, x ->
                val row = from.row + 1 + index
                path.nodes.filter { it.row == row }.forEach { node ->
                    assertTrue(kotlin.math.abs(node.x - x) >= 0.17f, "$edge choca con ${node.concept.id}")
                }
            }
        }
    }

    @Test
    fun `a prerequisite that does not exist draws no line`() {
        val path = course(Concept("a", "A", requires = listOf("ghost"))).pathLayout()
        assertTrue(path.edges.isEmpty())
        assertEquals(0, path.rowOf("a"))
    }

    // ── Curso de ejemplo y casos raros ──

    @Test
    fun `no concept is above or beside any of its prerequisites`() {
        val path = example.pathLayout()
        path.edges.forEach { assertTrue(path.rowOf(it.from) < path.rowOf(it.to), "$it") }
    }

    @Test
    fun `every concept of the course is in the path, once`() {
        assertEquals(example.concepts.map { it.id }.sorted(), example.pathLayout().nodes.map { it.concept.id }.sorted())
    }

    @Test
    fun `an empty course has an empty path`() {
        val path = course().pathLayout()
        assertTrue(path.nodes.isEmpty())
        assertEquals(0, path.rowCount)
    }

    // El validador (§6) no deja importar cursos con ciclos; si llegara uno, mejor fallar claro que colgarse.
    @Test
    fun `a cycle is rejected`() {
        val cyclic = course(Concept("a", "A", requires = listOf("b")), Concept("b", "B", requires = listOf("a")))
        assertFailsWith<IllegalArgumentException> { cyclic.pathLayout() }
    }

    private companion object {
        // A → B, A → C, B y C → D.
        val diamond = Course(
            "d", "Rombo",
            listOf(
                Concept("a", "A"),
                Concept("b", "B", requires = listOf("a")),
                Concept("c", "C", requires = listOf("a")),
                Concept("d", "D", requires = listOf("b", "c")),
            ),
        )

        // Como el curso de ejemplo de la app: zigzag, un nivel de 3 y otro de 4 (que se parte).
        val example = Course(
            "ed", "Estructuras de datos",
            listOf(
                Concept("conjuntos", "Conjuntos"),
                Concept("relaciones", "Relaciones", requires = listOf("conjuntos")),
                Concept("grafos", "Grafos", requires = listOf("relaciones")),
                Concept("arboles", "Árboles", requires = listOf("relaciones")),
                Concept("hash", "Tablas hash", requires = listOf("relaciones")),
                Concept("bfs", "BFS", requires = listOf("grafos")),
                Concept("dfs", "DFS", requires = listOf("grafos", "arboles")),
                Concept("monticulos", "Montículos", requires = listOf("arboles")),
                Concept("colisiones", "Colisiones", requires = listOf("hash")),
                Concept("dijkstra", "Caminos mínimos", requires = listOf("bfs", "monticulos")),
            ),
        )
    }
}
