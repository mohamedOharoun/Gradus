package es.ulpgc.pamn.dominio.text

import es.ulpgc.pamn.dominio.text.InlineStyle.BOLD
import es.ulpgc.pamn.dominio.text.InlineStyle.CODE
import es.ulpgc.pamn.dominio.text.InlineStyle.ITALIC
import kotlin.test.Test
import kotlin.test.assertEquals

// Markdown básico de lecciones y explicaciones (decisión 2): negrita, cursiva, código en línea,
// párrafos y listas. Lo demás se queda como texto.
class MarkdownTest {

    // ── Dentro de una línea ──

    @Test
    fun `plain text is a single span`() {
        assertEquals(listOf(TextSpan("Un grafo")), parseInline("Un grafo"))
    }

    @Test
    fun `double asterisks are bold`() {
        assertEquals(
            listOf(TextSpan("Un "), TextSpan("grafo", setOf(BOLD)), TextSpan(" tiene vértices")),
            parseInline("Un **grafo** tiene vértices"),
        )
    }

    @Test
    fun `single asterisks are italic`() {
        assertEquals(listOf(TextSpan("BFS ("), TextSpan("breadth-first", setOf(ITALIC)), TextSpan(")")), parseInline("BFS (*breadth-first*)"))
    }

    @Test
    fun `backticks are code, and nothing inside them is formatted`() {
        assertEquals(listOf(TextSpan("Usa "), TextSpan("**x**", setOf(CODE))), parseInline("Usa `**x**`"))
    }

    @Test
    fun `italic can go inside bold`() {
        assertEquals(
            listOf(TextSpan("muy ", setOf(BOLD)), TextSpan("rápido", setOf(BOLD, ITALIC))),
            parseInline("**muy *rápido***"),
        )
    }

    // Un asterisco suelto (p. ej. en una multiplicación) no es formato.
    @Test
    fun `unclosed or spaced markers stay as text`() {
        assertEquals(listOf(TextSpan("a * b y **c")), parseInline("a * b y **c"))
    }

    @Test
    fun `an empty text has no spans`() {
        assertEquals(emptyList(), parseInline(""))
    }

    // ── Bloques ──

    @Test
    fun `a blank line separates paragraphs and single line breaks are joined`() {
        assertEquals(
            listOf(MarkdownBlock.Paragraph(parseInline("Uno y dos")), MarkdownBlock.Paragraph(parseInline("Tres"))),
            parseMarkdown("Uno\ny dos\n\nTres"),
        )
    }

    @Test
    fun `dashes are bullet items`() {
        assertEquals(
            listOf(
                MarkdownBlock.Paragraph(parseInline("Hay dos:")),
                MarkdownBlock.ListItem("•", parseInline("**Matriz**: O(V²)")),
                MarkdownBlock.ListItem("•", parseInline("Lista")),
            ),
            parseMarkdown("Hay dos:\n\n- **Matriz**: O(V²)\n- Lista"),
        )
    }

    @Test
    fun `numbers with a dot are numbered items`() {
        assertEquals(
            listOf(MarkdownBlock.ListItem("1.", parseInline("Encolar s")), MarkdownBlock.ListItem("2.", parseInline("Sacar u"))),
            parseMarkdown("1. Encolar s\n2. Sacar u"),
        )
    }

    @Test
    fun `the lesson of the example course is read in blocks`() {
        val lesson = "El **recorrido** explora por capas.\n\nBFS usa una **cola**:\n\n1. Encolar s.\n2. Sacar u.\n\nAsí nunca entra dos veces."
        assertEquals(5, parseMarkdown(lesson).size)
    }
}
