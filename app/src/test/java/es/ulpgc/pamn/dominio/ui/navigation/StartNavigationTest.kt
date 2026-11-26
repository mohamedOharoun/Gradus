package es.ulpgc.pamn.dominio.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Las reglas de arranque y salida que no necesitan Android: qué ruta abre cada estado y a dónde
// salir de Main según el motivo. Navegar de verdad se prueba en GradusNavHostStartTest (instrumentado).
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
    fun `without a session it always starts in Welcome regardless of exit reason`() {
        assertEquals(Route.Welcome, StartState.SignedOut(MainExit.ToLogin).startRoute())
        assertEquals(Route.Welcome, StartState.SignedOut(MainExit.ToWelcome).startRoute())
    }

    @Test
    fun `unexpected sign out inside Main leaves to Login`() {
        assertEquals(MainExit.ToLogin, StartState.SignedOut(MainExit.ToLogin).mainExit(inMain = true))
    }

    @Test
    fun `voluntary sign out inside Main leaves to Welcome`() {
        assertEquals(MainExit.ToWelcome, StartState.SignedOut(MainExit.ToWelcome).mainExit(inMain = true))
    }

    @Test
    fun `without a session outside Main there is nothing to leave`() {
        assertNull(StartState.SignedOut(MainExit.ToLogin).mainExit(inMain = false))
        assertNull(StartState.SignedOut(MainExit.ToWelcome).mainExit(inMain = false))
    }

    @Test
    fun `with a session it never leaves Main`() {
        assertNull(StartState.SignedIn.mainExit(inMain = true))
        assertNull(StartState.SignedIn.mainExit(inMain = false))
    }

    // Una Main restaurada no se abandona mientras aún se está leyendo la sesión.
    @Test
    fun `while loading it does not leave Main`() {
        assertNull(StartState.Loading.mainExit(inMain = true))
        assertNull(StartState.Loading.mainExit(inMain = false))
    }
}
