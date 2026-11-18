package es.ulpgc.pamn.dominio.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemorySessionStoreTest {

    private val account = Account(id = "id-1", name = "Ana", email = "ana@ulpgc.es", createdAt = "2026-11-18T10:00:00Z")
    private val session = Session(token = "t1", expiresAt = "2026-12-18T10:00:00Z", account = account)

    @Test
    fun `a new store has no session`() = runTest {
        assertNull(InMemorySessionStore().current())
    }

    @Test
    fun `a saved session can be read back`() = runTest {
        val store = InMemorySessionStore()
        store.save(session)
        assertEquals(session, store.current())
    }

    // Una sola sesión por dispositivo: iniciar sesión otra vez sustituye a la anterior.
    @Test
    fun `saving again replaces the previous session`() = runTest {
        val store = InMemorySessionStore()
        val newer = session.copy(token = "t2")
        store.save(session)
        store.save(newer)
        assertEquals(newer, store.current())
    }

    @Test
    fun `clearing removes the session`() = runTest {
        val store = InMemorySessionStore()
        store.save(session)
        store.clear()
        assertNull(store.current())
    }

    @Test
    fun `clearing an empty store is not an error`() = runTest {
        val store = InMemorySessionStore()
        store.clear()
        assertNull(store.current())
    }
}
