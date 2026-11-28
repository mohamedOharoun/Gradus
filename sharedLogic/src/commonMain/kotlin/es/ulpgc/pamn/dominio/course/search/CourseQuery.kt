package es.ulpgc.pamn.dominio.course.search

// Lo que el usuario ha pedido en Buscar: un texto, unas etiquetas (el curso debe tenerlas todas), un
// creador y una fecha de publicación. Los filtros presentes se combinan con Y, como en el backend.
//
// Es un valor inmutable: cada función devuelve una consulta nueva y no modifica la original.
// El texto y el creador se guardan tal como se escriben (la caja y el campo los enseñan así);
// recortarlos es cosa del adaptador HTTP. En blanco no filtran.
// Las etiquetas van en el orden en que se eligen, sin repetir (sin distinguir mayúsculas ni
// espacios de sobra, como `TextNormalizer` del backend) y se guarda la forma con que se eligieron.
// La fecha `ANY` («Cualquiera») no filtra.
data class CourseQuery(
    val text: String = "",
    val tags: List<String> = emptyList(),
    val author: String = "",
    val published: PublishedWithin = PublishedWithin.ANY,
) {
    // Hay texto que buscar (un texto en blanco cuenta como «sin texto»).
    val hasText: Boolean get() = text.isNotBlank()

    // Hay creador por el que filtrar (en blanco cuenta como «sin creador»).
    val hasAuthor: Boolean get() = author.isNotBlank()

    // Los chips quitables, en el orden de las secciones de la hoja: etiquetas, creador y fecha
    // (decisión C de la tarea 11). Su tamaño es el número de «Filtros 3».
    val activeFilters: List<ActiveFilter>
        get() = buildList {
            tags.mapTo(this, ActiveFilter::Tag)
            if (hasAuthor) add(ActiveFilter.Author(author.tidy()))
            if (published != PublishedWithin.ANY) add(ActiveFilter.Published(published))
        }

    fun withText(text: String): CourseQuery = copy(text = text)

    // Añade la etiqueta si no está y la quita si está. Una etiqueta en blanco no cambia nada.
    fun toggleTag(tag: String): CourseQuery {
        val key = tag.searchKey()
        return when {
            key.isEmpty() -> this
            tags.any { it.searchKey() == key } -> withoutTag(tag)
            else -> copy(tags = tags + tag.tidy())
        }
    }

    // Quita la etiqueta; si no estaba, devuelve la consulta igual.
    fun withoutTag(tag: String): CourseQuery {
        val key = tag.searchKey()
        return copy(tags = tags.filterNot { it.searchKey() == key })
    }

    fun withAuthor(author: String): CourseQuery = copy(author = author)

    fun withoutAuthor(): CourseQuery = copy(author = "")

    // Con `ANY` se quita la fecha (así se quita su chip).
    fun withPublished(published: PublishedWithin): CourseQuery = copy(published = published)

    // «Restablecer» (decisión D): quita etiquetas, creador y fecha, y conserva el texto.
    fun withoutFilters(): CourseQuery = CourseQuery(text = text)

    companion object {
        val EMPTY = CourseQuery()
    }
}

private val whitespace = Regex("\\s+")

// Espacios de sobra fuera (agrupados en uno y recortados); conserva las mayúsculas.
private fun String.tidy(): String = trim().replace(whitespace, " ")

// Con qué se compara lo que el backend normaliza (las etiquetas y el creador): con los espacios
// ordenados y en minúsculas. Como el backend, no quita tildes: «angel» no es «Ángel».
internal fun String.searchKey(): String = tidy().lowercase()

// Dos consultas piden lo mismo si tienen el mismo texto, las mismas etiquetas **sin importar el
// orden** (el curso debe tenerlas todas), el mismo creador y la misma fecha. El texto se compara
// recortado, como hace el adaptador HTTP: «red » y «red» piden lo mismo. El creador, con
// `searchKey`, como lo compara el backend: «Ana Ruiz» y « ana  ruiz» piden lo mismo.
internal fun CourseQuery.asksTheSameAs(other: CourseQuery): Boolean =
    text.trim() == other.text.trim() &&
        tags.map(String::searchKey).toSet() == other.tags.map(String::searchKey).toSet() &&
        author.searchKey() == other.author.searchKey() &&
        published == other.published
