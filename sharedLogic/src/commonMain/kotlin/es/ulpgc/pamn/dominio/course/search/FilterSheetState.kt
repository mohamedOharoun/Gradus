package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.CourseSummary

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
) {
    // Las etiquetas que se dibujan: las 7 primeras o, tras «Ver todas», todas.
    val visibleTags: List<String> get() = if (showingAll) suggestions.all else suggestions.shown

    val canShowAll: Boolean get() = !showingAll && suggestions.hasHidden

    // «Restablecer» solo tiene sentido si hay etiquetas que quitar (el texto no cuenta).
    val canReset: Boolean get() = draft.tags.isNotEmpty()

    fun isSelected(tag: String): Boolean = draft.tags.any { it.tagKey() == tag.tagKey() }

    fun toggle(tag: String): FilterSheetState = copy(draft = draft.toggleTag(tag))

    // Vacía las etiquetas del borrador y conserva el texto; «Ver todas» sigue como estaba.
    fun reset(): FilterSheetState = copy(draft = draft.withoutFilters())

    fun showAll(): FilterSheetState = if (showingAll) this else copy(showingAll = true)

    // La consulta que hay que lanzar al pulsar «Mostrar N cursos», o `null` si no hay nada nuevo
    // que buscar. El texto es el de la caja, que puede haber cambiado desde que se abrió la hoja.
    //
    // Dos consultas piden lo mismo si tienen el mismo texto y las mismas etiquetas, **sin importar
    // el orden** (el curso debe tenerlas todas): quitar una etiqueta y volver a ponerla no repite
    // la búsqueda. El texto se compara recortado, como hace el adaptador HTTP: «red » y «red»
    // piden lo mismo. Si algo más cambia, la consulta lleva el texto tal como se teclea.
    fun queryToApply(typedText: String, applied: CourseQuery): CourseQuery? =
        draft.withText(typedText).takeUnless { it.asksTheSameAs(applied) }

    companion object {
        fun open(applied: CourseQuery, results: List<CourseSummary>): FilterSheetState =
            FilterSheetState(draft = applied, suggestions = TagSuggestions.from(results, applied.tags))
    }
}

private fun CourseQuery.asksTheSameAs(other: CourseQuery): Boolean =
    text.trim() == other.text.trim() &&
        tags.map(String::tagKey).toSet() == other.tags.map(String::tagKey).toSet()
