package es.ulpgc.pamn.dominio.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Muestrario del tema: solo sirve para ver en la preview que colores y fuentes salen bien.
@Composable
private fun ThemeSample(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Aprende paso a paso", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Cada concepto se desbloquea cuando dominas los anteriores.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Kotlin desde cero", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "12 de 20 conceptos dominados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Crear cuenta")
            }
        }
    }
}

// Las dos previews no pasan darkTheme: el tema lo deduce del uiMode, como en el móvil.
@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeSamplePreview() {
    GradusTheme {
        ThemeSample()
    }
}
