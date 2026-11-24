package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.SearchOutcome

// La hoja de filtros: el borrador que se edita, las etiquetas que se ofrecen y si se ha pulsado
// «Ver todas». Todo lo que no es dibujar la hoja vive aquí, para que iOS solo rehaga las vistas.
//
// Es un valor inmutable: cada función devuelve un estado nuevo. Se crea con `open`, un *factory
// method*, cuando se abre la hoja.
data class FilterSheetState(
    // Empieza igual que la consulta aplicada; ✕ o «atrás» lo descartan sin aplicarlo.
    val draft: CourseQuery,
    // Se calculan al abrir y no siguen al borrador: así los chips no se mueven bajo el dedo.
    val suggestions: TagSuggestions,
    val showingAll: Boolean = false,
    // El recuento del borrador; `null` = todavía no se ha pedido.
    val preview: CourseSearchState? = null,
) {
    // Las etiquetas que se dibujan: las 7 primeras o, tras «Ver todas», todas.
    val visibleTags: List<String> get() = if (showingAll) suggestions.all else suggestions.shown

    val canShowAll: Boolean get() = !showingAll && suggestions.hasHidden

    // «Restablecer» solo tiene sentido si hay algún filtro que quitar: etiquetas, creador o fecha
    // (decisión D de la tarea 11). El texto no cuenta.
    val canReset: Boolean get() = draft.activeFilters.isNotEmpty()

    fun isSelected(tag: String): Boolean = draft.tags.any { it.searchKey() == tag.searchKey() }

    fun toggle(tag: String): FilterSheetState = copy(draft = draft.toggleTag(tag))

    // El creador se guarda tal como se escribe (el campo lo enseña así).
    fun withAuthor(author: String): FilterSheetState = copy(draft = draft.withAuthor(author))

    fun withPublished(published: PublishedWithin): FilterSheetState = copy(draft = draft.withPublished(published))

    // Vacía etiquetas, creador y fecha del borrador y conserva el texto; «Ver todas» sigue como estaba.
    fun reset(): FilterSheetState = copy(draft = draft.withoutFilters())

    fun showAll(): FilterSheetState = if (showingAll) this else copy(showingAll = true)

    // La consulta que hay que lanzar al pulsar «Mostrar N cursos», o `null` si no hay nada nuevo
    // que buscar. El texto es el de la caja, que puede haber cambiado desde que se abrió la hoja.
    //
    // Dos consultas piden lo mismo si tienen el mismo texto, las mismas etiquetas **sin importar
    // el orden** (el curso debe tenerlas todas), el mismo creador y la misma fecha: quitar una
    // etiqueta y volver a ponerla no repite la búsqueda. El texto se compara recortado, como hace el
    // adaptador HTTP: «red » y «red» piden lo mismo; el creador, sin mayúsculas ni espacios de sobra,
    // como el backend (`asksTheSameAs`). Si algo más cambia, la consulta lleva el texto y el creador
    // tal como se teclean.
    fun queryToApply(typedText: String, applied: CourseQuery): CourseQuery? =
        draft.withText(typedText).takeUnless { it.asksTheSameAs(applied) }

    // ── Recuento del borrador («Mostrar N cursos») ──
    // Este estado no pide nada ni espera: el ViewModel decide cuándo contar (tras 300 ms) y devuelve
    // la respuesta con `receivePreview` (o lo cancela con `cancelPreview`). `toggle`, `withAuthor`,
    // `withPublished` y `reset` no tocan `preview`.

    // Lo que se pediría con el borrador: sus filtros y el texto de la caja en este momento.
    fun previewQuery(typedText: String): CourseQuery = draft.withText(typedText)

    // ¿hay que pedir un recuento nuevo? No si el borrador pide lo mismo que lo aplicado (ya se sabe
    // su total) ni lo mismo que el último recuento (está en curso, terminado o fallido: no se cuenta
    // dos veces lo mismo, ni se reintenta solo un fallo).
    fun previewNeeded(typedText: String, applied: CourseQuery): Boolean {
        val query = previewQuery(typedText)
        val alreadyCounted = preview?.let { query.asksTheSameAs(it.query) } ?: false
        return !query.asksTheSameAs(applied) && !alreadyCounted
    }

    // Empieza a contar el borrador. Sustituye al recuento anterior: su respuesta, si llega tarde,
    // la ignora `CourseSearchState`. La petición que hay que lanzar es `preview.pending`.
    fun startPreview(typedText: String): FilterSheetState =
        copy(preview = CourseSearchState.start(previewQuery(typedText)))

    fun receivePreview(request: SearchRequest, outcome: SearchOutcome): FilterSheetState =
        preview?.let { copy(preview = it.receive(request, outcome)) } ?: this

    // El ViewModel ha cancelado el recuento. Uno a medias ya no va a terminar: se olvida, para que
    // `previewNeeded` vuelva a pedirlo si el borrador regresa a él (si no, el botón se quedaría en
    // «Mostrar cursos» para siempre). Uno terminado o fallido se conserva: su resultado sigue valiendo.
    // (Tarea 8c-1; ver la nota para el revisor en buscar-cursos.md.)
    fun cancelPreview(): FilterSheetState = if (preview?.pending != null) copy(preview = null) else this

    // Lo que dice el botón. Primero lo aplicado (su total ya se conoce), después el recuento; si el
    // borrador no coincide con ninguno de los dos, aún se está esperando a contar.
    fun count(typedText: String, applied: CourseSearchState): DraftCount {
        val query = previewQuery(typedText)
        val counted = when {
            query.asksTheSameAs(applied.query) -> applied
            preview != null && query.asksTheSameAs(preview.query) -> preview
            else -> null
        }
        return counted?.draftCount() ?: DraftCount.Counting
    }

    // Los resultados que se pueden usar al pulsar «Mostrar N cursos» sin volver a pedirlos: solo si
    // el recuento terminó y es del borrador de ahora (con el texto de ahora).
    fun previewToApply(typedText: String): CourseSearchState? =
        preview?.takeIf { it.status == SearchStatus.Ready && it.query.asksTheSameAs(previewQuery(typedText)) }

    companion object {
        fun open(applied: CourseQuery, results: List<CourseSummary>): FilterSheetState =
            FilterSheetState(draft = applied, suggestions = TagSuggestions.from(results, applied.tags))
    }
}

// Qué número se puede enseñar de una búsqueda. Con «Cargar más» en curso o fallido el `total` sigue
// siendo el de la consulta, así que se enseña. Si falló la primera página no hay total que enseñar.
// (Decisión de la tarea 8a; ver la nota del revisor en buscar-cursos.md.)
private fun CourseSearchState.draftCount(): DraftCount = when (status) {
    SearchStatus.Ready, is SearchStatus.LoadingMore, is SearchStatus.MoreFailed -> DraftCount.Known(total)
    is SearchStatus.Loading -> DraftCount.Counting
    is SearchStatus.Failed -> DraftCount.Unknown
}
