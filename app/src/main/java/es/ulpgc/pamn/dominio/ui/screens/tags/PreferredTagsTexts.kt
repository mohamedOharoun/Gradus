package es.ulpgc.pamn.dominio.ui.screens.tags

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.settings.PreferredTags
import es.ulpgc.pamn.dominio.settings.TagAddition
import es.ulpgc.pamn.dominio.ui.text.UiText

// Textos de «Etiquetas preferidas» y de la sección de Ajustes. Funciones puras, probadas en el ordenador
// (PreferredTagsTextsTest), con UiText (fase 3, decisión U): las frases, en values/ y values-en/.

// Por qué no se añadió una etiqueta; null si se añadió.
fun TagAddition.message(): UiText? = when (this) {
    is TagAddition.Added -> null
    TagAddition.Empty -> UiText.of(R.string.tags_error_empty)
    TagAddition.AlreadyAdded -> UiText.of(R.string.tags_error_already_added)
    TagAddition.Full -> UiText.of(R.string.tags_error_full, PreferredTags.MAX)
}

fun countText(count: Int): UiText = UiText.of(R.string.tags_count, count, PreferredTags.MAX)

// Como en los cursos y en Buscar. No depende del idioma.
fun tagLabel(tag: String): String = "#$tag"

fun removeTagDescription(tag: String): UiText = UiText.of(R.string.tags_remove, tag)
