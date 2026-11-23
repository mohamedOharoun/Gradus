package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import es.ulpgc.pamn.dominio.ui.screens.search.publishedLabel
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

private val OptionShape = RoundedCornerShape(12.dp)

// Opciones del mismo ancho en una fila, de las que se elige una (Figma «Fecha de publicación»).
// Genérico en T: sirve para la fecha y, cuando haya valoraciones, para «Ordenar por».
//
// Es un *selectable group*: TalkBack lee cada opción como un botón de opción («seleccionado» o no)
// y avisa de cuántas hay. Tocar la ya elegida también avisa con ella (`onSelect`). No es el
// SegmentedButton de Material porque ese pega las opciones y en el Figma van separadas.
// El estado es del que la usa (state hoisting): aquí solo se dibuja `selected`.
@Composable
fun <T> SingleChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    // IntrinsicSize.Min: la fila mide lo que la opción más alta, y las demás se igualan a ella.
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            ChoiceOption(
                text = label(option),
                isSelected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ChoiceOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(OptionShape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        shape = OptionShape,
        color = if (isSelected) colors.primaryContainer else colors.surface,
        contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
        border = if (isSelected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outline),
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@LightDarkPreviews
@Composable
private fun SingleChoiceRowPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PublishedWithin.entries.forEach { selected ->
                    SingleChoiceRow(
                        options = PublishedWithin.entries,
                        selected = selected,
                        label = ::publishedLabel,
                        onSelect = {},
                    )
                }
            }
        }
    }
}
