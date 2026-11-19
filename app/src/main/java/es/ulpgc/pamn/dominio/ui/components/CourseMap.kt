package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.ConceptState
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.PathEdge
import es.ulpgc.pamn.dominio.course.PathNode
import es.ulpgc.pamn.dominio.course.pathLayout
import es.ulpgc.pamn.dominio.course.statesFor
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Alto de cada fila del camino: el nodo (círculo + título de dos líneas) y el hueco para las líneas.
private val RowHeight = 136.dp
private val TopPadding = 8.dp

// El camino de un curso: cada concepto donde dice `pathLayout()` y, detrás, las líneas de sus
// prerrequisitos. Sin estado y sin scroll propio: la pantalla que lo usa decide si se desplaza.
@Composable
fun CourseMap(
    course: Course,
    states: Map<String, ConceptState>,
    onConceptClick: (conceptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // remember(course): la distribución solo se recalcula si cambia el curso, no al cambiar el progreso.
    val path = remember(course) { course.pathLayout() }
    val lineColors = LineColors(unlocked = EstadoDominado, locked = MaterialTheme.colorScheme.outline)

    BoxWithConstraints(modifier.fillMaxWidth().height(RowHeight * path.rowCount + TopPadding)) {
        val geometry = MapGeometry(maxWidth)

        Canvas(Modifier.matchParentSize()) {
            path.edges
                .mapNotNull { edge ->
                    val from = path.node(edge.from) ?: return@mapNotNull null
                    val to = path.node(edge.to) ?: return@mapNotNull null
                    Line(geometry.pointsOf(edge, from, to), unlocked = states[edge.to] != ConceptState.LOCKED)
                }
                .sortedBy { it.unlocked } // las moradas encima de las grises
                .forEach { drawLine(it, lineColors) }
        }

        path.nodes.forEach { node ->
            val id = node.concept.id
            ConceptNode(
                title = node.concept.title,
                state = states[id] ?: ConceptState.LOCKED,
                onClick = { onConceptClick(id) },
                modifier = Modifier.offset(x = geometry.leftOf(node), y = geometry.rowTop(node)),
            )
        }
    }
}

// Pasa de las fracciones de `pathLayout()` a dp, con el ancho real del camino.
private class MapGeometry(private val width: Dp) {
    private val half = ConceptNodeDefaults.Width / 2

    // Sin salirse por los lados aunque una fila tenga muchos conceptos.
    private fun centerX(node: PathNode): Dp = (width * node.x).coerceIn(half, (width - half).coerceAtLeast(half))

    fun rowTop(node: PathNode): Dp = RowHeight * node.row + TopPadding
    fun leftOf(node: PathNode): Dp = centerX(node) - half

    // El círculo empieza 4 dp por debajo del borde del nodo (su padding vertical).
    private fun circleTop(node: PathNode): Dp = circleTop(node.row)
    private fun circleTop(row: Int): Dp = RowHeight * row + TopPadding + 4.dp

    // Del borde inferior de `from` al superior de `to`. Si cruza filas, en cada una baja en vertical
    // por su hueco (`via`) a la altura de los círculos de esa fila.
    fun pointsOf(edge: PathEdge, from: PathNode, to: PathNode): List<DpPoint> {
        val start = DpPoint(centerX(from), circleTop(from) + ConceptNodeDefaults.CircleSize)
        val end = DpPoint(centerX(to), circleTop(to))
        val crossings = edge.via.flatMapIndexed { index, x ->
            val top = circleTop(from.row + 1 + index)
            listOf(DpPoint(width * x, top), DpPoint(width * x, top + ConceptNodeDefaults.CircleSize))
        }
        return listOf(start) + crossings + end
    }
}

private data class DpPoint(val x: Dp, val y: Dp)
private data class Line(val points: List<DpPoint>, val unlocked: Boolean)
private data class LineColors(val unlocked: Color, val locked: Color)

// Curvas en S entre cada par de puntos (si tienen la misma x, la «S» es una recta). Pasa por detrás
// del título del nodo de arriba, que tiene fondo y la tapa.
private fun DrawScope.drawLine(line: Line, colors: LineColors) {
    val points = line.points.map { Offset(it.x.toPx(), it.y.toPx()) }
    val curve = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.zipWithNext { start, end ->
            val middleY = (start.y + end.y) / 2
            cubicTo(start.x, middleY, end.x, middleY, end.x, end.y)
        }
    }
    val dash = 6.dp.toPx()
    drawPath(
        path = curve,
        color = if (line.unlocked) colors.unlocked else colors.locked,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (line.unlocked) null else PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        ),
    )
}

@LightDarkPreviews
@Composable
private fun CourseMapPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CourseMap(
                course = demoCourse,
                states = demoCourse.statesFor(demoMastered),
                onConceptClick = {},
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
