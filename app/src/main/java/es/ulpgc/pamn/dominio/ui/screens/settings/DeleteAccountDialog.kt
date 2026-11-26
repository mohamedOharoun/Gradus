package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.DeleteAccountForm
import es.ulpgc.pamn.dominio.model.DeleteProblem
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PasswordField
import es.ulpgc.pamn.dominio.ui.components.buttonText
import es.ulpgc.pamn.dominio.ui.components.toMessage
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// «Eliminar cuenta» (docs/tareas/ajustes.md, fase 2): un diálogo encima de Ajustes, no una pantalla.
// Decisión H: avisa de que no se puede deshacer y pide la contraseña. Decisión I: si la cuenta ha
// publicado cursos, explica por qué no se puede y solo queda «Entendido». Sin estado.
@Composable
fun DeleteAccountDialog(
    form: DeleteAccountForm,
    actions: DeleteAccountActions,
    modifier: Modifier = Modifier,
) {
    val sending = form.submission == Submission.Sending
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        modifier = modifier,
        title = { Text(stringResource(if (form.isAuthorOfCourses) R.string.delete_author_title else R.string.delete_title)) },
        text = { if (form.isAuthorOfCourses) AuthorText() else ConfirmContent(form, actions) },
        confirmButton = {
            if (form.isAuthorOfCourses) {
                TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.action_understood)) }
            } else {
                // Rojo: es la acción peligrosa (como la fila de Ajustes).
                TextButton(
                    onClick = actions.onConfirm,
                    enabled = !sending,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(form.submission.buttonText(UiText.of(R.string.action_delete_account), UiText.of(R.string.action_deleting)).asString())
                }
            }
        },
        dismissButton = {
            if (!form.isAuthorOfCourses) TextButton(onClick = actions.onDismiss, enabled = !sending) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ConfirmContent(form: DeleteAccountForm, actions: DeleteAccountActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.delete_body))
        Text(stringResource(R.string.delete_confirm_hint))
        PasswordField(
            label = stringResource(R.string.field_password),
            value = form.password,
            onValueChange = actions.onPasswordChange,
            error = form.passwordError?.toMessage()?.asString() ?: stringResource(R.string.delete_wrong_password).takeIf { form.problem == DeleteProblem.WRONG_PASSWORD },
        )
        (form.submission as? Submission.Failed)?.reason?.toMessage()?.let { ErrorText(it.asString()) }
    }
}

@Composable
private fun AuthorText() {
    Text(stringResource(R.string.delete_author_body))
}

// ── Previews ──

private val typed = DeleteAccountForm().withPassword("Gradus2026!")

@LightDarkPreviews
@Composable
private fun DeleteAccountDialogPreview() {
    GradusTheme { DeleteAccountDialog(DeleteAccountForm(), DeleteAccountActions()) }
}

@LightDarkPreviews
@Composable
private fun DeleteAccountDialogWrongPasswordPreview() {
    GradusTheme { DeleteAccountDialog(typed.sending().rejected(DeleteProblem.WRONG_PASSWORD), DeleteAccountActions()) }
}

@LightDarkPreviews
@Composable
private fun DeleteAccountDialogUnreachablePreview() {
    GradusTheme { DeleteAccountDialog(typed.sending().failed(FailureReason.UNREACHABLE), DeleteAccountActions()) }
}

@LightDarkPreviews
@Composable
private fun DeleteAccountDialogAuthorPreview() {
    GradusTheme { DeleteAccountDialog(typed.sending().rejected(DeleteProblem.AUTHOR_OF_COURSES), DeleteAccountActions()) }
}
