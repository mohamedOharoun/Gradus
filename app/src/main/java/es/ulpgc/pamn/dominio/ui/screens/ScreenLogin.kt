package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.LoginForm
import es.ulpgc.pamn.dominio.model.LoginProblem
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.FormField
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PasswordField
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.PromptLink
import es.ulpgc.pamn.dominio.ui.components.buttonText
import es.ulpgc.pamn.dominio.ui.components.toMessage
import es.ulpgc.pamn.dominio.ui.screens.login.LoginActions
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Inicio de sesión (no está en Figma: sigue el estilo del registro).
// Sin estado: pinta el LoginForm que recibe y avisa de cada cosa con `actions`.
@Composable
fun ScreenLogin(
    form: LoginForm,
    actions: LoginActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            FormTopBar(title = "Iniciar sesión", onBack = actions.onBack)
            // La parte central se desplaza si el teclado no deja sitio; el botón se queda abajo.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LoginHeader()
                LoginFields(form, actions)
            }
            Spacer(Modifier.height(16.dp))
            LoginMainButton(form, actions.onSubmit)
            PromptLink(prompt = "¿No tienes cuenta?", action = "Crear cuenta", onClick = actions.onCreateAccount)
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Hola de nuevo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Entra con tu correo y tu contraseña para seguir donde lo dejaste.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoginFields(form: LoginForm, actions: LoginActions) {
    FormField(
        label = "Correo electrónico",
        value = form.email,
        onValueChange = actions.onEmailChange,
        error = form.emailError?.toMessage(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    PasswordField(
        label = "Contraseña",
        value = form.password,
        onValueChange = actions.onPasswordChange,
        error = form.passwordError?.toMessage(),
    )
}

// El aviso va encima del botón y no bajo un campo: «Correo o contraseña incorrectos» no debe
// señalar cuál de los dos falla. El mismo botón sirve para reintentar si falla la red.
// Mientras se envía, el botón se desactiva; con datos no válidos sigue activo para mostrar los errores.
@Composable
private fun LoginMainButton(form: LoginForm, onSubmit: () -> Unit) {
    form.message?.let { ErrorText(it, Modifier.padding(bottom = 8.dp)) }
    val isIdle = form.submission == Submission.Idle
    PrimaryButton(
        text = form.submission.buttonText(idle = "Entrar", sending = "Entrando…"),
        onClick = onSubmit,
        enabled = form.submission != Submission.Sending,
        trailingIcon = if (isIdle) R.drawable.ic_arrow_forward else null,
    )
}

// El error de todo el formulario o, si no hay, el motivo por el que falló el envío.
private val LoginForm.message: String?
    get() = generalError?.toMessage() ?: (submission as? Submission.Failed)?.reason?.toMessage()

@Composable
private fun LoginPreview(form: LoginForm) {
    GradusTheme { ScreenLogin(form, LoginActions()) }
}

private val filled = LoginForm(email = "ana@ulpgc.es", password = "Gradus2026!")

@LightDarkPreviews
@Composable
private fun ScreenLoginEmptyPreview() = LoginPreview(LoginForm())

@LightDarkPreviews
@Composable
private fun ScreenLoginErrorsPreview() = LoginPreview(LoginForm(email = "ana@correo", isSubmitted = true))

@LightDarkPreviews
@Composable
private fun ScreenLoginSendingPreview() = LoginPreview(filled.sending())

@LightDarkPreviews
@Composable
private fun ScreenLoginInvalidCredentialsPreview() =
    LoginPreview(filled.rejected(LoginProblem(field = null, error = FieldError.INVALID_CREDENTIALS)))

@LightDarkPreviews
@Composable
private fun ScreenLoginUnreachablePreview() = LoginPreview(filled.sending().failed(FailureReason.UNREACHABLE))
