package es.ulpgc.pamn.dominio.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.rules.ExternalResource
import java.io.File
import java.security.KeyStore
import java.util.UUID

// Ayudas comunes de los tests de la sesión guardada (11b).

internal val context: Context get() = ApplicationProvider.getApplicationContext()

internal fun keystore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

internal fun deleteKey(alias: String) = keystore().deleteEntry(alias)

// Un alias de prueba nuevo en cada test. Los tests NUNCA usan KeystoreSessionCipher.DEFAULT_ALIAS:
// borrarían o sustituirían la clave de la app instalada y su sesión guardada dejaría de leerse.
internal fun testAlias() = "gradus-test-${UUID.randomUUID()}"

// Regla de JUnit: cada test tiene su fichero (en noBackupFilesDir, como la app), su alias de Keystore
// y un ámbito para cada DataStore que abre. Después se cancelan los ámbitos y se borran fichero y clave.
class SessionStoreRule : ExternalResource() {
    val alias = testAlias()
    val fileName = "test-session-${UUID.randomUUID()}.bin"
    val file: File get() = File(context.noBackupFilesDir, fileName)
    private val scopes = mutableListOf<CoroutineScope>()

    // Abre un DataStore nuevo sobre el fichero del test. Antes de abrir otro sobre el mismo fichero hay
    // que cancelar el ámbito del anterior (closeAll): dos activos sobre el mismo fichero lanzan.
    fun open(cipher: SessionCipher = KeystoreSessionCipher(alias)): DataStoreSessionStore {
        val scope = CoroutineScope(Dispatchers.IO + Job()).also { scopes += it }
        return DataStoreSessionStore.create(context, scope, fileName, cipher)
    }

    fun closeAll() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    override fun after() {
        closeAll()
        file.delete()
        deleteKey(alias)
    }
}
