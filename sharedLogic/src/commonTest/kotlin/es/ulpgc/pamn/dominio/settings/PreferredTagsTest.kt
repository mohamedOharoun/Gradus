package es.ulpgc.pamn.dominio.settings

import kotlin.test.Test
import kotlin.test.assertEquals

// Las etiquetas preferidas de Ajustes (docs/tareas/ajustes.md, fase 3, decisión M): se guardan como
// las de los cursos (minúsculas, sin «#» ni espacios de sobra), sin repetir y como mucho 10.
class PreferredTagsTest {

    private val none = PreferredTags()

    private fun PreferredTags.added(raw: String): PreferredTags = (add(raw) as TagAddition.Added).tags

    // ── Añadir ──

    @Test
    fun `a tag is added`() {
        assertEquals(listOf("grafos"), none.added("grafos").values)
    }

    @Test
    fun `they keep the order in which they were added`() {
        assertEquals(listOf("grafos", "kotlin"), none.added("grafos").added("kotlin").values)
    }

    @Test
    fun `it is saved like the tags of the courses`() {
        assertEquals(listOf("bases de datos"), none.added("  Bases   de  DATOS ").values)
    }

    // Se puede escribir como se ve en Ajustes («#grafos»).
    @Test
    fun `the hash in front is not part of the tag`() {
        assertEquals(listOf("grafos"), none.added("#grafos").values)
        assertEquals(listOf("grafos"), none.added(" ## grafos").values)
    }

    // ── Lo que no se añade ──

    @Test
    fun `an empty tag is not added`() {
        listOf("", "   ", "#", " # ").forEach { assertEquals(TagAddition.Empty, none.add(it), "«$it»") }
    }

    @Test
    fun `a tag that is already there is not added twice`() {
        assertEquals(TagAddition.AlreadyAdded, none.added("grafos").add(" #GRAFOS "))
    }

    @Test
    fun `there can be at most ten`() {
        val full = (1..PreferredTags.MAX).fold(none) { tags, n -> tags.added("tema$n") }
        assertEquals(PreferredTags.MAX, full.values.size)
        assertEquals(TagAddition.Full, full.add("otra"))
    }

    // ── Quitar ──

    @Test
    fun `a tag is removed and the rest keep their order`() {
        val three = none.added("grafos").added("kotlin").added("algoritmos")
        assertEquals(listOf("grafos", "algoritmos"), three.remove("kotlin").values)
    }

    @Test
    fun `removing a tag that is not there changes nothing`() {
        val one = none.added("grafos")
        assertEquals(one, one.remove("kotlin"))
    }

    // ── Al leerlas de lo guardado ──

    // Lo guardado se vuelve a pasar por las reglas: si se toca a mano o cambian, no entra basura.
    @Test
    fun `saved tags are tidied when read`() {
        assertEquals(listOf("grafos", "kotlin"), PreferredTags.of(listOf(" Grafos", "#grafos", "", "kotlin")).values)
    }

    @Test
    fun `saved tags beyond ten are dropped`() {
        assertEquals(PreferredTags.MAX, PreferredTags.of((1..15).map { "tema$it" }).values.size)
    }
}
