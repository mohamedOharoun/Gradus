package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Barra superior de los formularios (Figma «Barra superior»): botón redondo de atrás y título centrado.
@Composable
fun FormTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        BackButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        // En Figma el título usa «Texto/Botón» (Sans Bold 16), que en el tema es labelLarge.
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Center))
    }
}

// Círculo de 44 dp como en Figma; al ser un Surface pulsable, Material amplía la zona táctil a 48 dp.
// Público para reutilizarlo en barras que no son de formulario (CourseHeader).
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
        }
    }
}

@LightDarkPreviews
@Composable
private fun FormTopBarPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FormTopBar(title = "Iniciar sesión", onBack = {}, modifier = Modifier.padding(20.dp))
        }
    }
}
