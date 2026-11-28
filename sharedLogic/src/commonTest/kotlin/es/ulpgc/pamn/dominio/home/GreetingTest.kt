package es.ulpgc.pamn.dominio.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// «Hola, Ana» en Inicio (docs/tareas/inicio.md, paso 3): se saluda con el primer nombre.
class GreetingTest {

    @Test
    fun `the greeting uses the first name`() {
        assertEquals("Mohamed", greetingName("Mohamed Oharoun Zarkik"))
    }

    @Test
    fun `a single name is used as it is`() {
        assertEquals("Ana", greetingName("Ana"))
    }

    // Como en las iniciales: un nombre con guion es una sola palabra.
    @Test
    fun `a hyphenated name is one word`() {
        assertEquals("María-José", greetingName("María-José Pérez"))
    }

    @Test
    fun `spaces around and between words do not matter`() {
        assertEquals("Ana", greetingName("   Ana    Ruiz "))
    }

    // Sin nombre, la pantalla dice solo «Hola».
    @Test
    fun `without a name there is no one to greet`() {
        assertNull(greetingName(""))
        assertNull(greetingName("   "))
    }
}
