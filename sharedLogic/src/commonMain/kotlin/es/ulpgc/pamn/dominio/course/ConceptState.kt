package es.ulpgc.pamn.dominio.course

// Los tres estados de la especificación: dominado, frontera (toca estudiarlo) y bloqueado.
enum class ConceptState { MASTERED, FRONTIER, LOCKED }

// Funciones puras: el estado no se guarda, se calcula a partir de lo dominado.

fun Course.stateOf(conceptId: String, mastered: Set<String>): ConceptState = when {
    conceptId in mastered -> ConceptState.MASTERED
    missingFor(conceptId, mastered).isEmpty() -> ConceptState.FRONTIER
    else -> ConceptState.LOCKED
}

// LinkedHashMap por dentro: conserva el orden de los conceptos del curso.
fun Course.statesFor(mastered: Set<String>): Map<String, ConceptState> =
    concepts.associate { it.id to stateOf(it.id, mastered) }

// Los prerrequisitos que faltan por dominar, para decir «Domina antes …».
fun Course.missingFor(conceptId: String, mastered: Set<String>): List<Concept> =
    concept(conceptId)?.requires.orEmpty()
        .filterNot { it in mastered }
        .mapNotNull { concept(it) }

fun Course.masteredCount(mastered: Set<String>): Int = concepts.count { it.id in mastered }
