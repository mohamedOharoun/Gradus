package es.ulpgc.pamn.dominio.ui.screens.detail

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
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.toMessage
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Diálogo destructivo de baja en línea (docs/tareas/ficha-curso-ui.md, Decisión C-5).
// Sigue el patrón de DeleteAccountDialog: acción principal en rojo, controles bloqueados durante
// Sending y ErrorText accesible ante errores de red o servidor.
@Composable
fun UnenrollCourseDialog(
    dialogState: UnenrollDialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sending = dialogState is UnenrollDialogState.Sending
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        modifier = modifier,
        title = { Text(stringResource(R.string.detail_unenroll_title)) },
        text = { UnenrollDialogBody(dialogState) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !sending,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(dialogState.confirmButtonText().asString())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !sending) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun UnenrollDialogBody(dialogState: UnenrollDialogState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.detail_unenroll_body))
        if (dialogState is UnenrollDialogState.Failed) {
            ErrorText(dialogState.reason.toMessage().asString())
        }
    }
}

// ── Previews ──

@LightDarkPreviews
@Composable
private fun UnenrollCourseDialogConfirmingPreview() {
    GradusTheme {
        UnenrollCourseDialog(
            dialogState = UnenrollDialogState.Confirming,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun UnenrollCourseDialogSendingPreview() {
    GradusTheme {
        UnenrollCourseDialog(
            dialogState = UnenrollDialogState.Sending,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun UnenrollCourseDialogUnreachablePreview() {
    GradusTheme {
        UnenrollCourseDialog(
            dialogState = UnenrollDialogState.Failed(FailureReason.UNREACHABLE),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun UnenrollCourseDialogServerFailurePreview() {
    GradusTheme {
        UnenrollCourseDialog(
            dialogState = UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
