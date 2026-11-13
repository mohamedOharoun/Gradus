package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado
import es.ulpgc.pamn.dominio.ui.theme.EstadoFrontera
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Logotipo: un mini grafo (dos conceptos dominados que llevan a uno frontera) y el nombre.
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) { drawMiniGraph() }
        Text("Gradus", style = MaterialTheme.typography.headlineSmall)
    }
}

// Coordenadas en fracciones del lienzo (0..1), así el dibujo escala con el tamaño.
private fun DrawScope.drawMiniGraph() {
    val left = Offset(size.width * 0.25f, size.height * 0.28f)
    val right = Offset(size.width * 0.75f, size.height * 0.28f)
    val bottom = Offset(size.width * 0.5f, size.height * 0.75f)
    val stroke = size.width * 0.09f
    val radius = size.width * 0.16f

    drawLine(EstadoDominado, left, bottom, strokeWidth = stroke)
    drawLine(EstadoDominado, right, bottom, strokeWidth = stroke)
    drawCircle(EstadoDominado, radius, left)
    drawCircle(EstadoDominado, radius, right)
    drawCircle(EstadoFrontera, radius, bottom)
}

@LightDarkPreviews
@Composable
private fun BrandLogoPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BrandLogo(modifier = Modifier.padding(16.dp))
        }
    }
}
