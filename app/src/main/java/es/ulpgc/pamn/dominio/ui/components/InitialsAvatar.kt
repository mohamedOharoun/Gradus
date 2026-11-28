package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Círculo morado con las iniciales («NM»). Lo usan Ajustes (ProfileRow) e Inicio (HomeHeader); antes era
// privado de ProfileRow (docs/tareas/inicio.md, paso 4a). Decorativo: TalkBack no lo lee, porque al lado
// siempre está el nombre («Hola, Ana» o el nombre completo).
@Composable
fun InitialsAvatar(initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@LightDarkPreviews
@Composable
private fun InitialsAvatarPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(16.dp)) {
                InitialsAvatar("NM")
                InitialsAvatar("A", Modifier.padding(start = 16.dp))
            }
        }
    }
}
