package es.ulpgc.pamn.dominio.settings

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary
import kotlin.test.Test
import kotlin.test.assertEquals

// Poner antes los cursos con alguna etiqueta preferida (fase 3, decisión N). Lo usará Buscar (del
// compañero); aquí solo la regla.
class PrioritizingTest {

    private fun course(id: String, vararg tags: String) = CourseSummary(
        id = id, version = 1, author = Author("a1", "Ana"), publishedAt = "2026-11-17T10:00:00Z",
        title = id, description = "", language = "es", tags = tags.toList(), moduleCount = 1,
    )

    private val redes = course("redes", "redes")
    private val grafos = course("grafos", "Grafos", "algoritmos")
    private val sql = course("sql", "bases de datos")
    private val kotlin = course("kotlin", "kotlin")

    private val byDate = listOf(redes, grafos, sql, kotlin)

    @Test
    fun `courses with a preferred tag go first`() {
        assertEquals(listOf(grafos, kotlin, redes, sql), byDate.prioritizing(PreferredTags.of(listOf("grafos", "kotlin"))))
    }

    // El orden de antes (p. ej. por fecha) se respeta dentro de cada grupo.
    @Test
    fun `within each group the previous order is kept`() {
        assertEquals(listOf(grafos, kotlin, redes, sql), byDate.prioritizing(PreferredTags.of(listOf("kotlin", "grafos"))))
    }

    // Las etiquetas de los cursos vienen como las escribió su autor («Grafos»).
    @Test
    fun `tags are compared like in search`() {
        assertEquals(grafos, byDate.prioritizing(PreferredTags.of(listOf("GRAFOS"))).first())
    }

    @Test
    fun `without preferred tags nothing changes`() {
        assertEquals(byDate, byDate.prioritizing(PreferredTags()))
    }

    @Test
    fun `if no course has them nothing changes`() {
        assertEquals(byDate, byDate.prioritizing(PreferredTags.of(listOf("física"))))
    }
}
