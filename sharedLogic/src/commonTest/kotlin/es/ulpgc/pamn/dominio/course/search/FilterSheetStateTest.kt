package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CoursePage
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.SearchOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Tarea 7a de buscar-cursos.md: el estado de la hoja de filtros (borrador, sugerencias y «Ver todas»).
class FilterSheetStateTest {

    private fun course(id: String, vararg tags: String) = CourseSummary(
        id = id,
        version = 1,
        author = Author(id = "u1", name = "Ana Ruiz"),
        publishedAt = "2026-10-03T08:35:00Z",
        title = "Curso $id",
        description = "",
        language = "es",
        tags = tags.toList(),
        moduleCount = 10,
    )

    // grafos en 3 cursos, árboles en 2 y recursión en 1: caben todas en las 7 que se ven.
    private val results = listOf(
        course("c1", "grafos", "árboles", "recursión"),
        course("c2", "grafos", "árboles"),
        course("c3", "grafos"),
    )

    // Nueve etiquetas con un curso cada una: salen a..g y quedan h, i ocultas.
    private val manyTags = listOf(course("c1", "a", "b", "c", "d", "e", "f", "g", "h", "i"))

    private fun opened(applied: CourseQuery = CourseQuery.EMPTY) = FilterSheetState.open(applied, results)

    // ── open ──

    @Test
    fun `open copies the applied query into the draft`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        val state = FilterSheetState.open(applied, results)

