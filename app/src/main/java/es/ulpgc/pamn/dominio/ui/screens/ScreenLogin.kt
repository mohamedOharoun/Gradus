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
import es.ulpgc.pamn.dominio.model.LoginForm
import es.ulpgc.pamn.dominio.ui.components.FormField
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PasswordField
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.PromptLink
import es.ulpgc.pamn.dominio.ui.components.toMessage
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Inicio de sesión (no está en Figma: sigue el estilo del registro).
// Sin estado: pinta el LoginForm que recibe y avisa de cada cosa con una lambda.
@Composable
fun ScreenLogin(
    form: LoginForm,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            FormTopBar(title = "Iniciar sesión", onBack = onBack)
            // La parte central se desplaza si el teclado no deja sitio; el botón se queda abajo.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LoginHeader()
                LoginFields(form, onEmailChange, onPasswordChange)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Entrar", onClick = onSubmit, trailingIcon = R.drawable.ic_arrow_forward)
            PromptLink(prompt = "¿No tienes cuenta?", action = "Crear cuenta", onClick = onCreateAccount)
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
private fun LoginFields(
    form: LoginForm,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    FormField(
        label = "Correo electrónico",
        value = form.email,
        onValueChange = onEmailChange,
        error = form.emailError?.toMessage(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    PasswordField(
        label = "Contraseña",
        value = form.password,
        onValueChange = onPasswordChange,
        error = form.passwordError?.toMessage(),
    )
}

@LightDarkPreviews
@Composable
private fun ScreenLoginEmptyPreview() {
    GradusTheme {
        ScreenLogin(LoginForm(), {}, {}, {}, {}, {})
    }
}

@LightDarkPreviews
@Composable
private fun ScreenLoginErrorsPreview() {
    GradusTheme {
        ScreenLogin(LoginForm(email = "ana@correo", isSubmitted = true), {}, {}, {}, {}, {})
    }
}
