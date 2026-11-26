package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

private enum class StepStatus { DONE, CURRENT, PENDING }

private fun statusOf(index: Int, currentIndex: Int) = when {
    index < currentIndex -> StepStatus.DONE
    index == currentIndex -> StepStatus.CURRENT
    else -> StepStatus.PENDING
}

// Indicador de pasos (Figma, registro): círculos unidos por líneas y el nombre de cada paso debajo.
// Hechos: morado con ✓. Actual: anillo morado con punto. Pendientes: gris con su número.
@Composable
fun StepIndicator(
    labels: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    // TalkBack lee una sola frase en lugar de cada círculo suelto.
    val description = stringResource(R.string.step_of_with_label, currentIndex + 1, labels.size, labels[currentIndex])
    Row(modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        labels.forEachIndexed { index, label ->
            StepItem(
                number = index + 1,
                label = label,
                status = statusOf(index, currentIndex),
                lineBefore = lineColor(index - 1, currentIndex).takeIf { index > 0 },
                lineAfter = lineColor(index, currentIndex).takeIf { index < labels.lastIndex },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// El tramo que sale del paso `index` es morado si ese paso ya está hecho.
@Composable
private fun lineColor(index: Int, currentIndex: Int): Color =
    if (index < currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

// Cada paso ocupa un tercio: media línea a cada lado del círculo, así las líneas se tocan.
@Composable
private fun StepItem(
    number: Int,
    label: String,
    status: StepStatus,
    lineBefore: Color?,
    lineAfter: Color?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth()) {
                HalfLine(lineBefore, Modifier.weight(1f))
                HalfLine(lineAfter, Modifier.weight(1f))
            }
            StepCircle(number, status)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (status == StepStatus.CURRENT) FontWeight.Bold else null,
            color = if (status == StepStatus.CURRENT) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HalfLine(color: Color?, modifier: Modifier = Modifier) {
    Box(modifier.height(2.dp).background(color ?: Color.Transparent))
}

@Composable
private fun StepCircle(number: Int, status: StepStatus) {
    val colors = MaterialTheme.colorScheme
    val (fill, border) = when (status) {
        StepStatus.DONE -> colors.primary to null
        StepStatus.CURRENT -> colors.surface to BorderStroke(2.dp, colors.primary)
        StepStatus.PENDING -> colors.surface to BorderStroke(1.dp, colors.outline)
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(fill, CircleShape)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            StepStatus.DONE -> Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.onPrimary,
            )
            StepStatus.CURRENT -> Box(Modifier.size(8.dp).background(colors.primary, CircleShape))
            StepStatus.PENDING -> Text(
                "$number",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@LightDarkPreviews
@Composable
private fun StepIndicatorPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                (0..2).forEach { StepIndicator(listOf("Nombre", "Correo", "Contraseña"), currentIndex = it) }
            }
        }
    }
}
