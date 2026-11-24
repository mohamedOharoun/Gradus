package es.ulpgc.pamn.dominio.ui.screens.tags

import es.ulpgc.pamn.dominio.settings.TagAddition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Los textos de «Etiquetas preferidas» (fase 3, tarea 3).
class PreferredTagsTextsTest {

    @Test
    fun `each problem has its message`() {
        assertEquals("Escribe una etiqueta", TagAddition.Empty.message())
        assertEquals("Ya tienes esa etiqueta", TagAddition.AlreadyAdded.message())
        assertEquals("Puedes tener 10 como mucho. Quita una para añadir otra.", TagAddition.Full.message())
    }

    @Test
    fun `added has no message`() {
        assertNull(TagAddition.Added(es.ulpgc.pamn.dominio.settings.PreferredTags()).message())
    }

    @Test
    fun `the count says how many of the maximum`() {
        assertEquals("0 de 10", countText(0))
        assertEquals("3 de 10", countText(3))
    }

    // Como en los cursos y en Buscar: «#grafos».
    @Test
    fun `a tag is shown with a hash`() {
        assertEquals("#bases de datos", tagLabel("bases de datos"))
    }

    // TalkBack lee el chip entero como un botón que la quita.
    @Test
    fun `removing a tag is read with its name`() {
        assertEquals("Quitar la etiqueta grafos", removeTagDescription("grafos"))
    }
}
