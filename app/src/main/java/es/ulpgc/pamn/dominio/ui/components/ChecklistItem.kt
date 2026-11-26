package es.ulpgc.pamn.dominio.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

enum class CheckStatus { MET, PENDING, FAILED }

// Una línea de comprobación: círculo (✓ morado, vacío o rojo) y su texto.
// `emphasized` pone el texto en morado y negrita cuando se cumple (Figma: «Las contraseñas coinciden»).
@Composable
fun ChecklistItem(
    text: String,
    status: CheckStatus,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val spoken = stringResource(status.spoken())
    val textColor = when {
        status == CheckStatus.FAILED -> colors.error
        status == CheckStatus.MET && emphasized -> colors.primary
        else -> colors.onSurfaceVariant
    }
    Row(
        // TalkBack lee el texto y, después, si se cumple.
        modifier = modifier.semantics(mergeDescendants = true) { stateDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(status)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (status == CheckStatus.MET && emphasized) FontWeight.Bold else null,
            color = textColor,
        )
    }
}

@StringRes
private fun CheckStatus.spoken(): Int = when (this) {
    CheckStatus.MET -> R.string.check_met
    CheckStatus.PENDING -> R.string.check_pending
    CheckStatus.FAILED -> R.string.check_failed
}

@Composable
private fun CheckCircle(status: CheckStatus) {
    val colors = MaterialTheme.colorScheme
    val shape = Modifier.size(16.dp)
    when (status) {
        CheckStatus.MET -> Box(shape.background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = colors.onPrimary,
            )
        }
        CheckStatus.PENDING -> Box(shape.border(1.5.dp, colors.outline, CircleShape))
        CheckStatus.FAILED -> Box(shape.border(1.5.dp, colors.error, CircleShape))
    }
}

@LightDarkPreviews
@Composable
private fun ChecklistItemPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChecklistItem("Cumplido", CheckStatus.MET)
                ChecklistItem("Pendiente", CheckStatus.PENDING)
                ChecklistItem("No cumplido", CheckStatus.FAILED)
                ChecklistItem("Cumplido y destacado", CheckStatus.MET, emphasized = true)
            }
        }
    }
}
