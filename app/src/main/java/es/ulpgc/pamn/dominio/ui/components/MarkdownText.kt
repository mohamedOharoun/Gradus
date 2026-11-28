package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.text.InlineStyle
import es.ulpgc.pamn.dominio.text.MarkdownBlock
import es.ulpgc.pamn.dominio.text.TextSpan
import es.ulpgc.pamn.dominio.text.parseMarkdown
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Texto en Markdown básico (lecciones y explicaciones): párrafos, listas, negrita, cursiva y
// código. Quien lo lee es parseMarkdown (sharedLogic); aquí solo se pinta.
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    // remember(text): solo se vuelve a leer el Markdown si cambia el texto, no en cada repintado.
    val blocks = remember(text) { parseMarkdown(text) }
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> Text(block.spans.toAnnotated(codeBackground), style = style, color = color)
                // mergeDescendants: TalkBack lee el número (o el punto) y el texto como una sola cosa.
                is MarkdownBlock.ListItem -> Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(block.marker, style = style, color = color, modifier = Modifier.widthIn(min = 16.dp))
                    Text(block.spans.toAnnotated(codeBackground), style = style, color = color)
                }
            }
        }
    }
}

// AnnotatedString: un texto con estilos distintos en cada trozo (como un HTML muy pequeño).
private fun List<TextSpan>.toAnnotated(codeBackground: Color): AnnotatedString = buildAnnotatedString {
    forEach { span -> withStyle(span.styles.toSpanStyle(codeBackground)) { append(span.text) } }
}

private fun Set<InlineStyle>.toSpanStyle(codeBackground: Color) = SpanStyle(
    fontWeight = if (InlineStyle.BOLD in this) FontWeight.Bold else null,
    fontStyle = if (InlineStyle.ITALIC in this) FontStyle.Italic else null,
    fontFamily = if (InlineStyle.CODE in this) FontFamily.Monospace else null,
    background = if (InlineStyle.CODE in this) codeBackground else Color.Unspecified,
)

@LightDarkPreviews
@Composable
private fun MarkdownTextPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MarkdownText(
                "El **recorrido en anchura** (BFS, *breadth-first search*) usa una cola:\n\n" +
                    "1. Encolar `s` y marcarlo.\n2. Sacar un vértice **u**.\n\n- Cuesta *O(V + E)*.\n- Calcula distancias.",
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
