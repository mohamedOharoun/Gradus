package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.components.BackButton
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// La pantalla del curso mientras no hay camino: descargando o con un problema.
// Siempre con «atrás», para no quedarse atrapado si el servidor no responde.

data class CourseProblemText(val title: String, val body: String)

fun CourseProblem.toText(): CourseProblemText = when (this) {
    CourseProblem.NOT_FOUND ->
        CourseProblemText("Este curso no existe", "Puede que su autor lo haya retirado.")
    CourseProblem.UNREACHABLE ->
        CourseProblemText("Sin conexión", "No se ha podido descargar el curso. Comprueba la conexión y vuelve a intentarlo.")
    CourseProblem.UNSUPPORTED_FORMAT ->
        CourseProblemText("Actualiza Gradus", "Este curso usa un formato más nuevo que esta versión de la app.")
    CourseProblem.SERVER_FAILURE ->
        CourseProblemText("Algo ha fallado", "El servidor no ha respondido bien. Vuelve a intentarlo en un momento.")
}

@Composable
fun CourseLoadingContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    CourseStatusFrame(onBack, modifier) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = "Descargando el curso" })
    }
}

@Composable
fun CourseProblemContent(problem: CourseProblem, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val text = problem.toText()
    CourseStatusFrame(onBack, modifier) {
        // liveRegion: TalkBack lee el problema en cuanto aparece.
        Column(
            modifier = Modifier.widthIn(max = 400.dp).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                text.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (problem.canRetry) {
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp)) {
                    Text("Reintentar")
                }
            }
        }
    }
}

// Atrás arriba a la izquierda y el contenido en el centro.
@Composable
private fun CourseStatusFrame(onBack: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            BackButton(onClick = onBack)
            Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@LightDarkPreviews
@Composable
private fun CourseLoadingPreview() {
    GradusTheme { CourseLoadingContent(onBack = {}) }
}

@LightDarkPreviews
@Composable
private fun CourseProblemPreview() {
    GradusTheme { CourseProblemContent(CourseProblem.UNREACHABLE, onRetry = {}, onBack = {}) }
}

@LightDarkPreviews
@Composable
private fun CourseNotFoundPreview() {
    GradusTheme { CourseProblemContent(CourseProblem.NOT_FOUND, onRetry = {}, onBack = {}) }
}
