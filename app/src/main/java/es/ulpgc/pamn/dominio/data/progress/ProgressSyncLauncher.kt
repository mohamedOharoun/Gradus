package es.ulpgc.pamn.dominio.data.progress

import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.progress.ProgressSync
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Pide sincronizar sin bloquear a quien llama (Command). La sincronización corre en `scope`, un
// ámbito de la APP (en AppContainer, uno con SupervisorJob; en los tests, el de la prueba): no se
// cancela al cerrarse la pantalla que la pidió.
// - Lee la sesión al ejecutarse. Sin sesión no llama al servidor y lastOutcome no cambia.
// - Con sesión llama SIEMPRE a ProgressSync, aunque no haya nada pendiente (B-10).
// - No captura excepciones: sync no lanza por la red, y un fallo del almacén es real (sale por el
//   ámbito). No reintenta (D13). Dos peticiones a la vez no hacen daño (B-11): sin Mutex.
// - lastOutcome: null hasta el primer desenlace. Con dos a la vez, queda el de la que termina la última.
class ProgressSyncLauncher(
    private val sync: ProgressSync,
    private val sessions: SessionStore,
    private val scope: CoroutineScope,
) : ProgressSyncRequester {

    private val outcome = MutableStateFlow<SyncOutcome?>(null)
    val lastOutcome: StateFlow<SyncOutcome?> = outcome.asStateFlow()

    override fun requestSync() {
        scope.launch {
            val session = sessions.current() ?: return@launch
            outcome.value = sync.sync(session)
        }
    }
}
