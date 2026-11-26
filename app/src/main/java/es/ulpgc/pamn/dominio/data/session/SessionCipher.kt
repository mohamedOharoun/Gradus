package es.ulpgc.pamn.dominio.data.session

// Cómo se cifran los bytes de la sesión guardada (patrón Strategy): en la app, con una clave de
// Android Keystore (KeystoreSessionCipher); en los tests JVM, con un cifrado falso.
interface SessionCipher {

    fun encrypt(plain: ByteArray): ByteArray

    // Lanza GeneralSecurityException si no se puede descifrar (otra clave, un byte cambiado, muy corto
    // o sin clave): eso es «esta sesión no vale», y quien la usa solo captura esa. Los fallos del propio
    // sistema (ProviderException de Keystore, IOException de KeyStore.load) salen tal cual: no dicen que
    // la sesión esté mal, así que no se convierten en «sin sesión» ni se borra nada.
    fun decrypt(sealed: ByteArray): ByteArray
}
