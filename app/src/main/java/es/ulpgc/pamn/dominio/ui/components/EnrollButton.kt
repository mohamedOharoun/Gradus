package es.ulpgc.pamn.dominio.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Botón de inscribirse en un curso. Solo pinta el estado que recibe y avisa con onEnroll: no navega
// ni sabe en qué pantalla está, así que se puede usar en cualquier sitio. Reutiliza PrimaryButton
// (56 dp de alto, colores y tipografía del tema).
@Composable
fun EnrollButton(
    state: EnrollmentState,
    onEnroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryButton(
        text = stringResource(state.labelRes()),
        onClick = onEnroll,
        modifier = modifier,
        enabled = state == EnrollmentState.NotEnrolled,
    )
}

// Mientras no se sabe, se ve como antes de inscribirse (el botón no cambia de tamaño al cargar y
// nunca enseña «Inscrito» por error), pero desactivado.
@StringRes
private fun EnrollmentState.labelRes(): Int = when (this) {
    EnrollmentState.Unknown, EnrollmentState.NotEnrolled -> R.string.enroll_action
    EnrollmentState.Enrolling -> R.string.enroll_enrolling
    EnrollmentState.Enrolled -> R.string.enroll_enrolled
}

@LightDarkPreviews
@Composable
private fun EnrollButtonPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EnrollButton(state = EnrollmentState.Unknown, onEnroll = {})
                EnrollButton(state = EnrollmentState.NotEnrolled, onEnroll = {})
                EnrollButton(state = EnrollmentState.Enrolling, onEnroll = {})
                EnrollButton(state = EnrollmentState.Enrolled, onEnroll = {})
            }
        }
    }
}
