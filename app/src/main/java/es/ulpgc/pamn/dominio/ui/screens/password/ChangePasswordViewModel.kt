package es.ulpgc.pamn.dominio.ui.screens.password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.ChangePasswordOutcome
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.applying
import es.ulpgc.pamn.dominio.account.toChange
import es.ulpgc.pamn.dominio.model.PasswordForm
import kotlinx.coroutines.launch

// «Cambiar contraseña» (docs/tareas/ajustes.md, fase 2, tarea 7, decisión G). La sesión de este móvil
// no cambia: el servidor solo cierra las de los otros dispositivos. Recibe los puertos por constructor.
class ChangePasswordViewModel(
    private val accounts: AccountGateway,
    private val sessions: SessionStore,
) : ViewModel() {

    companion object {
        fun factory(accounts: AccountGateway, sessions: SessionStore) = viewModelFactory {
            initializer { ChangePasswordViewModel(accounts, sessions) }
        }
    }

    var state by mutableStateOf<ChangePasswordState>(ChangePasswordState.Editing(PasswordForm()))
        private set

    fun onCurrentChange(current: String) = updateForm { it.withCurrent(current) }
    fun onNextChange(next: String) = updateForm { it.withNext(next) }
    fun onConfirmationChange(confirmation: String) = updateForm { it.withConfirmation(confirmation) }

    // «Cambiar contraseña» (y reintentar). Si falta algo o ya se está enviando, solo muestra los errores.
    fun onSave() {
        val editing = state as? ChangePasswordState.Editing ?: return
        if (!editing.form.canSend) {
            state = ChangePasswordState.Editing(editing.form.sending())
            return
        }
        val sending = editing.form.sending()
        state = ChangePasswordState.Editing(sending)
        viewModelScope.launch { save(sending) }
    }

    private suspend fun save(form: PasswordForm) {
        val session = sessions.current()
        if (session == null) {
            state = ChangePasswordState.LoggedOut
            return
        }
        val outcome = accounts.changePassword(session, form.toChange())
        // La sesión ya no vale: se olvida y se sale a Bienvenida.
        if (outcome == ChangePasswordOutcome.SessionExpired) {
            sessions.clear()
            state = ChangePasswordState.LoggedOut
            return
        }
        updateForm { it.applying(outcome) }
    }

    private fun updateForm(change: (PasswordForm) -> PasswordForm) {
        val editing = state as? ChangePasswordState.Editing ?: return
        state = ChangePasswordState.Editing(change(editing.form))
    }
}
