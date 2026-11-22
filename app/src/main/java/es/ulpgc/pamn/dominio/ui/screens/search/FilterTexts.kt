package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.search.DraftCount

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
fun removeFilterDescription(tag: String): String = "Quitar filtro $tag"
