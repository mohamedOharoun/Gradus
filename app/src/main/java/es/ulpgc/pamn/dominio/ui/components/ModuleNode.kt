package es.ulpgc.pamn.dominio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import es.ulpgc.pamn.dominio.ui.theme.TextoSobreOscuro

// Medidas del nodo, para que el camino (CoursePath) sepa centrarlo y dejar sitio entre filas.
object ModuleNodeDefaults {
    val CircleSize = 56.dp
    val Width = 104.dp
}

// Un módulo del camino: círculo con el color y el icono de su estado y el título debajo.
// Sin estado: pinta lo que le dicen y avisa con onClick (también los bloqueados, para decir qué falta).
@Composable
fun ModuleNode(
    title: String,
    state: ModuleState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = moduleNodeStyle(state)
    val stateText = state.label().asString()
    // Todo el nodo (círculo + título) es una sola zona táctil: más grande que los 48 dp mínimos,
    // y TalkBack lo lee de una vez: «Grafos, frontera, botón, doble toque para practicar».
    Column(
        modifier = modifier
            .width(ModuleNodeDefaults.Width)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = state.actionLabel().asString(), onClick = onClick)
            .semantics { stateDescription = stateText }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(ModuleNodeDefaults.CircleSize),
            shape = CircleShape,
            color = style.container,
            contentColor = style.content,
            border = style.border,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Decorativo: el estado ya lo dice stateDescription.
                Icon(painterResource(style.icon), contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            text = title,
            // Con el color del fondo: tapa las líneas del camino que salen del círculo hacia abajo.
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            // 3 líneas caben en el alto de fila del camino (CourseMap) y no se corta «… (DFS)».
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Cómo se ve cada estado. Tabla de un solo sitio: cambiar un color es cambiar una línea.
private data class ModuleNodeStyle(
    val container: Color,
    val content: Color,
    val border: BorderStroke?,
    @DrawableRes val icon: Int,
)

@Composable
private fun moduleNodeStyle(state: ModuleState): ModuleNodeStyle {
    val colors = MaterialTheme.colorScheme
    return when (state) {
        // Mismo morado que las ilustraciones; con el blanco encima contrasta en claro y en oscuro.
        ModuleState.MASTERED -> ModuleNodeStyle(EstadoDominado, TextoSobreOscuro, null, R.drawable.ic_check)
        ModuleState.FRONTIER -> ModuleNodeStyle(colors.tertiary, colors.onTertiary, null, R.drawable.ic_play)
        ModuleState.LOCKED -> ModuleNodeStyle(
            container = colors.surface,
            content = colors.onSurfaceVariant,
            border = BorderStroke(1.5.dp, colors.outline),
            icon = R.drawable.ic_lock,
        )
    }
}

// Textos para TalkBack (y para el camino, que los reutiliza).
internal fun ModuleState.label(): UiText = when (this) {
    ModuleState.MASTERED -> UiText.of(R.string.module_state_mastered)
    ModuleState.FRONTIER -> UiText.of(R.string.module_state_frontier)
    ModuleState.LOCKED -> UiText.of(R.string.module_state_locked)
}

internal fun ModuleState.actionLabel(): UiText = when (this) {
    ModuleState.MASTERED -> UiText.of(R.string.module_action_review)
    ModuleState.FRONTIER -> UiText.of(R.string.module_action_practice)
    ModuleState.LOCKED -> UiText.of(R.string.module_action_missing)
}

@LightDarkPreviews
@Composable
private fun ModuleNodePreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModuleNode("Conjuntos", ModuleState.MASTERED, onClick = {})
                ModuleNode("Grafos", ModuleState.FRONTIER, onClick = {})
                ModuleNode("Búsqueda en profundidad (DFS)", ModuleState.LOCKED, onClick = {})
            }
        }
    }
}
