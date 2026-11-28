package es.ulpgc.pamn.dominio.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

// SessionStore guardado en disco y cifrado: la sesión sobrevive a cerrar la app. DataStore escribe el
// fichero entero de una vez (nunca a medias), una escritura detrás de otra, y avisa de cada cambio.
// Una IOException del disco sale hacia fuera (como en Room): no se confunde con «sin sesión».
class DataStoreSessionStore(private val dataStore: DataStore<Session?>) : SessionStore {

    // Cada cambio de la sesión (fuera del puerto): la usa la app para saber si se ha borrado.
    val session: Flow<Session?> = dataStore.data

    override suspend fun save(session: Session) {
        dataStore.updateData { session }
    }

    override suspend fun current(): Session? = dataStore.data.first()

    // Sin sesión, DataStore no escribe (el valor nuevo es igual al actual).
    override suspend fun clear() {
        dataStore.updateData { null }
    }

    companion object {
        const val FILE_NAME = "gradus-session.bin"

        // Método fábrica (factory method). El fichero va en noBackupFilesDir: una copia de seguridad
        // restaurada en otro móvil no tendría la clave. Un fichero roto o que no se puede descifrar se
        // sustituye por «sin sesión». Solo puede haber un DataStore activo por fichero: los tests pasan
        // otro nombre, su ámbito (para cancelarlo antes de abrir otro) y su alias de Keystore.
        fun create(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            fileName: String = FILE_NAME,
            cipher: SessionCipher = KeystoreSessionCipher(),
        ): DataStoreSessionStore = DataStoreSessionStore(
            DataStoreFactory.create(
                serializer = EncryptedSessionSerializer(cipher),
                corruptionHandler = ReplaceFileCorruptionHandler { null },
                scope = scope,
                produceFile = { File(context.applicationContext.noBackupFilesDir, fileName) },
            ),
        )
    }
}
