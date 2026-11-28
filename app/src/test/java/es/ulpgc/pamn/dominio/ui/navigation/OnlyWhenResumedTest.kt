package es.ulpgc.pamn.dominio.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

// onlyWhenResumed: como dropUnlessResumed, pero para acciones que reciben un valor (el id del curso).
// Al navegar, la pantalla de origen deja de estar RESUMED: un segundo toque rápido ya no navega.
class OnlyWhenResumedTest {

    // Un dueño de ciclo de vida de mentira: su estado se cambia a mano.
    // createUnsafe no comprueba que se use desde el hilo principal (en estos tests no lo hay).
    private class FakeOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }

    private fun ownerIn(state: Lifecycle.State) = FakeOwner().apply { lifecycle.currentState = state }

    @Test
    fun `runs the action with its value when resumed`() {
        val received = mutableListOf<String>()
        val action = ownerIn(Lifecycle.State.RESUMED).onlyWhenResumed<String> { received += it }
        action("estructuras-de-datos")
        assertEquals(listOf("estructuras-de-datos"), received)
    }

    @Test
    fun `drops the action when only started`() {
        val received = mutableListOf<String>()
        val action = ownerIn(Lifecycle.State.STARTED).onlyWhenResumed<String> { received += it }
        action("estructuras-de-datos")
        assertEquals(emptyList<String>(), received)
    }

    @Test
    fun `drops the action when only created`() {
        val received = mutableListOf<String>()
        val action = ownerIn(Lifecycle.State.CREATED).onlyWhenResumed<String> { received += it }
        action("estructuras-de-datos")
        assertEquals(emptyList<String>(), received)
    }

    @Test
    fun `looks at the state when called, not when created`() {
        val owner = ownerIn(Lifecycle.State.STARTED)
        val received = mutableListOf<String>()
        val action = owner.onlyWhenResumed<String> { received += it }
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        action("estructuras-de-datos")
        assertEquals(listOf("estructuras-de-datos"), received)
    }

    @Test
    fun `a second quick tap is dropped once the screen stops being resumed`() {
        val owner = ownerIn(Lifecycle.State.RESUMED)
        val received = mutableListOf<String>()
        // La primera vez «navega»: la pantalla pasa a STARTED, como al abrir otra encima.
        val action = owner.onlyWhenResumed<String> {
            received += it
            owner.lifecycle.currentState = Lifecycle.State.STARTED
        }
        action("estructuras-de-datos")
        action("estructuras-de-datos")
        assertEquals(listOf("estructuras-de-datos"), received)
    }
}
