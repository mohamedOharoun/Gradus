package es.ulpgc.pamn.dominio.notes

import kotlin.test.Test
import kotlin.test.assertEquals

// El pie de una nota (docs/tareas/notas-camara.md, decisión Y): opcional y de hasta 100 caracteres.
class NoteCaptionTest {

    @Test
    fun `a caption is kept without spaces of more`() {
        assertEquals(CaptionCheck.Valid("Dijkstra paso a paso"), checkCaption("  Dijkstra   paso a  paso "))
    }

    // Opcional: vacío (o solo espacios) es «sin pie».
    @Test
    fun `an empty caption is no caption`() {
        assertEquals(CaptionCheck.Valid(null), checkCaption(""))
        assertEquals(CaptionCheck.Valid(null), checkCaption("   \n "))
    }

    @Test
    fun `up to one hundred characters it is valid`() {
        val longest = "a".repeat(MAX_CAPTION_LENGTH)
        assertEquals(CaptionCheck.Valid(longest), checkCaption(longest))
    }

    @Test
    fun `more than one hundred characters is too long`() {
        assertEquals(CaptionCheck.TooLong, checkCaption("a".repeat(MAX_CAPTION_LENGTH + 1)))
    }

    // Se cuentan caracteres, no unidades de Java: un emoji es uno.
    @Test
    fun `an emoji counts as one character`() {
        val withEmoji = "a".repeat(MAX_CAPTION_LENGTH - 1) + "📷"
        assertEquals(CaptionCheck.Valid(withEmoji), checkCaption(withEmoji))
    }
}
