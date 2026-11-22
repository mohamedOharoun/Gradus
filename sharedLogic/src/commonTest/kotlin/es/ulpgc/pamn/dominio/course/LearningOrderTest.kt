package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.course.http.CourseDecoding
import es.ulpgc.pamn.dominio.course.http.EXAMPLE_COURSE_RESPONSE
import es.ulpgc.pamn.dominio.course.http.decodeCourseVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// «Qué vas a aprender» en la ficha del curso: los módulos en el orden en que se ven en el camino
// (de arriba abajo y, en cada fila, de izquierda a derecha), no en el del JSON.
class LearningOrderTest {

    private fun course(vararg modules: Module) = Course("c", "Curso", modules.toList())
    private fun Course.order() = learningOrder().map { it.id }

    @Test
    fun `a chain written backwards is learnt from the first prerequisite`() {
        val chain = course(
            Module("c", "C", requires = listOf("b")),
            Module("b", "B", requires = listOf("a")),
            Module("a", "A"),
        )
        assertEquals(listOf("a", "b", "c"), chain.order())
    }

    @Test
    fun `modules of the same row go from left to right, as in the path`() {
        val fan = course(
            Module("a", "A"),
            Module("b", "B", requires = listOf("a")),
            Module("c", "C", requires = listOf("a")),
            Module("d", "D", requires = listOf("a")),
        )
        val path = fan.pathLayout()
        val order = fan.order()
        assertEquals("a", order.first())
        val xs = order.drop(1).map { path.node(it)!!.x }
        assertEquals(xs.sorted(), xs)
    }

    // Un nivel de 4 módulos se parte en dos filas (2 + 2): la lista sigue las filas.
    @Test
    fun `a level split in two rows follows the rows`() {
        val wide = course(
            Module("a", "A"),
            Module("b", "B", requires = listOf("a")),
            Module("c", "C", requires = listOf("a")),
            Module("d", "D", requires = listOf("a")),
            Module("e", "E", requires = listOf("a")),
        )
        val path = wide.pathLayout()
        val rows = wide.order().map { path.node(it)!!.row }
        assertEquals(rows.sorted(), rows)
        assertTrue(rows.last() > rows[1], "los 4 del mismo nivel ocupan dos filas")
    }

    @Test
    fun `every module appears once`() {
        val diamond = course(
            Module("a", "A"),
            Module("b", "B", requires = listOf("a")),
            Module("c", "C", requires = listOf("a")),
            Module("d", "D", requires = listOf("b", "c")),
        )
        assertEquals(setOf("a", "b", "c", "d"), diamond.order().toSet())
        assertEquals(4, diamond.order().size)
        assertEquals("d", diamond.order().last())
    }

    @Test
    fun `a course without modules has nothing to learn`() {
        assertTrue(course().learningOrder().isEmpty())
    }

    @Test
    fun `the example course of the backend starts with representing a graph`() {
        val example = (decodeCourseVersion(EXAMPLE_COURSE_RESPONSE) as CourseDecoding.Decoded).course
        assertEquals(listOf("representacion", "bfs"), example.order())
    }
}
