package es.ulpgc.pamn.dominio.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// El idioma de la app (docs/tareas/ajustes.md, fase 3, decisiones O, S y V): Español, English o el del
// sistema. Android lo guarda como etiquetas de idioma («es», «en-US»…); vacío = el del sistema.
class AppLanguageTest {

    // En este orden se ofrecen en Ajustes.
    @Test
    fun `the options are the system one, spanish and english`() {
        assertEquals(listOf(AppLanguage.SYSTEM, AppLanguage.SPANISH, AppLanguage.ENGLISH), AppLanguage.entries)
    }

    @Test
    fun `each language has its tag, and the system one has none`() {
        assertNull(AppLanguage.SYSTEM.tag)
        assertEquals("es", AppLanguage.SPANISH.tag)
        assertEquals("en", AppLanguage.ENGLISH.tag)
    }

    @Test
    fun `without tags it is the system one`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
    }

    @Test
    fun `a tag is read by its language, whatever its country`() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTags("es"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTags("es-ES"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-US"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("EN-gb"))
    }

    // Android puede guardar varios, separados por comas: cuenta el primero.
    @Test
    fun `with several tags the first one counts`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-US,es-ES"))
    }

    // Uno que la app no tiene (lo eligió en los ajustes del sistema): la app sigue al sistema.
    @Test
    fun `a language the app does not have is the system one`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("fr-FR"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("  "))
    }
}
