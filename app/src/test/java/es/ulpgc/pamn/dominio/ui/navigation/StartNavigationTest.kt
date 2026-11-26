package es.ulpgc.pamn.dominio.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Las reglas de arranque que no necesitan Android: qué ruta abre cada estado y cuándo hay que salir de
// Main. Sacar la pila de Main y navegar de verdad se prueba en GradusNavHostStartTest (instrumentado).
class StartNavigationTest {

    @Test
    fun `while loading there is no start route`() {
        assertNull(StartState.Loading.startRoute())
    }

    @Test
    fun `with a session it starts in Main`() {
        assertEquals(Route.Main, StartState.SignedIn.startRoute())
    }

    @Test
    fun `without a session it starts in Welcome`() {
        assertEquals(Route.Welcome, StartState.SignedOut.startRoute())
    }

    @Test
    fun `without a session inside Main it must leave Main`() {
        assertTrue(StartState.SignedOut.mustLeaveMain(inMain = true))
    }

    @Test
    fun `without a session outside Main there is nothing to leave`() {
        assertFalse(StartState.SignedOut.mustLeaveMain(inMain = false))
    }

    @Test
    fun `with a session it never leaves Main`() {
        assertFalse(StartState.SignedIn.mustLeaveMain(inMain = true))
        assertFalse(StartState.SignedIn.mustLeaveMain(inMain = false))
    }

    // Una Main restaurada no se abandona mientras aún se está leyendo la sesión.
    @Test
    fun `while loading it does not leave Main`() {
        assertFalse(StartState.Loading.mustLeaveMain(inMain = true))
        assertFalse(StartState.Loading.mustLeaveMain(inMain = false))
    }
}
