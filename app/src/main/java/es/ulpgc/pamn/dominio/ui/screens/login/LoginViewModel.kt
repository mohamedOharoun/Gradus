package es.ulpgc.pamn.dominio.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.LogInOutcome
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.applying
import es.ulpgc.pamn.dominio.account.toCredentials
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.model.LoginForm
import kotlinx.coroutines.launch

// Guarda el estado del inicio de sesión fuera de la pantalla, así sobrevive al giro del móvil.
// Flujo en una dirección: la pantalla lee `form` y avisa de lo que pasa con los métodos on…
// Recibe los puertos por constructor: no sabe si el servidor es HTTP ni dónde se guarda la sesión.
class LoginViewModel(
    private val accountGateway: AccountGateway,
    private val sessionStore: SessionStore,
    private val progressSync: ProgressSyncRequester,
) : ViewModel() {

    companion object {
        // Android crea los ViewModel por su cuenta: la factory le dice cómo pasarle las dependencias.
        fun factory(accountGateway: AccountGateway, sessionStore: SessionStore, progressSync: ProgressSyncRequester) =
            viewModelFactory { initializer { LoginViewModel(accountGateway, sessionStore, progressSync) } }
    }

    var form by mutableStateOf(LoginForm())
        private set

    fun onEmailChange(email: String) {
        form = form.withEmail(email)
    }

    fun onPasswordChange(password: String) {
        form = form.withPassword(password)
    }

    // «Entrar» (y «Reintentar»). Con datos no válidos o un envío en curso, solo muestra los errores.
    // La respuesta vuelve al formulario como un estado más: GradusNavHost navega al ver Succeeded.
    fun onSubmit() {
        val credentials = form.toCredentials()
        if (credentials == null || !form.canSend) {
            form = form.submitted()
            return
        }
        form = form.sending()
        viewModelScope.launch {
            val outcome = accountGateway.logIn(credentials)
            // La sesión se guarda antes de marcar el formulario como terminado: así, cuando se
            // navegue a Inicio, la sesión ya está disponible.
            if (outcome is LogInOutcome.LoggedIn) {
                sessionStore.save(outcome.session)
                progressSync.requestSync() // trae lo dominado en otros móviles; la sesión ya está guardada
            }
            form = form.applying(outcome)
        }
    }
}
