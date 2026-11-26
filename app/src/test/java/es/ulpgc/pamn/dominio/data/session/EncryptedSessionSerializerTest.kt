package es.ulpgc.pamn.dominio.data.session

import androidx.datastore.core.CorruptionException
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class EncryptedSessionSerializerTest {

    private val cipher = FakeSessionCipher()
    private val serializer = EncryptedSessionSerializer(cipher)

    private val session = Session(
        token = "secret-token",
        expiresAt = "2026-12-18T10:00:00Z",
        account = Account("ana", "Ana Pérez Ñúñez", "ana@ulpgc.es", "2026-11-13T10:00:00Z"),
    )

    private suspend fun written(value: Session?): ByteArray =
        ByteArrayOutputStream().also { serializer.writeTo(value, it) }.toByteArray()

    private suspend fun read(bytes: ByteArray): Session? =
        serializer.readFrom(ByteArrayInputStream(bytes))

    private suspend fun failureReading(bytes: ByteArray): Throwable? =
        runCatching { read(bytes) }.exceptionOrNull()

    @Test
    fun `a session written and read back is the same, with accents`() = runTest {
        assertEquals(session, read(written(session)))
    }

    @Test
    fun `the default value is no session`() {
        assertNull(serializer.defaultValue)
    }

    @Test
    fun `no session is written as an empty file`() = runTest {
        assertEquals(0, written(null).size)
    }

    @Test
    fun `an empty file reads as no session`() = runTest {
        assertNull(read(ByteArray(0)))
    }

    @Test
    fun `what is written goes through the cipher, so the token is not in plain text`() = runTest {
        val bytes = written(session)
        assertArrayEquals(FakeSessionCipher.MARK, bytes.copyOfRange(0, FakeSessionCipher.MARK.size))
        assertFalse(String(bytes, Charsets.UTF_8).contains("secret-token"))
    }

    @Test
    fun `bytes that cannot be decrypted give a CorruptionException`() = runTest {
        assertTrue(failureReading(byteArrayOf(1, 2, 3)) is CorruptionException)
    }

    @Test
    fun `bytes that decrypt to something that is not a session give a CorruptionException`() = runTest {
        val notASession = cipher.encrypt("""{"token":"secret-token"""".toByteArray())
        assertTrue(failureReading(notASession) is CorruptionException)
    }

    @Test
    fun `the failure for an unreadable session does not quote the token, not even in its causes`() = runTest {
        val notASession = cipher.encrypt("""{"token":"secret-token"""".toByteArray())
        val chain = generateSequence(failureReading(notASession)) { it.cause }.toList()
        assertTrue(chain.isNotEmpty())
        chain.forEach {
            assertFalse(it.toString().contains("secret-token"))
            assertFalse(it.message.orEmpty().contains("secret-token"))
        }
    }

    @Test
    fun `an IOException of the stream is not turned into a corruption, so the file is not erased`() = runTest {
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("disk")
        }
        val failure = runCatching { serializer.readFrom(broken) }.exceptionOrNull()
        assertEquals(IOException::class.java, failure?.javaClass)
    }
}
