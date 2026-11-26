package es.ulpgc.pamn.dominio.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.security.ProviderException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Decide el estado de arranque (Loading | SignedIn | SignedOut) a partir del Flow de la sesión guardada.
// Recibe el Flow por constructor: no sabe que detrás hay DataStore ni Keystore.
//
// Si leer la sesión falla (isSessionReadFailure):
// - el estado es SignedOut, sin borrar el fichero y sin cerrar la app, y
// - se vuelve a leer cada `retryDelay`. Hace falta: un Flow que falla se termina, y el estado se
//   quedaría para siempre en SignedOut aunque el usuario iniciara sesión (entraría en Main y la regla de
//   «sin sesión en Main» lo echaría a Login una y otra vez).
// Consecuencia: un error pasajero con la app ya dentro de Main (Android restauró la pila) lleva a Login
// aunque la sesión siga guardada. Cuando la lectura funciona llega SignedIn, pero el usuario se queda en
// Login: no entra solo. Si inicia sesión antes del siguiente reintento, puede ver un parpadeo de Login.
// Cualquier otro error (un fallo de programación) no se traga: cierra la app.
class StartViewModel(
    sessions: Flow<Session?>,
    progressSync: ProgressSyncRequester,
    retryDelay: Duration = 2.seconds,
) : ViewModel() {

    companion object {
        // Android crea los ViewModel por su cuenta: la factory le dice cómo pasarle el Flow y el requester.
        fun factory(sessions: Flow<Session?>, progressSync: ProgressSyncRequester) =
            viewModelFactory { initializer { StartViewModel(sessions, progressSync) } }
    }

    // Eagerly: escucha desde que se crea el ViewModel (vive con la Activity, sobrevive al giro).
    // Pide sincronizar una sola vez, solo si el primer estado resuelto tras Loading es SignedIn
    // (arranque con sesión guardada → Main). En SignedOut → SignedIn (tras Login) no lo pide para
    // no pisar con REPLACE la petición de LoginViewModel; tampoco tras un fallo pasajero de lectura.
    val state: StateFlow<StartState> = run {
        var initialHandled = false
        sessions
            .map { it.toStartState() }
            .retryWhen { cause, _ ->
                if (!cause.isSessionReadFailure()) return@retryWhen false
                emit(StartState.SignedOut)
                delay(retryDelay)
                true
            }
            .onEach { resolved ->
                if (!initialHandled) {
                    initialHandled = true
                    if (resolved == StartState.SignedIn) progressSync.requestSync()
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, StartState.Loading)
    }
}

// Los fallos al leer la sesión que NO significan «no hay sesión»: IOException (también la
// CorruptionException de DataStore, que es una IOException) y ProviderException de Keystore.
internal fun Throwable.isSessionReadFailure(): Boolean = this is IOException || this is ProviderException
