package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// «A, B, C» sin «y» (docs/tareas/ajustes.md, paso 9a): lo que lee TalkBack de una tarjeta de curso.
// joinedWithAnd se prueba en CoursePathTextsTest.
class ListTextsTest {

    @Test
    fun `nothing joins into an empty text`() {
        assertEquals(UiText.raw(""), joinedWithCommas(emptyList()))
    }

    // Lo que escribe el autor va tal cual; un UiText se queda como está.
    @Test
    fun `a single item is itself`() {
        assertEquals(UiText.raw("Grafos"), joinedWithCommas(listOf("Grafos")))
        assertEquals(UiText.of(R.string.detail_by_author, "Ana"), joinedWithCommas(listOf(UiText.of(R.string.detail_by_author, "Ana"))))
    }

    @Test
    fun `two items are joined with the comma of each language`() {
        assertEquals(UiText.of(R.string.list_comma, "Grafos", "Árboles"), joinedWithCommas(listOf("Grafos", "Árboles")))
    }

    // Se anidan en orden: «(A, B), C».
    @Test
    fun `three items keep their order`() {
        assertEquals(
            UiText.of(R.string.list_comma, UiText.of(R.string.list_comma, "A", "B"), "C"),
            joinedWithCommas(listOf("A", "B", "C")),
        )
    }
}
