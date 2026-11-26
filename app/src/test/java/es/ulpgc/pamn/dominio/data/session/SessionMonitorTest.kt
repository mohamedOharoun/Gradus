package es.ulpgc.pamn.dominio.data.session

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

// SessionMonitor (Decorator + Interface Segregation): envuelve SessionStore + Flow<Session?>
// distinguiendo salida voluntaria (signOut) de pérdida involuntaria (clear / 401) sin tocar el puerto.
class SessionMonitorTest {

    private val ana = Session("tok-ana", "2026-12-25T00:00:00Z", Account("ana", "Ana", "ana@ulpgc.es", "2026-11-17T10:00:00Z"))
    private val beto = Session("tok-beto", "2026-12-25T00:00:00Z", Account("beto", "Beto", "beto@ulpgc.es", "2026-11-17T10:00:00Z"))

    private class FakeReactiveSessionStore(var failOnClear: Throwable? = null) : SessionStore {
        val flow = MutableStateFlow<Session?>(null)
        override suspend fun save(session: Session) {
            flow.value = session
        }

        override suspend fun current(): Session? = flow.value
        override suspend fun clear() {
            failOnClear?.let { throw it }
            flow.value = null
        }
    }

    @Test
    fun `without a saved session it emits Missing non voluntary`() = runTest {
        val backing = FakeReactiveSessionStore()
        val monitor = SessionMonitor(backing, backing.flow)

        assertEquals(SessionUpdate.Missing(voluntary = false), monitor.updates.first())
    }

    @Test
    fun `saving a session emits Active and current returns it`() = runTest {
        val backing = FakeReactiveSessionStore()
        val monitor = SessionMonitor(backing, backing.flow)

        monitor.save(ana)

        assertEquals(ana, monitor.current())
        assertEquals(SessionUpdate.Active(ana), monitor.updates.first())
    }

    @Test
    fun `clearing via SessionStore emits Missing non voluntary (401)`() = runTest {
        val backing = FakeReactiveSessionStore()
        val monitor = SessionMonitor(backing, backing.flow)
        monitor.save(ana)

        monitor.clear()

        assertNull(monitor.current())
        assertEquals(SessionUpdate.Missing(voluntary = false), monitor.updates.first())
    }

    @Test
    fun `signing out via SessionSignOut emits Missing voluntary`() = runTest {
        val backing = FakeReactiveSessionStore()
        val monitor = SessionMonitor(backing, backing.flow)
        monitor.save(ana)

        monitor.signOut()

        assertNull(monitor.current())
        assertEquals(SessionUpdate.Missing(voluntary = true), monitor.updates.first())
    }

    @Test
    fun `saving a new session after voluntary sign out resets the flag so a later clear is non voluntary`() = runTest {
        val backing = FakeReactiveSessionStore()
        val monitor = SessionMonitor(backing, backing.flow)
        monitor.save(ana)
        monitor.signOut()
        assertEquals(SessionUpdate.Missing(voluntary = true), monitor.updates.first())

        monitor.save(beto)
        assertEquals(SessionUpdate.Active(beto), monitor.updates.first())

        monitor.clear()
        assertEquals(SessionUpdate.Missing(voluntary = false), monitor.updates.first())
    }

    @Test
    fun `if signOut fails to clear the store the voluntary flag is reset`() {
        val backing = FakeReactiveSessionStore(failOnClear = IOException("disk full"))
        val monitor = SessionMonitor(backing, backing.flow)
        runTest { monitor.save(ana) }

        assertThrows(IOException::class.java) {
            runTest { monitor.signOut() }
        }

        runTest {
            backing.failOnClear = null
            // Un 401 posterior (clear directo) no debe heredar el voluntary = true del intento fallido.
            backing.clear()
            assertEquals(SessionUpdate.Missing(voluntary = false), monitor.updates.first())
        }
    }
}
