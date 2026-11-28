package es.ulpgc.pamn.dominio.data.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.data.progress.ANA
import es.ulpgc.pamn.dominio.data.progress.BETO
import es.ulpgc.pamn.dominio.data.progress.sessionOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Lo que queda en el disco: dónde, cifrado, qué pasa al reabrir y con un fichero o una clave perdidos.
@RunWith(AndroidJUnit4::class)
class DataStoreSessionStoreFileTest {

    @get:Rule
    val rule = SessionStoreRule()

    @Test
    fun file_lives_in_no_backup_dir() = runTest {
        rule.open().save(sessionOf(ANA))
        assertEquals(context.noBackupFilesDir, rule.file.parentFile)
        assertTrue(rule.file.length() > 0)
    }

    @Test
    fun second_datastore_after_cancelling_first_reads_saved() = runTest {
        rule.open().save(sessionOf(ANA))
        rule.closeAll()
        assertEquals(sessionOf(ANA), rule.open().current())
    }

    @Test
    fun cleared_session_stays_cleared_after_reopening() = runTest {
        val first = rule.open()
        first.save(sessionOf(ANA))
        first.clear()
        rule.closeAll()
        assertEquals(0L, rule.file.length())
        assertNull(rule.open().current())
    }

    @Test
    fun broken_file_gives_null_then_save_works() = runTest {
        // Más largo que IV + etiqueta, para que falle la etiqueta y no solo la longitud.
        rule.file.writeBytes(ByteArray(40) { it.toByte() })
        val store = rule.open()
        assertNull(store.current())
        store.save(sessionOf(BETO))
        rule.closeAll()
        assertEquals(sessionOf(BETO), rule.open().current())
    }

    @Test
    fun file_bytes_contain_neither_token_nor_email() = runTest {
        rule.open().save(sessionOf(ANA))
        val text = rule.file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(text.contains(sessionOf(ANA).token))
        assertFalse(text.contains(sessionOf(ANA).account.email))
    }

    @Test
    fun deleted_keystore_key_gives_null() = runTest {
        rule.open().save(sessionOf(ANA))
        rule.closeAll()
        deleteKey(rule.alias)
        assertNull(rule.open().current())
    }

    @Test
    fun after_deleted_key_a_new_session_can_be_saved_and_read_again() = runTest {
        rule.open().save(sessionOf(ANA))
        rule.closeAll()
        deleteKey(rule.alias)
        rule.open().save(sessionOf(BETO))
        rule.closeAll()
        assertEquals(sessionOf(BETO), rule.open().current())
    }
}
