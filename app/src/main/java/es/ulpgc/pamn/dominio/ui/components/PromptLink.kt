package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Pregunta + enlace centrados, como «¿Ya tienes cuenta? Inicia sesión».
// Solo el enlace es pulsable (TextButton: 48 dp de alto).
@Composable
fun PromptLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onClick) {
            Text(action, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@LightDarkPreviews
@Composable
private fun PromptLinkPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PromptLink(prompt = "¿Ya tienes cuenta?", action = "Inicia sesión", onClick = {})
        }
    }
}
