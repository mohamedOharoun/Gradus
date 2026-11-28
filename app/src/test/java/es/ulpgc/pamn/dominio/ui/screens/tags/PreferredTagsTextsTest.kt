package es.ulpgc.pamn.dominio.ui.screens.tags

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.settings.PreferredTags
import es.ulpgc.pamn.dominio.settings.TagAddition
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Los textos de «Etiquetas preferidas» (fase 3, tarea 3; con UiText desde el paso 7, decisión U).
class PreferredTagsTextsTest {

    @Test
    fun `each problem has its message`() {
        assertEquals(UiText.of(R.string.tags_error_empty), TagAddition.Empty.message())
        assertEquals(UiText.of(R.string.tags_error_already_added), TagAddition.AlreadyAdded.message())
    }

    // El máximo sale de la regla (PreferredTags.MAX), no del texto.
    @Test
    fun `full says the maximum`() {
        assertEquals(UiText.of(R.string.tags_error_full, PreferredTags.MAX), TagAddition.Full.message())
    }

    @Test
    fun `added has no message`() {
        assertNull(TagAddition.Added(PreferredTags()).message())
    }

    @Test
    fun `the count says how many of the maximum`() {
        assertEquals(UiText.of(R.string.tags_count, 0, PreferredTags.MAX), countText(0))
        assertEquals(UiText.of(R.string.tags_count, 3, PreferredTags.MAX), countText(3))
    }

    // Como en los cursos y en Buscar: «#grafos». No depende del idioma: sigue siendo un String.
    @Test
    fun `a tag is shown with a hash`() {
        assertEquals("#bases de datos", tagLabel("bases de datos"))
    }

    // TalkBack lee el chip entero como un botón que la quita, con su nombre.
    @Test
    fun `removing a tag is read with its name`() {
        assertEquals(UiText.of(R.string.tags_remove, "grafos"), removeTagDescription("grafos"))
    }
}
