package es.ulpgc.pamn.dominio.ui.text

import es.ulpgc.pamn.dominio.R

// Los nombres de los meses (docs/tareas/ajustes.md, paso 9a), compartidos por las notas («24 nov») y las
// tarjetas de los cursos («oct 2026»). Van como textos propios en strings.xml y no con los de Java: sus
// abreviaturas en español cambian según la versión («nov» o «nov.»). `month` va de 1 a 12.

private val SHORT_MONTHS = listOf(
    R.string.month_short_1, R.string.month_short_2, R.string.month_short_3, R.string.month_short_4,
    R.string.month_short_5, R.string.month_short_6, R.string.month_short_7, R.string.month_short_8,
    R.string.month_short_9, R.string.month_short_10, R.string.month_short_11, R.string.month_short_12,
)
private val LONG_MONTHS = listOf(
    R.string.month_long_1, R.string.month_long_2, R.string.month_long_3, R.string.month_long_4,
    R.string.month_long_5, R.string.month_long_6, R.string.month_long_7, R.string.month_long_8,
    R.string.month_long_9, R.string.month_long_10, R.string.month_long_11, R.string.month_long_12,
)

// «nov» / «Nov».
fun shortMonth(month: Int): UiText = UiText.of(SHORT_MONTHS[month - 1])

// «noviembre» / «November».
fun longMonth(month: Int): UiText = UiText.of(LONG_MONTHS[month - 1])
