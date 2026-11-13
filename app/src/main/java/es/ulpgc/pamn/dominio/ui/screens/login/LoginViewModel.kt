package es.ulpgc.pamn.dominio.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import es.ulpgc.pamn.dominio.model.LoginForm

// Guarda el estado del inicio de sesión fuera de la pantalla, así sobrevive al giro del móvil.
// Flujo en una dirección: la pantalla lee `form` y avisa de lo que pasa con los métodos on…
class LoginViewModel : ViewModel() {

    var form by mutableStateOf(LoginForm())
        private set

    fun onEmailChange(email: String) {
        form = form.withEmail(email)
    }

    fun onPasswordChange(password: String) {
        form = form.withPassword(password)
    }

    // Marca el formulario como enviado (para mostrar errores) y dice si se puede entrar.
    fun onSubmit(): Boolean {
        form = form.submitted()
        return form.isValid
    }
}
