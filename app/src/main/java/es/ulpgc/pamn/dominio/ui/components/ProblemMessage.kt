package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Un aviso centrado: título, texto (opcional) y «Reintentar» (solo si se pasa onRetry).
// Sirve para problemas y para listas vacías. Sin estado: no decide qué hacer al reintentar.
@Composable
fun ProblemMessage(
    title: String,
    modifier: Modifier = Modifier,
    body: String = "",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        // liveRegion: TalkBack lee el aviso en cuanto aparece (como CourseProblemContent).
        modifier = modifier.widthIn(max = 400.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body.isNotBlank()) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (onRetry != null) {
            RetryButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// «Reintentar» con zona táctil de 48 dp; lo usan ProblemMessage y el pie de la lista de Buscar.
@Composable
fun RetryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.action_retry))
    }
}

@LightDarkPreviews
@Composable
private fun ProblemMessagePreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                // Con texto y «Reintentar».
                ProblemMessage(
                    title = "Sin conexión",
                    body = "No se ha podido buscar. Comprueba la conexión y vuelve a intentarlo.",
                    onRetry = {},
                )
                // Sin botón y sin texto (lista vacía).
                ProblemMessage(title = "Todavía no hay cursos publicados")
            }
        }
    }
}
