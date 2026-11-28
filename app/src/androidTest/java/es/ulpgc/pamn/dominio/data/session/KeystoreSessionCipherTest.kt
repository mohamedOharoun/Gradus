package es.ulpgc.pamn.dominio.data.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.GeneralSecurityException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class KeystoreSessionCipherTest {

    private val alias = testAlias()
    private val cipher = KeystoreSessionCipher(alias)
    private val plain = "{\"token\":\"secreto\"} ñ 🎓".encodeToByteArray()

    @After
    fun deleteTestKey() = deleteKey(alias)

    private fun failureOf(block: () -> Unit): Throwable? = runCatching(block).exceptionOrNull()

    @Test
    fun round_trip() {
        assertArrayEquals(plain, cipher.decrypt(cipher.encrypt(plain)))
    }

    @Test
    fun two_encryptions_of_same_text_differ() {
        assertFalse(cipher.encrypt(plain).contentEquals(cipher.encrypt(plain)))
    }

    @Test
    fun changed_byte_fails_decryption() {
        val sealed = cipher.encrypt(plain)
        sealed[sealed.size / 2] = (sealed[sealed.size / 2] + 1).toByte()
        assertTrue(failureOf { cipher.decrypt(sealed) } is GeneralSecurityException)
    }

    @Test
    fun sealed_size_is_iv_plus_text_plus_tag() {
        assertEquals(12 + plain.size + 16, cipher.encrypt(plain).size)
    }

    @Test
    fun too_short_input_fails_with_security_exception() {
        cipher.encrypt(plain)
        assertTrue(failureOf { cipher.decrypt(ByteArray(5)) } is GeneralSecurityException)
    }

    @Test
    fun encrypt_creates_key_when_missing() {
        assertFalse(keystore().containsAlias(alias))
        cipher.encrypt(plain)
        assertTrue(keystore().containsAlias(alias))
    }

    @Test
    fun decrypt_with_missing_key_does_not_create_it() {
        val sealed = cipher.encrypt(plain)
        deleteKey(alias)
        assertTrue(failureOf { cipher.decrypt(sealed) } is GeneralSecurityException)
        assertFalse(keystore().containsAlias(alias))
    }

    @Test
    fun another_instance_with_same_alias_decrypts() {
        assertArrayEquals(plain, KeystoreSessionCipher(alias).decrypt(cipher.encrypt(plain)))
    }

    @Test
    fun decrypt_after_key_recreated_fails() {
        val sealed = cipher.encrypt(plain)
        deleteKey(alias)
        cipher.encrypt(plain)
        assertTrue(failureOf { cipher.decrypt(sealed) } is GeneralSecurityException)
    }

    // Con el cerrojo, solo un hilo crea la clave; sin él, otro la sustituiría y lo que cifró el primero
    // no se podría descifrar.
    @Test
    fun concurrent_first_encryptions_share_one_key() {
        val threads = 8
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        try {
            val sealed = (1..threads).map { n ->
                pool.submit(Callable {
                    start.await()
                    KeystoreSessionCipher(alias).encrypt("mensaje $n".encodeToByteArray())
                })
            }
            start.countDown()
            sealed.forEachIndexed { i, future ->
                assertEquals("mensaje ${i + 1}", cipher.decrypt(future.get()).decodeToString())
            }
        } finally {
            pool.shutdown()
        }
    }
}
