package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Bloque «DATOS AÑADIDOS» del registro: un sobretítulo y los chips que se pasan como slot.
@Composable
fun AddedDataSection(
    modifier: Modifier = Modifier,
    chips: @Composable FlowRowScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.register_added_data),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // FlowRow pasa a la línea siguiente cuando los chips no caben en una.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chips()
        }
    }
}

// Chip con un dato ya validado (✓) y el lápiz para volver a editarlo.
@Composable
fun AddedDataChip(
    text: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onEdit,
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckBadge()
            Text(text, style = MaterialTheme.typography.labelMedium)
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.action_edit),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Círculo morado con ✓. Es decorativo: el texto del chip ya dice qué dato es.
@Composable
private fun CheckBadge() {
    Box(
        modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@LightDarkPreviews
@Composable
private fun AddedDataSectionPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AddedDataSection(modifier = Modifier.padding(20.dp)) {
                AddedDataChip(text = "Ana", onEdit = {})
                AddedDataChip(text = "ana@ejemplo.com", onEdit = {})
            }
        }
    }
}
