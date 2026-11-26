package es.ulpgc.pamn.dominio.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// AES-256-GCM con una clave de Android Keystore: la clave no sale nunca del sistema.
// Formato: IV (12 bytes) + texto cifrado + etiqueta (16 bytes). El IV lo elige el propio Cipher
// (Keystore no deja dar uno propio) y va delante para poder descifrar.
// La clave no se guarda en un campo: se busca en cada operación, así se nota si alguien la ha borrado.
class KeystoreSessionCipher(private val alias: String = DEFAULT_ALIAS) : SessionCipher {

    // Solo encrypt crea la clave: descifrar sin clave es un fallo, no un motivo para inventarse otra.
    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingOrNewKey())
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(sealed: ByteArray): ByteArray {
        if (sealed.size < IV_BYTES + TAG_BITS / 8) throw GeneralSecurityException("Datos cifrados demasiado cortos")
        val key = existingKey() ?: throw GeneralSecurityException("No hay clave para descifrar")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        return cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
    }

    private fun existingKey(): SecretKey? =
        (keyStore().getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey

    // Buscar y crear van dentro del mismo cerrojo (compartido por todas las instancias). Sin él, dos
    // hilos podrían ver a la vez que no hay clave y generarla los dos: generar con un alias que ya
    // existe SUSTITUYE la clave, y lo que el primero hubiera cifrado ya no se podría descifrar.
    private fun existingOrNewKey(): SecretKey = synchronized(LOCK) {
        existingKey() ?: newKey()
    }

    private fun newKey(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }

    companion object {
        const val DEFAULT_ALIAS = "gradus-session-key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private val LOCK = Any()

        private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }
}
