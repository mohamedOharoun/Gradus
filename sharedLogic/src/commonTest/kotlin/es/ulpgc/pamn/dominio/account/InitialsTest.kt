package es.ulpgc.pamn.dominio.account

import kotlin.test.Test
import kotlin.test.assertEquals

// Las iniciales del avatar de Ajustes (Figma: «MO» en un círculo).
class InitialsTest {

    @Test
    fun `two words give the first letter of each`() {
        assertEquals("MO", initialsOf("Mohamed Oharoun"))
    }

    @Test
    fun `with more than two words, the first and the last`() {
        assertEquals("MZ", initialsOf("Mohamed Oharoun Zarkik"))
    }

    @Test
    fun `a single word gives one letter`() {
        assertEquals("A", initialsOf("Ana"))
    }

    @Test
    fun `they are always capital letters, accents included`() {
        assertEquals("ÁN", initialsOf("álvaro núñez"))
    }

    @Test
    fun `extra spaces do not count as words`() {
        assertEquals("AR", initialsOf("  Ana   Ruiz  "))
    }

    // Un nombre compuesto con guion es una sola palabra.
    @Test
    fun `a hyphenated name is one word`() {
        assertEquals("ML", initialsOf("María-José López"))
    }

    @Test
    fun `without a name there is a question mark`() {
        assertEquals("?", initialsOf(""))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun `an account knows its initials`() {
        assertEquals("AR", Account("u1", "Ana Ruiz", "ana@ulpgc.es", "2026-11-17T10:00:00Z").initials)
    }
}
