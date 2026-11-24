package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.PublishedWithin

// Los textos de los filtros de Buscar, como funciones puras: sin Compose, para probarlas en el
// ordenador (FilterTextsTest) igual que SearchTexts.

const val FILTERS_LABEL = "Filtros"

// Lo que se ve en la hoja cuando no hay ninguna etiqueta que ofrecer (decisión G).
const val NO_TAGS_TEXT = "No hay etiquetas que elegir"

// Los textos fijos de la hoja de filtros (7c-2). El título es FILTERS_LABEL. Son constantes sin
// lógica, así que no tienen test propio: los tests de la hoja los usan al buscar los nodos.
const val RESET_FILTERS_TEXT = "Restablecer"
const val CLOSE_FILTERS_DESCRIPTION = "Cerrar filtros"
const val TAGS_SECTION_TITLE = "Etiquetas"
const val TAGS_SECTION_HINT = "· elige una o varias"
const val SHOW_ALL_TAGS_TEXT = "Ver todas"
const val SHOW_COURSES_TEXT = "Mostrar cursos"
const val NO_COURSES_MATCH_TEXT = "Ningún curso coincide"

// El texto del botón de la hoja según lo que se sabe del recuento del borrador (tarea 8b). Sin número
// mientras se cuenta y si el recuento falló (decisión B); con 0, que ningún curso coincide (decisión C).
fun showCoursesText(count: DraftCount): String = when (count) {
    is DraftCount.Known -> when (count.total) {
        0 -> NO_COURSES_MATCH_TEXT
        1 -> "Mostrar 1 curso"
        else -> "Mostrar ${count.total} cursos"
    }
    DraftCount.Counting, DraftCount.Unknown -> SHOW_COURSES_TEXT
}

// Lo que lee TalkBack en el botón «Filtros»: el número de filtros activos y no el número suelto del
// círculo. Con 0 (o menos) no se dice nada más: «Filtros».
fun filtersButtonDescription(count: Int): String = when {
    count <= 0 -> FILTERS_LABEL
    count == 1 -> "$FILTERS_LABEL, 1 activo"
    else -> "$FILTERS_LABEL, $count activos"
}

// Lo que se ve en el chip: «#grafos».
fun tagChipLabel(tag: String): String = "#$tag"

// Lo que lee TalkBack en el chip quitable. Sin «#»: se lee «Quitar filtro grafos», no «almohadilla».
fun removeFilterDescription(tag: String): String = removeFilter(tag)

// ── Creador y fecha (tarea 11, decisión C) ──

// Los textos fijos del campo «Creador» (11c). Constantes sin lógica: los tests del campo las usan al
// buscar los nodos. CREATOR_FIELD_DESCRIPTION es lo que lee TalkBack aunque el campo ya tenga texto
// (la ayuda desaparece al escribir).
const val CREATOR_HINT = "Nombre del creador"
const val CREATOR_FIELD_DESCRIPTION = "Creador"
const val CLEAR_CREATOR_DESCRIPTION = "Borrar creador"

// Los títulos de sección de la hoja (11d-1). El de «Creador» es CREATOR_FIELD_DESCRIPTION: el Figma
// no tiene etiqueta en el campo y el título hace de ella.
const val PUBLISHED_SECTION_TITLE = "Fecha de publicación"

// Lo que se ve en el chip del creador: «Creador: Ana», con la palabra del título de la sección.
// (Antes era «por Ana», como la tarjeta, pero en la prueba a mano no quedaba claro.)
fun authorChipLabel(name: String): String = "$CREATOR_FIELD_DESCRIPTION: $name"

// Lo que lee TalkBack en el chip del creador: dice de qué filtro se trata, no solo el nombre.
fun removeAuthorFilterDescription(name: String): String = removeFilter("creador $name")

// Los botones de «Fecha de publicación» y el chip de la fecha. Sin `else`: si se añade otra fecha, el
// compilador avisa aquí.
fun publishedLabel(within: PublishedWithin): String = when (within) {
    PublishedWithin.ANY -> "Cualquiera"
    PublishedWithin.LAST_MONTH -> "Último mes"
    PublishedWithin.LAST_YEAR -> "Último año"
}

// Lo que lee TalkBack en el chip de la fecha: la fecha en minúsculas, dentro de la frase
// («Quitar filtro último año»).
fun removePublishedFilterDescription(within: PublishedWithin): String =
    removeFilter(publishedLabel(within).lowercase())

private fun removeFilter(what: String): String = "Quitar filtro $what"
