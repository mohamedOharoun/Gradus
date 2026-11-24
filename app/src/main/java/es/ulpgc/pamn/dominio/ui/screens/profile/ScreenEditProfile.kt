package es.ulpgc.pamn.dominio.ui.screens.profile

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.model.ProfileForm
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.FormField
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.buttonText
import es.ulpgc.pamn.dominio.ui.components.toMessage

// «Editar perfil» (Ajustes → Cuenta → Editar; docs/tareas/ajustes.md, fase 2). No está en Figma: sigue
// el estilo del inicio de sesión. Sin estado: pinta el formulario que recibe y avisa con `actions`.
// Las previews están en ScreenEditProfilePreviews.kt.
@Composable
fun ScreenEditProfile(
    email: String,
    form: ProfileForm,
    actions: EditProfileActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            FormTopBar(title = "Editar perfil", onBack = actions.onBack)
            // La parte central se desplaza si el teclado no deja sitio; el botón se queda abajo.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                FormField(
                    label = "Nombre",
                    value = form.name,
                    onValueChange = actions.onNameChange,
                    error = form.nameError?.toMessage(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                )
                FixedEmail(email)
            }
            Spacer(Modifier.height(16.dp))
            SaveButton(form, actions.onSave)
        }
    }
}

// Decisión F: el correo se ve, pero no se edita (cambiarlo exigiría comprobar que es tuyo).
@Composable
private fun FixedEmail(email: String) {
    // mergeDescendants: TalkBack lo lee de una vez, como los campos.
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Correo electrónico", style = MaterialTheme.typography.titleSmall)
        Text(email, style = MaterialTheme.typography.bodyLarge)
        Text(
            "El correo no se puede cambiar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Sin cambios no hay nada que guardar: el botón se desactiva (también mientras se envía).
// Si falla la red, el aviso va encima y el mismo botón dice «Reintentar».
@Composable
private fun SaveButton(form: ProfileForm, onSave: () -> Unit) {
    (form.submission as? Submission.Failed)?.reason?.toMessage()?.let { ErrorText(it, Modifier.padding(bottom = 8.dp)) }
    PrimaryButton(
        text = form.submission.buttonText(idle = "Guardar", sending = "Guardando…"),
        onClick = onSave,
        enabled = form.hasChanges && form.submission != Submission.Sending,
    )
}
