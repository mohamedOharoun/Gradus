package es.ulpgc.pamn.dominio.ui.screens.search

// Los textos de los filtros de Buscar, como funciones puras: sin Compose, para probarlas en el
// ordenador (FilterTextsTest) igual que SearchTexts.

const val FILTERS_LABEL = "Filtros"

// Lo que se ve en la hoja cuando no hay ninguna etiqueta que ofrecer (decisión G).
const val NO_TAGS_TEXT = "No hay etiquetas que elegir"

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
