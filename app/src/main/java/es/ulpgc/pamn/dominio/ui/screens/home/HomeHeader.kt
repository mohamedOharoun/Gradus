package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.InitialsAvatar
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import java.time.LocalDate

// La cabecera de Inicio (Figma «Inicio»): la fecha de hoy, «Hola, Ana» y las iniciales.
// `date` llega por parámetro (state hoisting): la pantalla pasa hoy, las previews y los tests, un día fijo.
// El saludo es el título de la pantalla para TalkBack (heading); las iniciales no se leen (InitialsAvatar).
@Composable
fun HomeHeader(date: LocalDate, name: String?, initials: String, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = homeDateText(date, stringResource(R.string.home_date_pattern), locale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = greetingText(name).asString(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
        }
        InitialsAvatar(initials, Modifier.padding(start = 16.dp))
    }
}

@LightDarkPreviews
@Composable
private fun HomeHeaderPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                HomeHeader(date = LocalDate.of(2026, 10, 28), name = "Nombre", initials = "NM")
                // Sin nombre: solo «Hola».
                HomeHeader(date = LocalDate.of(2026, 11, 1), name = null, initials = "?")
            }
        }
    }
}
