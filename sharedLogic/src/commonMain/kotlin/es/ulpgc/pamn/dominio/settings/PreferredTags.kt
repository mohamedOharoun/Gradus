package es.ulpgc.pamn.dominio.settings

import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.search.searchKey

// Qué pasa al intentar añadir una etiqueta preferida. La pantalla decide qué texto enseña en cada caso.
sealed interface TagAddition {
    data class Added(val tags: PreferredTags) : TagAddition

    // Vacía, o solo «#» y espacios.
    data object Empty : TagAddition

    // Ya estaba (comparada como en Buscar: «#GRAFOS» es «grafos»).
    data object AlreadyAdded : TagAddition

    // Ya hay PreferredTags.MAX.
    data object Full : TagAddition
}

// Las etiquetas preferidas de Ajustes (docs/tareas/ajustes.md, fase 3, decisión M). Inmutable: añadir
// o quitar devuelve otra. Se guardan como Buscar compara las de los cursos (`searchKey`: espacios de sobra
// fuera y minúsculas), sin «#» delante, sin repetir y en el orden en que se añadieron.
data class PreferredTags(val values: List<String> = emptyList()) {

    fun add(raw: String): TagAddition {
        val tag = normalized(raw)
        return when {
            tag.isEmpty() -> TagAddition.Empty
            tag in values -> TagAddition.AlreadyAdded
            values.size >= MAX -> TagAddition.Full
            else -> TagAddition.Added(PreferredTags(values + tag))
        }
    }

    fun remove(tag: String): PreferredTags = PreferredTags(values - normalized(tag))

    companion object {
        const val MAX = 10

        // Factory method para lo que llega de fuera (lo guardado en el móvil): se vuelve a pasar por
        // las mismas reglas, así nunca entra una etiqueta vacía, repetida o de más.
        fun of(saved: List<String>): PreferredTags = saved.fold(PreferredTags()) { tags, raw ->
            (tags.add(raw) as? TagAddition.Added)?.tags ?: tags
        }

        private fun normalized(raw: String): String = raw.trim().trimStart('#').searchKey()
    }
}

// Decisión N: los cursos con alguna etiqueta preferida van primero. sortedBy es estable, así que dentro
// de cada grupo se respeta el orden que traía la lista (p. ej. por fecha). Lo usará Buscar (del compañero).
fun List<CourseSummary>.prioritizing(preferred: PreferredTags): List<CourseSummary> {
    if (preferred.values.isEmpty()) return this
    val wanted = preferred.values.toSet()
    return sortedBy { course -> if (course.tags.any { it.searchKey() in wanted }) 0 else 1 }
}
