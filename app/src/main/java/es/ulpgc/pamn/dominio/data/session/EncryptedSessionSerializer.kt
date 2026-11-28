package es.ulpgc.pamn.dominio.data.session

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.storage.decodeSession
import es.ulpgc.pamn.dominio.account.storage.encodeSession
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

// Adapter: DataStore guarda bytes; esto los traduce a la Session (JSON de sharedLogic) y los cifra.
// «Sin sesión» es un fichero vacío. Lo que no se puede descifrar o leer lanza CorruptionException, que
// el ReplaceFileCorruptionHandler de DataStoreSessionStore convierte en «sin sesión». Una IOException
// del disco sale tal cual: si fuera una corrupción, el handler borraría una sesión buena.
// Los mensajes son fijos: nunca citan el contenido (llevaría el token).
class EncryptedSessionSerializer(private val cipher: SessionCipher) : Serializer<Session?> {

    override val defaultValue: Session? = null

    override suspend fun readFrom(input: InputStream): Session? {
        val sealed = input.readBytes()
        if (sealed.isEmpty()) return null
        val plain = try {
            cipher.decrypt(sealed)
        } catch (e: GeneralSecurityException) {
            throw CorruptionException("No se puede descifrar la sesión guardada", e)
        }
        return decodeSession(plain.decodeToString())
            ?: throw CorruptionException("La sesión guardada no se puede leer")
    }

    override suspend fun writeTo(t: Session?, output: OutputStream) {
        if (t == null) return
        output.write(cipher.encrypt(encodeSession(t).encodeToByteArray()))
    }
}
