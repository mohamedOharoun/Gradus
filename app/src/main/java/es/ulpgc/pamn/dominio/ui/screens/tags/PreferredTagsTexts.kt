package es.ulpgc.pamn.dominio.ui.screens.tags

import es.ulpgc.pamn.dominio.settings.PreferredTags
import es.ulpgc.pamn.dominio.settings.TagAddition

// Textos de «Etiquetas preferidas» y de la sección de Ajustes. Funciones puras, probadas en el ordenador
// (PreferredTagsTextsTest). En la fase 3b pasarán a strings.xml.

// Por qué no se añadió una etiqueta; null si se añadió.
fun TagAddition.message(): String? = when (this) {
    is TagAddition.Added -> null
    TagAddition.Empty -> "Escribe una etiqueta"
    TagAddition.AlreadyAdded -> "Ya tienes esa etiqueta"
    TagAddition.Full -> "Puedes tener ${PreferredTags.MAX} como mucho. Quita una para añadir otra."
}

fun countText(count: Int): String = "$count de ${PreferredTags.MAX}"

fun tagLabel(tag: String): String = "#$tag"

fun removeTagDescription(tag: String): String = "Quitar la etiqueta $tag"
