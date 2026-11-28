package es.ulpgc.pamn.dominio.course.search

import es.ulpgc.pamn.dominio.course.CourseSummary

// Las etiquetas que se ofrecen en la hoja de filtros: las `shown` que se ven y el resto, que
// aparece al pulsar «Ver todas».
//
// Es un valor inmutable y se construye con `from`, una función pura: el resultado depende solo de
// lo que se le pasa. Las etiquetas salen de los resultados ya cargados, así que cualquiera que se
// elija da al menos un resultado.
data class TagSuggestions(val shown: List<String>, val hidden: List<String>) {
    // Tras pulsar «Ver todas».
    val all: List<String> get() = shown + hidden

    // ¿Se enseña «Ver todas»?
    val hasHidden: Boolean get() = hidden.isNotEmpty()

    companion object {
        const val SHOWN = 7

        // Orden: primero las elegidas, en el orden en que se eligieron (y se enseñan siempre, aunque
        // no salgan en los resultados, para poder quitarlas); después las que están en más cursos
        // y, a igualdad, por orden alfabético sin mayúsculas ni tildes.
        //
        // Se calcula al abrir la hoja con las etiquetas *aplicadas*, no las del borrador: si
        // siguiera al borrador, al tocar una etiqueta saltaría al principio bajo el dedo.
        fun from(results: List<CourseSummary>, selected: List<String>, shown: Int = SHOWN): TagSuggestions {
            val chosen = selected.filter { it.searchKey().isNotEmpty() }.distinctBy(String::searchKey)
            val chosenKeys = chosen.map(String::searchKey).toSet()

            val others = results
                // Cada curso cuenta una etiqueta una sola vez, aunque la repita.
                .flatMap { course -> course.tags.filter { it.searchKey().isNotEmpty() }.distinctBy(String::searchKey) }
                // Una entrada por etiqueta (sin distinguir mayúsculas), con una forma por curso.
                .groupBy(String::searchKey)
                .filterKeys { it !in chosenKeys }
                // `results` va del curso más reciente al más antiguo: se enseña la forma del primero.
                .map { (key, forms) -> Candidate(label = forms.first(), key = key, courses = forms.size) }
                .sortedWith(
                    compareByDescending<Candidate> { it.courses }
                        .thenBy { it.key.withoutAccents() }
                        .thenBy { it.key },  // desempate fijo si dos etiquetas solo se distinguen por la tilde
                )
                .map(Candidate::label)

            val ordered = chosen + others
            val visible = maxOf(shown, chosen.size)  // las elegidas se ven todas
            return TagSuggestions(shown = ordered.take(visible), hidden = ordered.drop(visible))
        }
    }
}

// Una etiqueta que no está elegida: la forma que se enseña, su clave y en cuántos cursos está.
private class Candidate(val label: String, val key: String, val courses: Int)

private val plainVowels = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')

// Para ordenar: á é í ó ú ü cuentan como su vocal. En `commonMain` no hay orden alfabético español
// sin una librería; la «ñ» se queda como está.
private fun String.withoutAccents(): String = map { plainVowels[it] ?: it }.joinToString("")
