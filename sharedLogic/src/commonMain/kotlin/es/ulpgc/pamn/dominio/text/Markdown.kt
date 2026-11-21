package es.ulpgc.pamn.dominio.text

// Markdown básico, sin librerías (decisión 2 de cursos-backend-app.md): lo justo para las lecciones
// y explicaciones de los cursos. Funciones puras: texto → bloques y trozos con estilo. La app los
// convierte en AnnotatedString; iOS podría hacer lo mismo con su propio texto con estilo.

enum class InlineStyle { BOLD, ITALIC, CODE }

// Un trozo de texto con sus estilos.
data class TextSpan(val text: String, val styles: Set<InlineStyle> = emptySet())

sealed interface MarkdownBlock {
    data class Paragraph(val spans: List<TextSpan>) : MarkdownBlock

    // Un elemento de lista: `marker` es «•» o el número («1.»).
    data class ListItem(val marker: String, val spans: List<TextSpan>) : MarkdownBlock
}

private val BULLET = Regex("""^\s*[-*]\s+(.*)$""")
private val NUMBERED = Regex("""^\s*(\d+)\.\s+(.*)$""")

// Una línea en blanco separa párrafos; un salto de línea dentro de un párrafo es un espacio.
// Cada línea de lista («- …» o «1. …») es su propio bloque.
fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()

    fun closeParagraph() {
        if (paragraph.isNotEmpty()) blocks += MarkdownBlock.Paragraph(parseInline(paragraph.joinToString(" ")))
        paragraph.clear()
    }

    for (line in text.lines()) {
        val bullet = BULLET.find(line)
        val numbered = NUMBERED.find(line)
        when {
            line.isBlank() -> closeParagraph()
            bullet != null -> { closeParagraph(); blocks += MarkdownBlock.ListItem("•", parseInline(bullet.groupValues[1])) }
            numbered != null -> {
                closeParagraph()
                blocks += MarkdownBlock.ListItem("${numbered.groupValues[1]}.", parseInline(numbered.groupValues[2]))
            }
            else -> paragraph += line.trim()
        }
    }
    closeParagraph()
    return blocks
}

// `código` | **negrita** | *cursiva*. La cursiva no puede empezar ni acabar en espacio: así un
// «a * b» suelto no es formato. Lo que hay dentro de la negrita se vuelve a leer (puede tener cursiva).
private val INLINE = Regex("""`([^`]+)`|\*\*(.+?)\*\*(?!\*)|\*([^*\s](?:[^*]*[^*\s])?)\*""")

fun parseInline(text: String, styles: Set<InlineStyle> = emptySet()): List<TextSpan> {
    val spans = mutableListOf<TextSpan>()
    var position = 0
    for (match in INLINE.findAll(text)) {
        if (match.range.first > position) spans += TextSpan(text.substring(position, match.range.first), styles)
        val (code, bold, italic) = match.destructured
        spans += when {
            code.isNotEmpty() -> listOf(TextSpan(code, styles + InlineStyle.CODE))
            bold.isNotEmpty() -> parseInline(bold, styles + InlineStyle.BOLD)
            else -> parseInline(italic, styles + InlineStyle.ITALIC)
        }
        position = match.range.last + 1
    }
    if (position < text.length) spans += TextSpan(text.substring(position), styles)
    return spans
}
