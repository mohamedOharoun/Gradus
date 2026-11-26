package es.ulpgc.pamn.dominio.account.storage

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

// Traducción Session <-> JSON (para guardarla en disco). Lo ilegible da null y nunca lanza.
class SessionJsonTest {

    private val account = Account(id = "id-1", name = "Ana", email = "ana@ulpgc.es", createdAt = "2026-11-18T10:00:00Z")
    private val session = Session(token = "t1", expiresAt = "2026-12-18T10:00:00Z", account = account)

    private val valid =
        """{"token":"t1","expiresAt":"2026-12-18T10:00:00Z","account":""" +
            """{"id":"id-1","name":"Ana","email":"ana@ulpgc.es","createdAt":"2026-11-18T10:00:00Z"}}"""

    @Test
    fun `a session survives the round trip`() {
        assertEquals(session, decodeSession(encodeSession(session)))
    }

    @Test
    fun `accents and emojis in the name survive the round trip`() {
        val fancy = session.copy(account = account.copy(name = "José Ñandú 🎓 山田"))
        assertEquals(fancy, decodeSession(encodeSession(fancy)))
    }

    @Test
    fun `unknown fields are ignored at both levels`() {
        val text = valid
            .replace("""{"token""", """{"extra":1,"token""")
            .replace("""{"id":""", """{"nick":"x","id":""")
        assertEquals(session, decodeSession(text))
    }

    @Test
    fun `broken json gives null`() {
        assertNull(decodeSession("""{"token":"t1","expiresAt":"""))
    }

    @Test
    fun `an empty text gives null`() {
        assertNull(decodeSession(""))
    }

    @Test
    fun `a missing token gives null`() {
        assertNull(decodeSession(valid.replace(""""token":"t1",""", "")))
    }

    @Test
    fun `a missing account email gives null`() {
        assertNull(decodeSession(valid.replace(""""email":"ana@ulpgc.es",""", "")))
    }

    @Test
    fun `a null token gives null`() {
        assertNull(decodeSession(valid.replace(""""token":"t1"""", """"token":null""")))
    }

    @Test
    fun `a json that is not an object gives null`() {
        assertNull(decodeSession("[]"))
    }

    @Test
    fun `a field of the wrong type gives null`() {
        assertNull(decodeSession(valid.replace(""""token":"t1"""", """"token":5""")))
    }

    // El token no puede acabar en un log por un toString.
    @Test
    fun `the stored dto does not show the token in its text`() {
        val dto = StoredSessionDto("token-secreto-123", "2026-12-18T10:00:00Z", StoredAccountDto("i", "n", "e", "c"))
        assertFalse(dto.toString().contains("token-secreto-123"))
    }
}
