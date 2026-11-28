package es.ulpgc.pamn.dominio.data.session

import java.security.GeneralSecurityException

// Cifrado de prueba (patrón Fake): «cifra» dando la vuelta a los bytes y les pone delante una marca.
// Lo que no empieza por la marca no se puede «descifrar», como un fichero de otra clave.
class FakeSessionCipher : SessionCipher {

    override fun encrypt(plain: ByteArray): ByteArray = MARK + plain.reversedArray()

    override fun decrypt(sealed: ByteArray): ByteArray {
        if (sealed.size < MARK.size || !sealed.copyOfRange(0, MARK.size).contentEquals(MARK)) {
            throw GeneralSecurityException("Tag mismatch")
        }
        return sealed.copyOfRange(MARK.size, sealed.size).reversedArray()
    }

    companion object {
        val MARK = byteArrayOf(0x47, 0x52)
    }
}
