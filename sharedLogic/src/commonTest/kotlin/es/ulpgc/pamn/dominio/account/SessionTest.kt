package es.ulpgc.pamn.dominio.account

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionTest {

    private val session = Session(
        token = "s3cr3t-t0k3n",
        expiresAt = "2026-12-18T10:00:00Z",
        account = Account(id = "id-1", name = "Ana", email = "ana@ulpgc.es", createdAt = "2026-11-18T10:00:00Z"),
    )

    // Quien tenga el token tiene la sesión: no puede acabar en los logs.
    @Test
    fun `the token never appears in toString`() {
        assertFalse(session.toString().contains("s3cr3t-t0k3n"))
    }

    @Test
    fun `toString still shows who the session belongs to and until when`() {
        val text = session.toString()
        assertTrue(text.contains("ana@ulpgc.es"))
        assertTrue(text.contains("2026-12-18T10:00:00Z"))
    }
}
