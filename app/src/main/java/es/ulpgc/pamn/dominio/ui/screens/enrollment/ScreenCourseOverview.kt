package es.ulpgc.pamn.dominio.ui.screens.enrollment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.EnrollButton
import es.ulpgc.pamn.dominio.ui.components.EnrollmentState
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Página de RELLENO del curso: se sustituirá por la definitiva. Al borrarla se borran también
// PathButton y su texto; EnrollButton y EnrollmentViewModel no cambian. No conoce ViewModels ni
// NavController: recibe el estado y avisa con lambdas (state hoisting).
@Composable
fun ScreenCourseOverview(
    enrollment: EnrollmentState,
    onEnroll: () -> Unit,
    onGoToPath: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fondo del tema y márgenes de las barras del sistema (la app dibuja de borde a borde).
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormTopBar(title = stringResource(R.string.course_overview_title), onBack = onBack)
            Text(
                stringResource(R.string.course_overview_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            EnrollButton(state = enrollment, onEnroll = onEnroll)
            PathButton(enrolled = enrollment == EnrollmentState.Enrolled, onClick = onGoToPath)
        }
    }
}

// Siempre activo (el camino se ve sin inscripción). Inscrito, estilo principal; si no, secundario
// (OutlinedButton, el que ya usa el proyecto para lo secundario) con el mismo tamaño que PrimaryButton.
@Composable
private fun PathButton(enrolled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.course_overview_go_to_path)
    if (enrolled) {
        PrimaryButton(text = label, onClick = onClick)
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(label)
        }
    }
}

@LightDarkPreviews
@Composable
private fun ScreenCourseOverviewNotEnrolledPreview() {
    GradusTheme {
        ScreenCourseOverview(EnrollmentState.NotEnrolled, onEnroll = {}, onGoToPath = {}, onBack = {})
    }
}

@LightDarkPreviews
@Composable
private fun ScreenCourseOverviewEnrolledPreview() {
    GradusTheme {
        ScreenCourseOverview(EnrollmentState.Enrolled, onEnroll = {}, onGoToPath = {}, onBack = {})
    }
}
