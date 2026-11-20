package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.theme.BordeFuerte
import es.ulpgc.pamn.dominio.ui.theme.EstadoFronteraOscuro
import es.ulpgc.pamn.dominio.ui.theme.FondoEstadoNeutro
import es.ulpgc.pamn.dominio.ui.theme.FondoNoche
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import es.ulpgc.pamn.dominio.ui.theme.MarcaLila

// Proporción del dibujo en Figma (282 × 268).
private const val ASPECT_RATIO = 282f / 268f

// Peldaños de la escalera, en fracciones del lienzo (0..1). El último es el módulo frontera.
private val steps = listOf(
    Offset(0.22f, 0.78f),
    Offset(0.41f, 0.60f),
    Offset(0.60f, 0.42f),
    Offset(0.78f, 0.23f),
)

// Ilustración de la bienvenida: módulos dominados que suben, peldaño a peldaño, hasta uno nuevo.
@Composable
fun WelcomeIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(ASPECT_RATIO)) {
        drawRect(FondoNoche)
        val points = steps.map { Offset(it.x * size.width, it.y * size.height) }
        drawStairs(points)
        points.dropLast(1).forEach { drawNode(it, size.width * 0.06f, isFrontier = false) }
        drawNode(points.last(), size.width * 0.08f, isFrontier = true)
    }
}

// Camino en escalones: de cada nodo, horizontal hasta debajo del siguiente y después hacia arriba.
private fun DrawScope.drawStairs(points: List<Offset>) {
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.zipWithNext { from, to ->
            lineTo(to.x, from.y)
            lineTo(to.x, to.y)
        }
    }
    drawPath(
        path = path,
        color = MarcaLila.copy(alpha = 0.5f),
        style = Stroke(width = size.width * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawNode(center: Offset, radius: Float, isFrontier: Boolean) {
    val fill = if (isFrontier) EstadoFronteraOscuro else FondoEstadoNeutro
    drawCircle(fill, radius, center)
    drawCircle(BordeFuerte, radius, center, style = Stroke(width = radius * 0.12f))
}

@LightDarkPreviews
@Composable
private fun WelcomeIllustrationPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WelcomeIllustration(modifier = Modifier.padding(24.dp).width(282.dp))
        }
    }
}
