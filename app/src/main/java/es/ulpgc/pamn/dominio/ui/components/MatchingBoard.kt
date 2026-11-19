package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MatchingDraft
import es.ulpgc.pamn.dominio.ui.theme.EstadoDominado
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Hueco entre las dos columnas, por donde van las flechas.
private val ColumnGap = 56.dp
private val RowGap = 12.dp
private val CardShape = RoundedCornerShape(12.dp)

// Emparejar: la izquierda en el orden de la pregunta, la derecha barajada (`draft.rightOrder`).
// Se toca una tarjeta de cada lado y aparece una flecha entre las dos. Sin estado: pinta el
// borrador que le dan y avisa de cada toque. Con `checked`, las flechas salen moradas si la
// pareja es correcta y rojas si no, y ya no se puede tocar.
@Composable
fun MatchingBoard(
    question: Matching,
    draft: MatchingDraft,
    checked: Boolean,
    onLeftClick: (index: Int) -> Unit,
    onRightClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pairs = question.pairs
    // Dónde quedó cada tarjeta. Lo rellena el Layout al colocarlas y lo lee drawBehind justo
    // después, en el mismo fotograma: no hace falta estado (ni un segundo fotograma).
    val positions = remember(pairs.size) { BoardPositions(pairs.size) }
    val arrows = ArrowColors(pending = MaterialTheme.colorScheme.primary, right = EstadoDominado, wrong = MaterialTheme.colorScheme.error)
    val leftOf = draft.pairs.entries.associate { (left, right) -> right to left } // derecha → izquierda

    Layout(
        modifier = modifier.drawBehind {
            draft.pairs.forEach { (left, right) ->
                val color = when {
                    !checked -> arrows.pending
                    question.isRightFor(left, right) -> arrows.right
                    else -> arrows.wrong
                }
                drawArrow(positions.left[left], positions.right[right], color)
            }
        },
        content = {
            pairs.forEachIndexed { left, pair ->
                val partner = draft.pairs[left]
                MatchingCard(
                    text = pair.left,
                    look = cardLook(selected = draft.pendingLeft == left, partner != null, checked, partner?.let { question.isRightFor(left, it) }),
                    description = partner?.let { "unido con ${pairs[it].right}" } ?: "sin unir",
                    enabled = !checked,
                    onClick = { onLeftClick(left) },
                )
            }
            draft.rightOrder.forEach { right ->
                val partner = leftOf[right]
                MatchingCard(
                    text = pairs[right].right,
                    look = cardLook(selected = draft.pendingRight == right, partner != null, checked, partner?.let { question.isRightFor(it, right) }),
                    description = partner?.let { "unido con ${pairs[it].left}" } ?: "sin unir",
                    enabled = !checked,
                    onClick = { onRightClick(right) },
                )
            }
        },
    ) { measurables, constraints ->
        val count = pairs.size
        val gap = ColumnGap.roundToPx()
        val rowGap = RowGap.roundToPx()
        val columnWidth = ((constraints.maxWidth - gap) / 2).coerceAtLeast(0)
        val lefts = measurables.take(count)
        val rights = measurables.drop(count)
        // Cada fila mide lo que la más alta de sus dos tarjetas: así quedan alineadas.
        val rowHeights = (0 until count).map { row ->
            maxOf(lefts[row].minIntrinsicHeight(columnWidth), rights[row].minIntrinsicHeight(columnWidth))
        }
        val placeables = (0 until count).map { row ->
            val fixed = Constraints.fixed(columnWidth, rowHeights[row])
            lefts[row].measure(fixed) to rights[row].measure(fixed)
        }
        val height = rowHeights.sum() + rowGap * (count - 1).coerceAtLeast(0)
        layout(constraints.maxWidth, height) {
            var y = 0
            placeables.forEachIndexed { row, (left, right) ->
                val rightX = columnWidth + gap
                left.place(0, y)
                right.place(rightX, y)
                positions.left[row] = Rect(0f, y.toFloat(), columnWidth.toFloat(), (y + rowHeights[row]).toFloat())
                positions.right[draft.rightOrder[row]] =
                    Rect(rightX.toFloat(), y.toFloat(), (rightX + columnWidth).toFloat(), (y + rowHeights[row]).toFloat())
                y += rowHeights[row] + rowGap
            }
        }
    }
}

