package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Etiqueta gris de la tarjeta de un curso («grafos», «+2»). Solo informa: no se puede pulsar
// (las etiquetas que se eligen para filtrar son otro componente, de la tarea 7).
//
// El borde es necesario en el tema oscuro: ahí `surfaceVariant` es igual que el fondo de la tarjeta
// y sin borde la etiqueta no se distinguiría. En claro casi no se nota.
@Composable
fun TagLabel(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@LightDarkPreviews
@Composable
private fun TagLabelPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagLabel("grafos")
                TagLabel("árboles")
                TagLabel("+2")
            }
        }
    }
}
