package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Posiciones de los nodos en fracciones del lienzo (medidas de las capturas de Figma).
private val prerequisites = listOf(Offset(0.22f, 0.15f), Offset(0.79f, 0.15f))
private val center = Offset(0.5f, 0.5f)
private val nextModules = listOf(Offset(0.16f, 0.84f), Offset(0.85f, 0.84f))
private const val NODE_RADIUS = 0.09f

// Grafo en X del registro: dos prerrequisitos arriba, un módulo en medio y dos más abajo.
// `masteredCount` (0, 1 o 2) dice cuántos prerrequisitos están dominados (morado); los que no,
// están en la frontera (naranja). Con los dos dominados, el del medio pasa a ser frontera.
@Composable
fun UnlockGraphIllustration(masteredCount: Int, modifier: Modifier = Modifier) {
    // MaterialTheme solo se puede leer en un composable, no dentro del Canvas: lo leemos antes.
    val colors = NodeColors(
        mastered = EstadoDominado,
        frontier = MaterialTheme.colorScheme.tertiary,
        locked = MaterialTheme.colorScheme.surface,
        line = MaterialTheme.colorScheme.outline,
    )
    Canvas(modifier = modifier.size(120.dp)) {
        val radius = size.width * NODE_RADIUS
        val middle = center.scaled(size.width)
        (prerequisites + nextModules).forEach {
            drawLine(colors.line, it.scaled(size.width), middle, strokeWidth = 1.5.dp.toPx())
        }
        prerequisites.forEachIndexed { index, position ->
            val fill = if (index < masteredCount) colors.mastered else colors.frontier
            drawFilledNode(position.scaled(size.width), radius, fill)
        }
        if (masteredCount >= prerequisites.size) {
            drawFilledNode(middle, radius, colors.frontier)
        } else {
            drawLockedNode(middle, radius, colors)
        }
        nextModules.forEach { drawLockedNode(it.scaled(size.width), radius, colors) }
    }
}

private data class NodeColors(val mastered: Color, val frontier: Color, val locked: Color, val line: Color)

// El lienzo es cuadrado: basta con multiplicar las dos fracciones por el lado.
private fun Offset.scaled(side: Float) = Offset(x * side, y * side)

private fun DrawScope.drawFilledNode(center: Offset, radius: Float, color: Color) {
    drawCircle(color, radius, center)
}

private fun DrawScope.drawLockedNode(center: Offset, radius: Float, colors: NodeColors) {
    drawCircle(colors.locked, radius, center)
    drawCircle(colors.line, radius, center, style = Stroke(width = 1.5.dp.toPx()))
}

@LightDarkPreviews
@Composable
private fun UnlockGraphIllustrationPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                (0..2).forEach { UnlockGraphIllustration(masteredCount = it) }
            }
        }
    }
}
