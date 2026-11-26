package es.ulpgc.pamn.dominio.data.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.data.progress.ANA
import es.ulpgc.pamn.dominio.data.progress.BETO
import es.ulpgc.pamn.dominio.data.progress.sessionOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.security.ProviderException

// Fallos del sistema al leer la sesión (Keystore ocupado, KeyStore.load): salen tal cual, no son «sin
// sesión» ni corrupción. 11c decide qué hace el arranque; aquí se comprueba qué ve quien lee y guarda.
// Todo con fichero, alias y ámbito de prueba (SessionStoreRule): nunca la sesión real de la app.
@RunWith(AndroidJUnit4::class)
class DataStoreSessionStoreFailureTest {

    @get:Rule
    val rule = SessionStoreRule()

    // Cifrado real con la clave de prueba, que falla al descifrar mientras `failure` no sea null.
    private inner class FlakyCipher(var failure: Throwable? = null) : SessionCipher {
        private val real = KeystoreSessionCipher(rule.alias)
        override fun encrypt(plain: ByteArray): ByteArray = real.encrypt(plain)
        override fun decrypt(sealed: ByteArray): ByteArray = failure?.let { throw it } ?: real.decrypt(sealed)
    }

    // Guarda una sesión con el cifrado sano y deja el fichero para que lo lea otro DataStore.
    private suspend fun savedBeforeFailing(cipher: FlakyCipher) {
        rule.open(cipher).save(sessionOf(ANA))
        rule.closeAll()
    }

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? = runCatching { block() }.exceptionOrNull()

    @Test
    fun provider_exception_on_read_comes_out_of_current() = runTest {
        val cipher = FlakyCipher()
        savedBeforeFailing(cipher)
        val error = ProviderException("keystore busy")
        cipher.failure = error
        val store = rule.open(cipher)
        assertSame(error, failureOf { store.current() })
    }

    // DataStore envuelve las IOException de lectura en otra IOException («Inoperable file…») con la
    // original como causa: sigue siendo una IOException, que es lo que mira el arranque (11c).
    @Test
    fun io_exception_on_read_comes_out_of_current_still_as_an_io_exception_with_the_original_as_cause() = runTest {
        val cipher = FlakyCipher()
        savedBeforeFailing(cipher)
        val error = IOException("KeyStore.load")
        cipher.failure = error
        val store = rule.open(cipher)
        val failure = failureOf { store.current() }
        assertTrue(failure is IOException)
        assertTrue(generateSequence(failure) { it.cause }.any { it === error })
    }

    @Test
    fun a_failed_read_does_not_erase_the_saved_session() = runTest {
        val cipher = FlakyCipher()
        savedBeforeFailing(cipher)
        cipher.failure = ProviderException("keystore busy")
        failureOf { rule.open(cipher).current() }
        rule.closeAll()
        cipher.failure = null
        assertEquals(sessionOf(ANA), rule.open(cipher).current())
    }

    // El arranque reintenta con el MISMO DataStore: después de un fallo pasajero, leer vuelve a funcionar.
    @Test
    fun after_a_transient_failure_the_same_store_reads_again() = runTest {
        val cipher = FlakyCipher()
        savedBeforeFailing(cipher)
        cipher.failure = ProviderException("keystore busy")
        val store = rule.open(cipher)
        failureOf { store.current() }
        cipher.failure = null
        assertEquals(sessionOf(ANA), store.current())
    }

    // Hallazgo 3 del plan de 11c: ¿«save lo sustituye» vale también con un fallo del sistema?
    // Con un fallo que no pasa, updateData tiene que leer antes de escribir y la lectura lanza: save
    // lanza (y LoginViewModel no lo captura). Con CorruptionException sí sustituye (ver FileTest).
    @Test
    fun save_while_the_read_keeps_failing_throws_the_same_failure() = runTest {
        val cipher = FlakyCipher()
        savedBeforeFailing(cipher)
        val error = ProviderException("keystore busy")
        cipher.failure = error
        val store = rule.open(cipher)
        assertSame(error, failureOf { store.save(sessionOf(BETO)) })
    }

    @Test
    fun save_after_a_transient_failure_replaces_the_session() = runTest {
        val cipher = FlakyCipher()
        savedBeforeFailing(cipher)
        cipher.failure = ProviderException("keystore busy")
        val store = rule.open(cipher)
        failureOf { store.current() }
        cipher.failure = null
        store.save(sessionOf(BETO))
        rule.closeAll()
        assertEquals(sessionOf(BETO), rule.open(cipher).current())
    }
}
