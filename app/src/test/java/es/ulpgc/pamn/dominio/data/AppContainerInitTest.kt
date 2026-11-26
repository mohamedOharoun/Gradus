package es.ulpgc.pamn.dominio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// En la JVM nadie llama a AppContainer.init (no hay GradusApplication): sirve para comprobar qué
// pasa si se usa el progreso antes de tiempo. Estos tests no deben llamar nunca a init.
class AppContainerInitTest {

    @Test
    fun progress_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.progressStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun enrollment_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.enrollmentStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    // Solo el progreso necesita Context: la sesión no.
    @Test
    fun session_store_works_without_init() {
        assertNotNull(AppContainer.sessionStore)
        assertEquals(AppContainer.sessionStore, AppContainer.sessionStore)
    }
}
