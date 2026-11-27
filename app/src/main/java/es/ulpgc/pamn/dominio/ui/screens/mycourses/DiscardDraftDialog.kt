package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.compose.foundation.layout.heightIn
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
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import kotlin.time.Instant

// Diálogo de confirmación para descartar un borrador local (curso nuevo o nueva versión). Sin estado.
@Composable
fun DiscardDraftDialog(
    draft: CourseDraft,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val courseTitle = draft.title.ifBlank { stringResource(R.string.my_courses_untitled_draft) }
    val titleRes = if (draft.isNewVersion) {
        R.string.my_courses_discard_version_title
    } else {
        R.string.my_courses_discard_draft_title
    }
    val bodyRes = if (draft.isNewVersion) {
        R.string.my_courses_discard_version_body
    } else {
        R.string.my_courses_discard_draft_body
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(bodyRes, courseTitle)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.my_courses_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private val dialogPreviewInstant = Instant.parse("2026-11-27T18:00:00Z")

@LightDarkPreviews
@Composable
private fun DiscardDraftDialogNewDraftPreview() {
    val draft = CourseDraft(
        id = "d-1",
        userId = "user-1",
        targetCourseId = null,
        filePath = "d-1.json",
        title = "Bases de datos",
        moduleCount = 11,
        status = DraftStatus.Accepted(),
        updatedAt = dialogPreviewInstant,
    )
    GradusTheme {
        DiscardDraftDialog(draft = draft, onConfirm = {}, onDismiss = {})
    }
}

@LightDarkPreviews
@Composable
private fun DiscardDraftDialogNewVersionPreview() {
    val draft = CourseDraft(
        id = "d-2",
        userId = "user-1",
        targetCourseId = "course-1",
        filePath = "d-2.json",
        title = "Kotlin para Android",
        moduleCount = 19,
        status = DraftStatus.Accepted(),
        updatedAt = dialogPreviewInstant,
    )
    GradusTheme {
        DiscardDraftDialog(draft = draft, onConfirm = {}, onDismiss = {})
    }
}