// left[i]: tarjeta i de la izquierda. right[j]: tarjeta de la pareja j (esté en la fila que esté).
private class BoardPositions(count: Int) {
    val left = Array(count) { Rect.Zero }
    val right = Array(count) { Rect.Zero }
}

private data class ArrowColors(val pending: Color, val right: Color, val wrong: Color)

// Curva del borde derecho de una tarjeta al borde izquierdo de la otra, con punta de flecha.
private fun DrawScope.drawArrow(from: Rect, to: Rect, color: Color) {
    val head = 9.dp.toPx()
    val start = Offset(from.right, from.center.y)
    val end = Offset(to.left - 2.dp.toPx(), to.center.y)
    val middleX = (start.x + end.x) / 2
    val curve = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(middleX, start.y, middleX, end.y, end.x - head, end.y)
    }
    drawPath(curve, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    val tip = Path().apply {
        moveTo(end.x, end.y)
        lineTo(end.x - head, end.y - head * 0.6f)
        lineTo(end.x - head, end.y + head * 0.6f)
        close()
    }
    drawPath(tip, color)
}

// Cómo se ve una tarjeta. La corrección manda sobre todo lo demás.
private data class CardLook(val container: Color, val content: Color, val border: BorderStroke, val state: String?)

@Composable
private fun cardLook(selected: Boolean, joined: Boolean, checked: Boolean, rightPair: Boolean?): CardLook {
    val colors = MaterialTheme.colorScheme
    return when {
        checked && rightPair == true -> CardLook(colors.surface, colors.onSurface, BorderStroke(2.dp, EstadoDominado), "correcta")
        checked && rightPair == false -> CardLook(colors.errorContainer, colors.onErrorContainer, BorderStroke(2.dp, colors.error), "incorrecta")
        selected -> CardLook(colors.primaryContainer, colors.onPrimaryContainer, BorderStroke(2.dp, colors.primary), "seleccionado")
        joined -> CardLook(colors.surface, colors.onSurface, BorderStroke(2.dp, colors.primary), null)
        else -> CardLook(colors.surface, colors.onSurface, BorderStroke(1.dp, colors.outlineVariant), null)
    }
}

// Una tarjeta del tablero. TalkBack la lee como botón con su texto y su estado, por ejemplo
// «BFS, unido con Cola, botón» o «DFS, seleccionado, sin unir, botón».
@Composable
private fun MatchingCard(
    text: String,
    look: CardLook,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = 56.dp)
            // El toque ya lo declara Surface(onClick); aquí solo el papel y el estado.
            .semantics {
                role = Role.Button
                stateDescription = listOfNotNull(look.state, description).joinToString(", ")
            },
        shape = CardShape,
        color = look.container,
        contentColor = look.content,
        border = look.border,
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

// ── Previews ──

private val previewQuestion = Matching(
    id = "q3", conceptId = "bfs", prompt = "Une cada algoritmo con lo que usa",
    pairs = listOf(MatchPair("BFS", "Cola"), MatchPair("DFS", "Pila"), MatchPair("Dijkstra", "Cola de prioridad")),
)
private val previewOrder = listOf(1, 2, 0) // a la derecha: Pila, Cola de prioridad, Cola

@Composable
private fun PreviewBoard(draft: MatchingDraft, checked: Boolean = false) {
    MatchingBoard(previewQuestion, draft, checked, onLeftClick = {}, onRightClick = {})
}

@LightDarkPreviews
@Composable
private fun MatchingBoardPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Sin unir.
                PreviewBoard(MatchingDraft(previewOrder))
                // A medias: BFS unido con Cola y DFS esperando pareja.
                PreviewBoard(MatchingDraft(previewOrder, pairs = mapOf(0 to 0), pendingLeft = 1))
                // Comprobado, todo bien.
                PreviewBoard(MatchingDraft(previewOrder, pairs = mapOf(0 to 0, 1 to 1, 2 to 2)), checked = true)
                // Comprobado, con BFS y Dijkstra cambiados.
                PreviewBoard(MatchingDraft(previewOrder, pairs = mapOf(0 to 2, 1 to 1, 2 to 0)), checked = true)
            }
        }
    }
}
