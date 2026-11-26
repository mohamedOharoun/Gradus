package es.ulpgc.pamn.dominio.data.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.data.progress.ANA
import es.ulpgc.pamn.dominio.data.progress.BETO
import es.ulpgc.pamn.dominio.data.progress.listenTo
import es.ulpgc.pamn.dominio.data.progress.sessionOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Lo que promete el puerto SessionStore, y el Flow de cambios.
@RunWith(AndroidJUnit4::class)
class DataStoreSessionStoreTest {

    @get:Rule
    val rule = SessionStoreRule()

    private val store by lazy { rule.open() }

    @Test
    fun without_session_current_is_null() = runTest {
        assertNull(store.current())
    }

    @Test
    fun save_then_current() = runTest {
        store.save(sessionOf(ANA))
        assertEquals(sessionOf(ANA), store.current())
    }

    @Test
    fun save_replaces() = runTest {
        store.save(sessionOf(ANA))
        store.save(sessionOf(BETO))
        assertEquals(sessionOf(BETO), store.current())
    }

    @Test
    fun clear_gives_null() = runTest {
        store.save(sessionOf(ANA))
        store.clear()
        assertNull(store.current())
    }

    @Test
    fun clear_without_session_does_nothing_and_creates_no_file() = runTest {
        store.clear()
        assertNull(store.current())
        assertFalse(rule.file.exists())
    }

    @Test
    fun flow_emits_on_save_and_clear() = runTest {
        val seen = listenTo(store.session)
        assertNull(seen.receive())
        store.save(sessionOf(ANA))
        assertEquals(sessionOf(ANA), seen.receive())
        store.clear()
        assertNull(seen.receive())
    }
}
