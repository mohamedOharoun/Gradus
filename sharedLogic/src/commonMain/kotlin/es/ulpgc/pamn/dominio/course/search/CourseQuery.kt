package es.ulpgc.pamn.dominio.course.search

// Lo que el usuario ha pedido en Buscar: un texto y unas etiquetas (el curso debe tenerlas todas).
//
// Es un valor inmutable: cada función devuelve una consulta nueva y no modifica la original.
// El texto se guarda tal como se escribe; recortarlo es cosa del adaptador HTTP.
// Las etiquetas van en el orden en que se eligen, sin repetir (sin distinguir mayúsculas ni
// espacios de sobra, como `TextNormalizer` del backend) y se guarda la forma con que se eligieron.
data class CourseQuery(
    val text: String = "",
    val tags: List<String> = emptyList(),
) {
    // Hay texto que buscar (un texto en blanco cuenta como «sin texto»).
    val hasText: Boolean get() = text.isNotBlank()

    // Los chips quitables, en orden. Su tamaño es el número de «Filtros 2».
    val activeFilters: List<ActiveFilter> get() = tags.map(ActiveFilter::Tag)

    fun withText(text: String): CourseQuery = copy(text = text)

    // Añade la etiqueta si no está y la quita si está. Una etiqueta en blanco no cambia nada.
    fun toggleTag(tag: String): CourseQuery {
        val key = tag.tagKey()
        return when {
            key.isEmpty() -> this
            tags.any { it.tagKey() == key } -> withoutTag(tag)
            else -> copy(tags = tags + tag.tidy())
        }
    }

    // Quita la etiqueta; si no estaba, devuelve la consulta igual.
    fun withoutTag(tag: String): CourseQuery {
        val key = tag.tagKey()
        return copy(tags = tags.filterNot { it.tagKey() == key })
    }

    // «Restablecer»: quita las etiquetas y conserva el texto.
    fun withoutFilters(): CourseQuery = copy(tags = emptyList())

    companion object {
        val EMPTY = CourseQuery()
    }
}

private val whitespace = Regex("\\s+")

// Espacios de sobra fuera (agrupados en uno y recortados); conserva las mayúsculas.
private fun String.tidy(): String = trim().replace(whitespace, " ")

// Con qué se compara una etiqueta: ordenada y en minúsculas.
internal fun String.tagKey(): String = tidy().lowercase()

// Dos consultas piden lo mismo si tienen el mismo texto y las mismas etiquetas, **sin importar el
// orden** (el curso debe tenerlas todas). El texto se compara recortado, como hace el adaptador
// HTTP: «red » y «red» piden lo mismo.
internal fun CourseQuery.asksTheSameAs(other: CourseQuery): Boolean =
    text.trim() == other.text.trim() &&
        tags.map(String::tagKey).toSet() == other.tags.map(String::tagKey).toSet()
