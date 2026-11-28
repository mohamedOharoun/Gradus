package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Cabecera del camino de un curso: atrás, título y progreso («3 de 10 dominados» y una barra).
// Va al principio del camino y se desplaza con él.
@Composable
fun CourseHeader(
    title: String,
    masteredCount: Int,
    total: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            // Edge-to-edge: deja sitio a la barra de estado (y a los lados, en horizontal).
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackButton(onClick = onBack)
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            stringResource(R.string.course_progress, masteredCount, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Misma información que el texto de encima: TalkBack ya la lee ahí.
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else masteredCount.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
            color = EstadoDominado,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            drawStopIndicator = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun CourseHeaderPreview() {
    GradusTheme {
        CourseHeader(title = "Estructuras de datos", masteredCount = 3, total = 10, onBack = {})
    }
}
