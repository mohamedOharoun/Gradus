package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.DeleteAccountOutcome
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.applying
import es.ulpgc.pamn.dominio.data.session.SessionSignOut
import es.ulpgc.pamn.dominio.model.DeleteAccountForm
import es.ulpgc.pamn.dominio.model.Submission
import kotlinx.coroutines.launch

// El diálogo «Eliminar cuenta» de Ajustes (docs/tareas/ajustes.md, fase 2, decisiones H e I). Va aparte
// de SettingsViewModel para que cada uno haga una sola cosa. Recibe los puertos por constructor.
class DeleteAccountViewModel(
    private val accounts: AccountGateway,
    private val sessions: SessionStore,
    private val signOut: SessionSignOut = SessionSignOut { sessions.clear() },
) : ViewModel() {

    companion object {
        fun factory(accounts: AccountGateway, sessions: SessionStore, signOut: SessionSignOut) = viewModelFactory {
            initializer { DeleteAccountViewModel(accounts, sessions, signOut) }
        }
    }

    var state by mutableStateOf<DeleteAccountState>(DeleteAccountState.Closed)
        private set

    // Siempre empieza vacío: no se queda la contraseña de una vez anterior.
    fun onOpen() {
        if (state == DeleteAccountState.Closed) state = DeleteAccountState.Open(DeleteAccountForm())
    }

    // Cancelar. Mientras se envía no se puede: la petición ya ha salido.
    fun onDismiss() {
        val open = state as? DeleteAccountState.Open ?: return
        if (open.form.submission != Submission.Sending) state = DeleteAccountState.Closed
    }

    fun onPasswordChange(password: String) = updateForm { it.withPassword(password) }

    // «Eliminar cuenta» (y reintentar). Sin contraseña, siendo autora o con un envío en curso, no envía.
    fun onConfirm() {
        val open = state as? DeleteAccountState.Open ?: return
        if (!open.form.canSend) {
            state = DeleteAccountState.Open(open.form.sending())
            return
        }
        val sending = open.form.sending()
        state = DeleteAccountState.Open(sending)
        viewModelScope.launch { delete(sending) }
    }

    private suspend fun delete(form: DeleteAccountForm) {
        val session = sessions.current()
        if (session == null) {
            state = DeleteAccountState.LoggedOut
            return
        }
        when (val outcome = accounts.deleteAccount(session, form.password)) {
            // La cuenta ya no existe: salida voluntaria (GradusNavHost lleva a Bienvenida).
            DeleteAccountOutcome.Deleted -> {
                signOut.signOut()
                state = DeleteAccountState.LoggedOut
            }
            // La sesión había caducado (401): salida involuntaria (GradusNavHost lleva a Login).
            DeleteAccountOutcome.SessionExpired -> {
                sessions.clear()
                state = DeleteAccountState.LoggedOut
            }
            else -> updateForm { it.applying(outcome) }
        }
    }

    private fun updateForm(change: (DeleteAccountForm) -> DeleteAccountForm) {
        val open = state as? DeleteAccountState.Open ?: return
        state = DeleteAccountState.Open(change(open.form))
    }
}
