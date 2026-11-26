package es.ulpgc.pamn.dominio.ui.screens.settings

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.settings.AppLanguage
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

// Cómo se llama cada idioma en Ajustes (fase 3, tarea 5b). Con UiText (decisión U), el test comprueba
// qué texto es; la frase en cada idioma está en values/ y values-en/.
class LanguageTextsTest {

    @Test
    fun `each language has its own name`() {
        assertEquals(UiText.of(R.string.language_system), AppLanguage.SYSTEM.label())
        assertEquals(UiText.of(R.string.language_spanish), AppLanguage.SPANISH.label())
        assertEquals(UiText.of(R.string.language_english), AppLanguage.ENGLISH.label())
    }

    @Test
    fun `no two languages share a name`() {
        assertEquals(AppLanguage.entries.size, AppLanguage.entries.map { it.label() }.toSet().size)
    }
}
