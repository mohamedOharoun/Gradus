package es.ulpgc.pamn.dominio.ui.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.RenameOutcome
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.applying
import es.ulpgc.pamn.dominio.model.ProfileForm
import kotlinx.coroutines.launch

// «Editar perfil» (docs/tareas/ajustes.md, fase 2, tarea 6): el nombre sale de la sesión guardada;
// al guardarlo, el servidor lo cambia y aquí se guarda en el móvil la sesión con el nombre nuevo
// (Ajustes y el resto de la app la leen de ahí). Recibe los puertos por constructor.
class EditProfileViewModel(
    private val accounts: AccountGateway,
    private val sessions: SessionStore,
) : ViewModel() {

    companion object {
        fun factory(accounts: AccountGateway, sessions: SessionStore) = viewModelFactory {
            initializer { EditProfileViewModel(accounts, sessions) }
        }
    }

    var state by mutableStateOf<EditProfileState>(EditProfileState.Loading)
        private set

    init {
        viewModelScope.launch {
            state = sessions.current()
                ?.let { EditProfileState.Editing(email = it.account.email, form = ProfileForm(original = it.account.name)) }
                ?: EditProfileState.LoggedOut
        }
    }

    fun onNameChange(name: String) = updateForm { it.withName(name) }

    // «Guardar» (y reintentar). Sin cambios, con un nombre no válido o con un envío en curso, solo
    // muestra los errores. GradusNavHost vuelve a Ajustes al ver Succeeded.
    fun onSave() {
        val editing = state as? EditProfileState.Editing ?: return
        if (!editing.form.canSend) {
            state = editing.copy(form = editing.form.submitted())
            return
        }
        val sending = editing.form.sending()
        state = editing.copy(form = sending)
        viewModelScope.launch { save(sending) }
    }

    private suspend fun save(form: ProfileForm) {
        val session = sessions.current()
        if (session == null) {
            state = EditProfileState.LoggedOut
            return
        }
        val outcome = accounts.rename(session, form.nameToSend)
        when (outcome) {
            // La sesión se guarda antes de marcar el formulario como terminado: al volver a Ajustes,
            // ya tiene el nombre nuevo.
            is RenameOutcome.Renamed -> sessions.save(session.copy(account = outcome.account))
            // La sesión ya no vale: se olvida y se sale a Bienvenida.
            RenameOutcome.SessionExpired -> {
                sessions.clear()
                state = EditProfileState.LoggedOut
                return
            }
            else -> Unit
        }
        updateForm { it.applying(outcome) }
    }

    private fun updateForm(change: (ProfileForm) -> ProfileForm) {
        val editing = state as? EditProfileState.Editing ?: return
        state = editing.copy(form = change(editing.form))
    }
}
