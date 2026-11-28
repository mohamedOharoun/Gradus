package es.ulpgc.pamn.dominio.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClearIfCurrentTest {

    private val account = Account(id = "id-1", name = "Ana", email = "ana@ulpgc.es", createdAt = "2026-11-18T10:00:00Z")
    private val session = Session(token = "t1", expiresAt = "2026-12-18T10:00:00Z", account = account)

    @Test
    fun `the same session is cleared`() = runTest {
        val store = InMemorySessionStore()
        store.save(session)
        store.clearIfCurrent(session)
        assertNull(store.current())
    }

    @Test
    fun `a session with another token is not touched`() = runTest {
        val store = InMemorySessionStore()
        val newer = session.copy(token = "t2")
        store.save(newer)
        store.clearIfCurrent(session)
        assertEquals(newer, store.current())
    }

    // Se compara solo el token: un 401 invalida el token sea cual sea su expiresAt.
    @Test
    fun `the same token with another expiry is cleared`() = runTest {
        val store = InMemorySessionStore()
        store.save(session.copy(expiresAt = "2027-01-01T00:00:00Z"))
        store.clearIfCurrent(session)
        assertNull(store.current())
    }

    @Test
    fun `without a session it does nothing`() = runTest {
        val store = InMemorySessionStore()
        store.clearIfCurrent(session)
        assertNull(store.current())
    }
}
