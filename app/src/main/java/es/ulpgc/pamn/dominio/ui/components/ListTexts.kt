package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.text.UiText

// «A», «A y B», «A, B y C» (en inglés, «A, B and C»), para los mensajes que enumeran cosas
// (docs/tareas/ajustes.md, fase 3, paso 8a). Cada idioma pone su «y» y su coma en strings.xml.
// Cada elemento es un String (lo que escribe el autor, que no se traduce: títulos de módulos, opciones…)
// o un UiText (p. ej. una opción entre las comillas de cada idioma).
fun joinedWithAnd(items: List<Any>): UiText = when (items.size) {
    0 -> UiText.raw("")
    1 -> items.single().let { it as? UiText ?: UiText.raw(it.toString()) }
    else -> UiText.of(R.string.list_and, items.dropLast(1).foldedWithCommas(), items.last())
}

// «A, B, C» sin «y» (paso 9a: lo que lee TalkBack de una tarjeta de curso).
fun joinedWithCommas(items: List<Any>): UiText =
    if (items.isEmpty()) UiText.raw("") else items.foldedWithCommas().let { it as? UiText ?: UiText.raw(it.toString()) }

// Anida «%1$s, %2$s». Con un solo elemento lo deja tal cual (un String sigue siendo un String).
private fun List<Any>.foldedWithCommas(): Any =
    drop(1).fold(first()) { joined, next -> UiText.of(R.string.list_comma, joined, next) }
