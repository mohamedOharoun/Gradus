package es.ulpgc.pamn.dominio.ui.screens.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.PasswordForm
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.PasswordField
import es.ulpgc.pamn.dominio.ui.components.PasswordMatchItem
import es.ulpgc.pamn.dominio.ui.components.PasswordRequirementList
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.buttonText
import es.ulpgc.pamn.dominio.ui.components.toMessage
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.asString

// «Cambiar contraseña» (Ajustes → Cuenta → Contraseña; docs/tareas/ajustes.md, fase 2). No está en
// Figma: sigue el estilo del registro (requisitos y coincidencia en vivo). Sin estado.
// Al terminar, la misma pantalla dice que se ha cambiado. Previews en ScreenChangePasswordPreviews.kt.
@Composable
fun ScreenChangePassword(
    form: PasswordForm,
    actions: ChangePasswordActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            FormTopBar(title = stringResource(R.string.action_change_password), onBack = actions.onBack)
            if (form.submission == Submission.Succeeded) Changed(actions.onBack) else Editing(form, actions)
        }
    }
}

@Composable
private fun ColumnScope.Editing(form: PasswordForm, actions: ChangePasswordActions) {
    // La parte central se desplaza si el teclado no deja sitio; el botón se queda abajo.
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PasswordField(
            label = stringResource(R.string.password_current),
            value = form.current,
            onValueChange = actions.onCurrentChange,
            error = form.currentError?.toMessage()?.asString() ?: stringResource(R.string.password_wrong_current).takeIf { form.isWrongCurrent },
        )
        Spacer(Modifier.height(8.dp))
        PasswordField(
            label = stringResource(R.string.password_new),
            value = form.next,
            onValueChange = actions.onNextChange,
            error = form.nextError?.toMessage()?.asString(),
        )
        PasswordRequirementList(unmet = form.unmetRequirements)
        PasswordField(
            label = stringResource(R.string.password_new_repeat),
            value = form.confirmation,
            onValueChange = actions.onConfirmationChange,
            // «No coinciden» ya lo dice la línea de debajo: aquí solo «obligatorio».
            error = form.confirmationError?.takeIf { it == FieldError.EMPTY }?.toMessage()?.asString(),
        )
        PasswordMatchItem(match = form.passwordsMatch)
    }
    Spacer(Modifier.height(16.dp))
    (form.submission as? Submission.Failed)?.reason?.toMessage()?.let { ErrorText(it.asString(), Modifier.padding(bottom = 8.dp)) }
    PrimaryButton(
        text = form.submission.buttonText(UiText.of(R.string.action_change_password), UiText.of(R.string.action_changing)).asString(),
        onClick = actions.onSave,
        enabled = form.submission != Submission.Sending,
    )
}

// Decisión G: se avisa de que se ha cerrado la sesión en los demás dispositivos.
// liveRegion: TalkBack lo lee en cuanto aparece, sin tener que ir a buscarlo.
@Composable
private fun ColumnScope.Changed(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(top = 32.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.password_changed_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(
            stringResource(R.string.password_changed_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    PrimaryButton(text = stringResource(R.string.password_back_to_settings), onClick = onBack)
}