        assertEquals(applied, state.draft)
    }

    @Test
    fun `open starts with the full list collapsed`() {
        assertFalse(opened().showingAll)
    }

    @Test
    fun `open puts the applied tags first in the suggestions`() {
        val state = opened(CourseQuery(tags = listOf("recursión")))

        assertEquals(listOf("recursión", "grafos", "árboles"), state.visibleTags)
    }

    @Test
    fun `open shows an applied tag even if the results do not have it`() {
        val state = opened(CourseQuery(tags = listOf("química")))

        assertEquals("química", state.visibleTags.first())
        assertTrue(state.isSelected("química"))
    }

    // ── toggle e isSelected ──

    @Test
    fun `toggle selects and unselects a tag`() {
        val selected = opened().toggle("grafos")
        assertTrue(selected.isSelected("grafos"))
        assertEquals(listOf("grafos"), selected.draft.tags)

        val unselected = selected.toggle("grafos")
        assertFalse(unselected.isSelected("grafos"))
        assertEquals(emptyList(), unselected.draft.tags)
    }

    @Test
    fun `toggle ignores case and extra spaces`() {
        val state = opened(CourseQuery(tags = listOf("grafos"))).toggle("  GRAFOS ")

        assertFalse(state.isSelected("grafos"))
        assertEquals(emptyList(), state.draft.tags)
    }

    @Test
    fun `isSelected ignores case and extra spaces`() {
        val state = opened().toggle("Grafos")

        assertTrue(state.isSelected("  grafos "))
    }

    @Test
    fun `isSelected is false for a tag that is not in the draft`() {
        assertFalse(opened().toggle("grafos").isSelected("árboles"))
    }

    @Test
    fun `toggle with a blank tag changes nothing`() {
        val state = opened()

        assertEquals(state, state.toggle("   "))
    }

    @Test
    fun `toggle does not touch the applied query`() {
        val applied = CourseQuery(tags = listOf("grafos"))

        opened(applied).toggle("árboles")

        assertEquals(CourseQuery(tags = listOf("grafos")), applied)
    }

    @Test
    fun `suggestions do not change when toggling`() {
        val state = opened()

        assertEquals(state.suggestions, state.toggle("recursión").suggestions)
        assertEquals(state.visibleTags, state.toggle("recursión").visibleTags)
    }

    // ── reset ──

    @Test
    fun `reset empties the tags and keeps the text`() {
        val state = opened(CourseQuery(text = "red", tags = listOf("grafos", "árboles")))

        assertEquals(CourseQuery(text = "red"), state.reset().draft)
    }

    @Test
    fun `reset does not touch the suggestions`() {
        val state = opened(CourseQuery(tags = listOf("recursión")))

        assertEquals(state.suggestions, state.reset().suggestions)
    }

    @Test
    fun `reset does not collapse the full list`() {
        val state = FilterSheetState.open(CourseQuery(tags = listOf("a")), manyTags).showAll()

        assertTrue(state.reset().showingAll)
    }

    @Test
    fun `canReset is false without tags and true with them`() {
        assertFalse(opened().canReset)
        assertTrue(opened().toggle("grafos").canReset)
        assertTrue(opened(CourseQuery(tags = listOf("grafos"))).canReset)
    }

    @Test
    fun `canReset is false after reset`() {
        assertFalse(opened(CourseQuery(tags = listOf("grafos"))).reset().canReset)
    }

    @Test
    fun `canReset ignores the text`() {
        assertFalse(opened(CourseQuery(text = "red")).canReset)
    }

    // ── Ver todas ──

    @Test
    fun `without hidden tags there is no see all`() {
        val state = opened()

        assertFalse(state.canShowAll)
        assertEquals(listOf("grafos", "árboles", "recursión"), state.visibleTags)
    }

    @Test
    fun `with hidden tags only the first seven are visible and see all is offered`() {
        val state = FilterSheetState.open(CourseQuery.EMPTY, manyTags)

        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g"), state.visibleTags)
        assertTrue(state.canShowAll)
    }

    @Test
    fun `showAll reveals the hidden tags and stops offering see all`() {
        val state = FilterSheetState.open(CourseQuery.EMPTY, manyTags).showAll()

        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g", "h", "i"), state.visibleTags)
        assertFalse(state.canShowAll)
        assertTrue(state.showingAll)
    }

    @Test
    fun `showAll twice gives the same state`() {
        val once = FilterSheetState.open(CourseQuery.EMPTY, manyTags).showAll()

        assertSame(once, once.showAll())
    }

    @Test
    fun `toggle keeps the full list open`() {
        val state = FilterSheetState.open(CourseQuery.EMPTY, manyTags).showAll().toggle("h")

        assertTrue(state.showingAll)
        assertEquals(9, state.visibleTags.size)
    }

    // ── queryToApply (decisión A) ──

    @Test
    fun `queryToApply returns the draft with the typed text`() {
        val state = opened().toggle("grafos")

        assertEquals(
            CourseQuery(text = "red", tags = listOf("grafos")),
            state.queryToApply(typedText = "red", applied = CourseQuery.EMPTY),
        )
    }

    @Test
    fun `queryToApply returns the new filters when the text did not change`() {
        val applied = CourseQuery(text = "red")
        val state = opened(applied).toggle("grafos")

        assertEquals(
            CourseQuery(text = "red", tags = listOf("grafos")),
            state.queryToApply(typedText = "red", applied = applied),
        )
    }

    @Test
    fun `queryToApply returns null when nothing changed`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        assertNull(opened(applied).queryToApply(typedText = "red", applied = applied))
    }

    @Test
    fun `queryToApply returns the query when only the typed text changed`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        assertEquals(
            CourseQuery(text = "redes", tags = listOf("grafos")),
            opened(applied).queryToApply(typedText = "redes", applied = applied),
        )
    }

    @Test
    fun `queryToApply uses the typed text and not the text the sheet was opened with`() {
        val applied = CourseQuery(text = "red")
        val state = opened(applied).toggle("grafos")

        assertEquals("redes", state.queryToApply(typedText = "redes", applied = applied)?.text)
    }

    @Test
    fun `queryToApply returns the query without tags when every filter was removed`() {
        val applied = CourseQuery(tags = listOf("grafos"))

        assertEquals(CourseQuery.EMPTY, opened(applied).reset().queryToApply(typedText = "", applied = applied))
    }

    @Test
    fun `queryToApply is null if a tag was removed and chosen again`() {
        val applied = CourseQuery(tags = listOf("grafos", "árboles"))
        val state = opened(applied).toggle("grafos").toggle("grafos")  // el borrador queda [árboles, grafos]

        assertEquals(listOf("árboles", "grafos"), state.draft.tags)
        assertNull(state.queryToApply(typedText = "", applied = applied))
    }

    @Test
    fun `queryToApply is null if a tag was chosen again with other capitals`() {
        val applied = CourseQuery(tags = listOf("grafos"))
        val state = opened(applied).toggle("grafos").toggle("Grafos")

        assertNull(state.queryToApply(typedText = "", applied = applied))
    }

    @Test
    fun `queryToApply is not null when the tags are different even with the same count`() {
        val applied = CourseQuery(tags = listOf("grafos"))
        val state = opened(applied).toggle("grafos").toggle("árboles")

        assertEquals(
            CourseQuery(tags = listOf("árboles")),
            state.queryToApply(typedText = "", applied = applied),
        )
    }

    // El adaptador HTTP recorta el texto: «red » y «red» piden lo mismo, así que no se repite la búsqueda.

    @Test
    fun `queryToApply is null if the typed text only differs by surrounding spaces`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        assertNull(opened(applied).queryToApply(typedText = "  red ", applied = applied))
    }

    @Test
    fun `queryToApply is null if the applied text has the extra spaces`() {
        val applied = CourseQuery(text = "red ")

        assertNull(opened(applied).queryToApply(typedText = "red", applied = applied))
    }

    @Test
    fun `queryToApply is null if the typed text is blank and no text was applied`() {
        assertNull(opened().queryToApply(typedText = "   ", applied = CourseQuery.EMPTY))
    }

    @Test
    fun `queryToApply does not ignore spaces inside the text`() {
        val applied = CourseQuery(text = "red neuronal")

        assertEquals(
            CourseQuery(text = "redneuronal"),
            opened(applied).queryToApply(typedText = "redneuronal", applied = applied),
        )
    }

    @Test
    fun `queryToApply keeps the typed text as typed when something else changed`() {
        val state = opened().toggle("grafos")

        assertEquals(
            CourseQuery(text = "red ", tags = listOf("grafos")),
            state.queryToApply(typedText = "red ", applied = CourseQuery.EMPTY),
        )
    }

    // ── Recuento del borrador: tarea 8a ──
    // El estado no pide nada ni espera: el ViewModel decide cuándo (tarea 8c).

    private val appliedRed = CourseQuery(text = "red")

    @Test
    fun `open starts without a preview`() {
        assertNull(opened().preview)
    }

    // ── previewQuery ──

    @Test
    fun `previewQuery is the draft with the typed text`() {
        val state = opened().toggle("grafos")

        assertEquals(CourseQuery(text = "red", tags = listOf("grafos")), state.previewQuery("red"))
    }

    @Test
    fun `previewQuery uses the typed text and not the text the sheet was opened with`() {
        assertEquals("redes", opened(appliedRed).previewQuery("redes").text)
    }

    @Test
    fun `previewQuery keeps the typed text as typed`() {
        assertEquals("red ", opened().previewQuery("red ").text)
    }

    // ── previewNeeded ──

    @Test
    fun `previewNeeded is false when the draft asks the same as what is applied`() {
        val applied = CourseQuery(text = "red", tags = listOf("grafos"))

        assertFalse(opened(applied).previewNeeded(typedText = "red", applied = applied))
    }

    @Test
    fun `previewNeeded is false when a tag was removed and chosen again`() {
        val applied = CourseQuery(tags = listOf("grafos", "árboles"))
        val state = opened(applied).toggle("grafos").toggle("Grafos")  // borrador [árboles, Grafos]

        assertFalse(state.previewNeeded(typedText = "", applied = applied))
    }

    @Test
    fun `previewNeeded is false when the typed text only differs by surrounding spaces`() {
        assertFalse(opened(appliedRed).previewNeeded(typedText = "  red ", applied = appliedRed))
    }

    @Test
    fun `previewNeeded is true when a tag was added`() {
        assertTrue(opened().toggle("grafos").previewNeeded(typedText = "", applied = CourseQuery.EMPTY))
    }

    @Test
    fun `previewNeeded is true when only the typed text changed`() {
        assertTrue(opened(appliedRed).previewNeeded(typedText = "redes", applied = appliedRed))
    }

    @Test
    fun `previewNeeded is false when that draft is already being counted`() {
        val state = opened().toggle("grafos").startPreview("")

        assertFalse(state.previewNeeded(typedText = "", applied = CourseQuery.EMPTY))
    }

    @Test
    fun `previewNeeded is false when that draft was already counted`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b"))

        assertFalse(state.previewNeeded(typedText = "", applied = CourseQuery.EMPTY))
    }

    @Test
    fun `previewNeeded is false when counting that draft failed and it is not retried`() {
        val state = opened().toggle("grafos").counted("", SearchOutcome.Unreachable)

        assertFalse(state.previewNeeded(typedText = "", applied = CourseQuery.EMPTY))
    }

    @Test
    fun `previewNeeded is true when the draft changed after being counted`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b")).toggle("árboles")

        assertTrue(state.previewNeeded(typedText = "", applied = CourseQuery.EMPTY))
    }

    @Test
    fun `previewNeeded is false again when the draft goes back to what is applied`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b")).toggle("grafos")

        assertFalse(state.previewNeeded(typedText = "", applied = CourseQuery.EMPTY))
    }

    // ── startPreview ──

    @Test
    fun `startPreview begins counting the draft with the typed text`() {
        val state = opened().toggle("grafos").startPreview("red")
        val query = CourseQuery(text = "red", tags = listOf("grafos"))

        assertEquals(query, state.preview?.query)
        assertEquals(SearchStatus.Loading(SearchRequest(query, 0)), state.preview?.status)
        assertEquals(SearchRequest(query, 0), state.preview?.pending)
    }

    @Test
    fun `startPreview replaces the previous preview`() {
        val first = opened().toggle("grafos").startPreview("")
        val second = first.toggle("árboles").startPreview("")

        assertEquals(CourseQuery(tags = listOf("grafos", "árboles")), second.preview?.query)
    }

    @Test
    fun `startPreview does not touch the draft nor the suggestions`() {
        val state = opened().toggle("grafos")
        val started = state.startPreview("red")

        assertEquals(state.draft, started.draft)
        assertEquals(state.suggestions, started.suggestions)
        assertEquals(state.showingAll, started.showingAll)
    }

    // ── receivePreview ──

    @Test
    fun `receivePreview keeps the answer of the count`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b"))

        assertEquals(3, state.preview?.total)
        assertEquals(SearchStatus.Ready, state.preview?.status)
    }

    @Test
    fun `receivePreview keeps a failure of the count`() {
        val state = opened().toggle("grafos").counted("", SearchOutcome.ServerFailure)

        assertEquals(SearchStatus.Failed(SearchProblem.SERVER_FAILURE), state.preview?.status)
    }

    // Una respuesta que llega tarde, de un borrador que ya no es el actual, no se aplica.
    @Test
    fun `receivePreview ignores a late answer from a previous count`() {
        val first = opened().toggle("grafos").startPreview("")
        val lateRequest = first.preview!!.pending!!
        val second = first.toggle("árboles").startPreview("")

        assertEquals(second, second.receivePreview(lateRequest, found(total = 9, "a", "b")))
    }

    @Test
    fun `receivePreview without a preview changes nothing`() {
        val state = opened()

        assertSame(state, state.receivePreview(SearchRequest(CourseQuery.EMPTY, 0), found(total = 3, "a", "b")))
    }

    // ── count ──

    @Test
    fun `count is the total of what is applied when the draft asks the same`() {
        val applied = appliedWith(appliedRed, total = 8)

        assertEquals(DraftCount.Known(8), opened(appliedRed).count(typedText = "red", applied = applied))
    }

    @Test
    fun `count of a draft that asks the same as what is applied ignores surrounding spaces`() {
        val applied = appliedWith(appliedRed, total = 8)

        assertEquals(DraftCount.Known(8), opened(appliedRed).count(typedText = "red ", applied = applied))
    }

    @Test
    fun `count of what is applied wins over an older preview`() {
        val applied = appliedWith(appliedRed, total = 8)
        val state = opened(appliedRed).toggle("grafos").counted("red", found(total = 3, "a", "b")).toggle("grafos")

        assertEquals(DraftCount.Known(8), state.count(typedText = "red", applied = applied))
    }

    @Test
    fun `count is known and zero when what is applied has no results`() {
        val applied = appliedWith(appliedRed, total = 0)

        assertEquals(DraftCount.Known(0), opened(appliedRed).count(typedText = "red", applied = applied))
    }

    @Test
    fun `count is counting while what is applied is still loading`() {
        val applied = CourseSearchState.start(appliedRed)

        assertEquals(DraftCount.Counting, opened(appliedRed).count(typedText = "red", applied = applied))
    }

    // Decisión de la tarea 8a: con «Cargar más» en curso o fallido el `total` sigue siendo válido.
    @Test
    fun `count keeps the total of what is applied while loading more`() {
        val applied = appliedWith(appliedRed, total = 8).loadMore()

        assertEquals(DraftCount.Known(8), opened(appliedRed).count(typedText = "red", applied = applied))
    }

    @Test
    fun `count keeps the total of what is applied after loading more failed`() {
        val applied = appliedWith(appliedRed, total = 8).loadMore().answer(SearchOutcome.Unreachable)

        assertEquals(DraftCount.Known(8), opened(appliedRed).count(typedText = "red", applied = applied))
    }

    @Test
    fun `count is unknown when what is applied failed, there is no total to show`() {
        val applied = CourseSearchState.start(appliedRed).answer(SearchOutcome.ServerFailure)

        assertEquals(DraftCount.Unknown, opened(appliedRed).count(typedText = "red", applied = applied))
    }

    @Test
    fun `count is the total of the preview when it is ready`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b"))

        assertEquals(DraftCount.Known(3), state.count(typedText = "", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    @Test
    fun `count is known and zero when the preview has no results`() {
        val state = opened().toggle("grafos").counted("", found(total = 0))

        assertEquals(DraftCount.Known(0), state.count(typedText = "", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    @Test
    fun `count is counting while the preview is loading`() {
        val state = opened().toggle("grafos").startPreview("")

        assertEquals(DraftCount.Counting, state.count(typedText = "", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    @Test
    fun `count is unknown when the preview failed`() {
        val state = opened().toggle("grafos").counted("", SearchOutcome.Unreachable)

        assertEquals(DraftCount.Unknown, state.count(typedText = "", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    @Test
    fun `count is counting when the preview has not started`() {
        val state = opened().toggle("grafos")

        assertEquals(DraftCount.Counting, state.count(typedText = "", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    @Test
    fun `count does not show the total of a preview of an older draft`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b")).toggle("árboles")

        assertEquals(DraftCount.Counting, state.count(typedText = "", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    @Test
    fun `count does not show the total of a preview made with other typed text`() {
        val state = opened().toggle("grafos").counted("red", found(total = 3, "a", "b"))

        assertEquals(DraftCount.Counting, state.count(typedText = "redes", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    @Test
    fun `count is counting again when the draft changed after a failed preview`() {
        val state = opened().toggle("grafos").counted("", SearchOutcome.Unreachable).toggle("árboles")

        assertEquals(DraftCount.Counting, state.count(typedText = "", applied = appliedWith(CourseQuery.EMPTY, 8)))
    }

    // ── previewToApply ──

    @Test
    fun `previewToApply is the preview when it is ready and asks for the same draft`() {
        val state = opened().toggle("grafos").counted("red", found(total = 3, "a", "b"))

        assertEquals(state.preview, state.previewToApply(typedText = "red"))
        assertEquals(listOf("a", "b"), state.previewToApply(typedText = "red")?.results?.map { it.id })
    }

    @Test
    fun `previewToApply ignores surrounding spaces in the typed text`() {
        val state = opened().toggle("grafos").counted("red", found(total = 3, "a", "b"))

        assertEquals(state.preview, state.previewToApply(typedText = " red "))
    }

    @Test
    fun `previewToApply is null while the preview is loading`() {
        assertNull(opened().toggle("grafos").startPreview("").previewToApply(typedText = ""))
    }

    @Test
    fun `previewToApply is null when the preview failed`() {
        val state = opened().toggle("grafos").counted("", SearchOutcome.ServerFailure)

        assertNull(state.previewToApply(typedText = ""))
    }

    @Test
    fun `previewToApply is null when the draft changed after counting`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b")).toggle("árboles")

        assertNull(state.previewToApply(typedText = ""))
    }

    @Test
    fun `previewToApply is null when the typed text changed after counting`() {
        val state = opened().toggle("grafos").counted("red", found(total = 3, "a", "b"))

        assertNull(state.previewToApply(typedText = "redes"))
    }

    @Test
    fun `previewToApply is null without a preview`() {
        assertNull(opened().toggle("grafos").previewToApply(typedText = ""))
    }

    // ── toggle y reset no tocan el recuento ──

    @Test
    fun `toggle keeps the preview`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b"))

        assertEquals(state.preview, state.toggle("árboles").preview)
    }

    @Test
    fun `reset keeps the preview`() {
        val state = opened().toggle("grafos").counted("", found(total = 3, "a", "b"))

        assertEquals(state.preview, state.reset().preview)
    }

    // ── Ayudas de los tests de recuento ──

    // Una página de tamaño 2: con `total` mayor que 2 quedan más («Cargar más»).
    private fun found(total: Int, vararg ids: String): SearchOutcome =
        SearchOutcome.Found(CoursePage(items = ids.map { course(it) }, page = 0, size = 2, total = total))

    private fun CourseSearchState.answer(outcome: SearchOutcome): CourseSearchState = receive(pending!!, outcome)

    // Una búsqueda aplicada y terminada con ese total (la primera página trae hasta 2 cursos).
    private fun appliedWith(query: CourseQuery, total: Int): CourseSearchState =
        CourseSearchState.start(query).answer(found(total, *arrayOf("a", "b").take(total).toTypedArray()))

    // Empieza a contar el borrador y recibe la respuesta.
    private fun FilterSheetState.counted(typedText: String, outcome: SearchOutcome): FilterSheetState {
        val started = startPreview(typedText)
        return started.receivePreview(started.preview!!.pending!!, outcome)
    }
}
