package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import es.ulpgc.pamn.dominio.ui.text.UiText

// Los textos de los filtros de Buscar, como funciones puras: sin Compose, para probarlas en el
// ordenador (FilterTextsTest) igual que SearchTexts.
// Desde docs/tareas/ajustes.md, paso 9c, son UiText: cada idioma pone sus palabras en strings.xml. Las
// etiquetas y el nombre del creador los escribe la persona: van tal cual.

val FILTERS_LABEL: UiText = UiText.of(R.string.filters)

// Lo que se ve en la hoja cuando no hay ninguna etiqueta que ofrecer (decisión G).
val NO_TAGS_TEXT: UiText = UiText.of(R.string.filters_no_tags)

// Los textos fijos de la hoja de filtros (7c-2). El título es FILTERS_LABEL. Son constantes sin
// lógica, así que no tienen test propio: los tests de la hoja los usan al buscar los nodos.
val RESET_FILTERS_TEXT: UiText = UiText.of(R.string.filters_reset)
val CLOSE_FILTERS_DESCRIPTION: UiText = UiText.of(R.string.filters_close)
val TAGS_SECTION_TITLE: UiText = UiText.of(R.string.filters_tags_title)
val TAGS_SECTION_HINT: UiText = UiText.of(R.string.filters_tags_hint)
val SHOW_ALL_TAGS_TEXT: UiText = UiText.of(R.string.filters_show_all_tags)
val SHOW_COURSES_TEXT: UiText = UiText.of(R.string.filters_show_courses)
val NO_COURSES_MATCH_TEXT: UiText = UiText.of(R.string.filters_no_courses_match)

// El texto del botón de la hoja según lo que se sabe del recuento del borrador (tarea 8b). Sin número
// mientras se cuenta y si el recuento falló (decisión B); con 0, que ningún curso coincide (decisión C).
fun showCoursesText(count: DraftCount): UiText = when (count) {
    is DraftCount.Known ->
        if (count.total == 0) NO_COURSES_MATCH_TEXT else UiText.plural(R.plurals.filters_show_count, count.total)
    DraftCount.Counting, DraftCount.Unknown -> SHOW_COURSES_TEXT
}

// Lo que lee TalkBack en el botón «Filtros»: el número de filtros activos y no el número suelto del
// círculo. Con 0 (o menos) no se dice nada más: «Filtros».
fun filtersButtonDescription(count: Int): UiText =
    if (count <= 0) FILTERS_LABEL else UiText.plural(R.plurals.filters_active, count)

// Lo que se ve en el chip: «#grafos». La almohadilla es igual en todos los idiomas.
fun tagChipLabel(tag: String): String = "#$tag"

// Lo que lee TalkBack en el chip quitable. Sin «#»: se lee «Quitar filtro grafos», no «almohadilla».
fun removeFilterDescription(tag: String): UiText = removeFilter(tag)

// ── Creador y fecha (tarea 11, decisión C) ──

// Los textos fijos del campo «Creador» (11c). Constantes sin lógica: los tests del campo las usan al
// buscar los nodos. CREATOR_FIELD_DESCRIPTION es lo que lee TalkBack aunque el campo ya tenga texto
// (la ayuda desaparece al escribir).
val CREATOR_HINT: UiText = UiText.of(R.string.filters_creator_hint)
val CREATOR_FIELD_DESCRIPTION: UiText = UiText.of(R.string.filters_creator)
val CLEAR_CREATOR_DESCRIPTION: UiText = UiText.of(R.string.filters_clear_creator)

// Los títulos de sección de la hoja (11d-1). El de «Creador» es CREATOR_FIELD_DESCRIPTION: el Figma
// no tiene etiqueta en el campo y el título hace de ella.
val PUBLISHED_SECTION_TITLE: UiText = UiText.of(R.string.filters_published_title)

// Lo que se ve en el chip del creador: «Creador: Ana», con la palabra del título de la sección.
// (Antes era «por Ana», como la tarjeta, pero en la prueba a mano no quedaba claro.)
fun authorChipLabel(name: String): UiText = UiText.of(R.string.filters_author_chip, name)

// Lo que lee TalkBack en el chip del creador: dice de qué filtro se trata, no solo el nombre.
fun removeAuthorFilterDescription(name: String): UiText = UiText.of(R.string.filters_remove_author, name)

// Los botones de «Fecha de publicación» y el chip de la fecha. Sin `else`: si se añade otra fecha, el
// compilador avisa aquí.
fun publishedLabel(within: PublishedWithin): UiText = when (within) {
    PublishedWithin.ANY -> UiText.of(R.string.published_any)
    PublishedWithin.LAST_MONTH -> UiText.of(R.string.published_last_month)
    PublishedWithin.LAST_YEAR -> UiText.of(R.string.published_last_year)
}

// Lo que lee TalkBack en el chip de la fecha: la fecha dentro de la frase («Quitar filtro último año»).
// Cada idioma tiene su forma en strings.xml (antes, lowercase(): no sirve para un texto sin traducir).
fun removePublishedFilterDescription(within: PublishedWithin): UiText = removeFilter(
    when (within) {
        PublishedWithin.ANY -> UiText.of(R.string.published_any_inline)
        PublishedWithin.LAST_MONTH -> UiText.of(R.string.published_last_month_inline)
        PublishedWithin.LAST_YEAR -> UiText.of(R.string.published_last_year_inline)
    },
)

private fun removeFilter(what: Any): UiText = UiText.of(R.string.filters_remove, what)
