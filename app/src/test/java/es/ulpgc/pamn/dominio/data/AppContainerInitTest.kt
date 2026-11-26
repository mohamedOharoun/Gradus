package es.ulpgc.pamn.dominio.data

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

    // La sesión también necesita Context (11c): se guarda en un fichero (DataStore).
    @Test
    fun session_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.sessionStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun session_updates_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.sessionUpdates }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }
}
