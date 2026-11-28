
package es.ulpgc.pamn.dominio.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.exercise.OptionMark
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import es.ulpgc.pamn.dominio.ui.theme.TextoSobreOscuro

// Opción única (radio: solo una) o selección múltiple (casilla: varias).
enum class AnswerOptionKind { SINGLE, MULTIPLE }

private val Shape = RoundedCornerShape(16.dp)

// Una opción tocable de una pregunta. Sin estado: le dicen si está elegida y qué marca enseña tras
// comprobar (OptionMark, de sharedLogic), y avisa con onClick. `enabled = false` después de
// comprobar (ya no se puede cambiar).
//
// Los cuatro aspectos: normal · elegida (lavanda) · correcta (morado, ✓) · incorrecta (rojo, ✗).
// Una opción buena que no se eligió también sale en morado: así se ve cuál era la respuesta.
@Composable
fun AnswerOption(
    text: String,
    kind: AnswerOptionKind,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mark: OptionMark = OptionMark.NONE,
    enabled: Boolean = true,
) {
    val style = answerOptionStyle(selected, mark)
    val markText = mark.label()?.let { stringResource(it) }
    // Toda la tarjeta es la zona táctil (y TalkBack la lee como «radio» o «casilla», con su estado).
    val interaction = when (kind) {
        AnswerOptionKind.SINGLE -> Modifier.selectable(selected, enabled, Role.RadioButton, onClick = onClick)
        AnswerOptionKind.MULTIPLE -> Modifier.toggleable(selected, enabled, Role.Checkbox) { onClick() }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(Shape)
            .then(interaction)
            .semantics { markText?.let { stateDescription = it } },
        shape = Shape,
        color = style.container,
        contentColor = style.content,
        border = style.border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Solo dibujo (onClick = null): quien responde al toque es la tarjeta entera.
            when (kind) {
                AnswerOptionKind.SINGLE -> RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = style.content, unselectedColor = style.content,
                        disabledSelectedColor = style.content, disabledUnselectedColor = style.content,
                    ),
                )
                AnswerOptionKind.MULTIPLE -> Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = style.content, uncheckedColor = style.content, checkmarkColor = style.container,
                        disabledCheckedColor = style.content, disabledUncheckedColor = style.content,
                    ),
                )
            }
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            // Decorativo: la corrección ya la dice stateDescription.
            style.icon?.let { Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(24.dp)) }
        }
    }
}

private data class AnswerOptionStyle(
    val container: Color,
    val content: Color,
    val border: BorderStroke,
    @DrawableRes val icon: Int? = null,
)

// Tabla de aspectos: la marca de la corrección manda sobre la elección.
@Composable
private fun answerOptionStyle(selected: Boolean, mark: OptionMark): AnswerOptionStyle {
    val colors = MaterialTheme.colorScheme
    return when {
        mark == OptionMark.CORRECT ->
            AnswerOptionStyle(EstadoDominado, TextoSobreOscuro, BorderStroke(2.dp, EstadoDominado), R.drawable.ic_check)
        mark == OptionMark.WRONG ->
            AnswerOptionStyle(colors.errorContainer, colors.onErrorContainer, BorderStroke(2.dp, colors.error), R.drawable.ic_close)
        selected ->
            AnswerOptionStyle(colors.primaryContainer, colors.onPrimaryContainer, BorderStroke(2.dp, colors.primary))
        else ->
            AnswerOptionStyle(colors.surface, colors.onSurface, BorderStroke(1.dp, colors.outlineVariant))
    }
}

@StringRes
private fun OptionMark.label(): Int? = when (this) {
    OptionMark.NONE -> null
    OptionMark.CORRECT -> R.string.option_correct
    OptionMark.WRONG -> R.string.option_wrong
}

@LightDarkPreviews
@Composable
private fun AnswerOptionSinglePreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnswerOption("Pila (normal)", AnswerOptionKind.SINGLE, selected = false, onClick = {})
                AnswerOption("Cola (elegida)", AnswerOptionKind.SINGLE, selected = true, onClick = {})
                AnswerOption("Cola (correcta)", AnswerOptionKind.SINGLE, selected = true, onClick = {}, mark = OptionMark.CORRECT, enabled = false)
                AnswerOption("Pila (incorrecta)", AnswerOptionKind.SINGLE, selected = true, onClick = {}, mark = OptionMark.WRONG, enabled = false)
                AnswerOption("Cola (era la buena)", AnswerOptionKind.SINGLE, selected = false, onClick = {}, mark = OptionMark.CORRECT, enabled = false)
                AnswerOption("Montículo (sin corrección)", AnswerOptionKind.SINGLE, selected = false, onClick = {}, enabled = false)
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun AnswerOptionMultiplePreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnswerOption("Árbol binario (normal)", AnswerOptionKind.MULTIPLE, selected = false, onClick = {})
                AnswerOption("Montículo (marcada)", AnswerOptionKind.MULTIPLE, selected = true, onClick = {})
                AnswerOption("Árbol binario (correcta)", AnswerOptionKind.MULTIPLE, selected = true, onClick = {}, mark = OptionMark.CORRECT, enabled = false)
                AnswerOption("Grafo con ciclos (incorrecta)", AnswerOptionKind.MULTIPLE, selected = true, onClick = {}, mark = OptionMark.WRONG, enabled = false)
                AnswerOption("Montículo (faltaba marcarla)", AnswerOptionKind.MULTIPLE, selected = false, onClick = {}, mark = OptionMark.CORRECT, enabled = false)
            }
        }
    }
}
