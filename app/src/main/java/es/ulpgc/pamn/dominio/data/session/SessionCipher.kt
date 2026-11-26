package es.ulpgc.pamn.dominio.data.session

// Cómo se cifran los bytes de la sesión guardada (patrón Strategy): en la app, con una clave de
// Android Keystore (KeystoreSessionCipher); en los tests JVM, con un cifrado falso.
interface SessionCipher {

    fun encrypt(plain: ByteArray): ByteArray

    // Lanza GeneralSecurityException si no se puede descifrar (otra clave, un byte cambiado, muy corto
    // o sin clave). Nunca otra excepción: quien la usa solo captura esa.
    fun decrypt(sealed: ByteArray): ByteArray
}
