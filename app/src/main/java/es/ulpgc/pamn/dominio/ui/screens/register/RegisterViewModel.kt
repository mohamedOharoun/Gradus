package es.ulpgc.pamn.dominio.ui.screens.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterStep

// Guarda el registro en memoria mientras dura la pantalla (sobrevive al giro del móvil).
// Recibe el AccountGateway por constructor (no sabe si es HTTP o uno falso); lo usa en onRegister (paso 8).
class RegisterViewModel(private val accountGateway: AccountGateway) : ViewModel() {

    companion object {
        // Android crea los ViewModel por su cuenta: la factory le dice cómo pasarle el gateway.
        fun factory(accountGateway: AccountGateway) = viewModelFactory {
            initializer { RegisterViewModel(accountGateway) }
        }
    }

    var form by mutableStateOf(RegisterForm())
        private set

    fun onNameChange(name: String) {
        form = form.withName(name)
    }

    fun onEmailChange(email: String) {
        form = form.withEmail(email)
    }

    fun onPasswordChange(password: String) {
        form = form.withPassword(password)
    }

    fun onConfirmationChange(confirmation: String) {
        form = form.withConfirmation(confirmation)
    }

    fun onTermsChange(accepted: Boolean) {
        form = form.withTermsAccepted(accepted)
    }

    fun onNext() {
        form = form.next()
    }

    fun onEdit(step: RegisterStep) {
        form = form.goBackTo(step)
    }

    // Retrocede un paso. Devuelve false si ya estaba en el primero: entonces hay que salir del registro.
    fun onBack(): Boolean {
        val previous = form.back() ?: return false
        form = previous
        return true
    }
}
