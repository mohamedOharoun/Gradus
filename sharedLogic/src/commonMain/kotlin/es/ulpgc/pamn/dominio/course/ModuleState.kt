package es.ulpgc.pamn.dominio.course

// Los tres estados de la especificación: dominado, frontera (toca estudiarlo) y bloqueado.
enum class ModuleState { MASTERED, FRONTIER, LOCKED }

// Funciones puras: el estado no se guarda, se calcula a partir de lo dominado.

fun Course.stateOf(moduleId: String, mastered: Set<String>): ModuleState = when {
    moduleId in mastered -> ModuleState.MASTERED
    missingFor(moduleId, mastered).isEmpty() -> ModuleState.FRONTIER
    else -> ModuleState.LOCKED
}

// LinkedHashMap por dentro: conserva el orden de los módulos del curso.
fun Course.statesFor(mastered: Set<String>): Map<String, ModuleState> =
    modules.associate { it.id to stateOf(it.id, mastered) }

// Los prerrequisitos que faltan por dominar, para decir «Domina antes …».
fun Course.missingFor(moduleId: String, mastered: Set<String>): List<Module> =
    module(moduleId)?.requires.orEmpty()
        .filterNot { it in mastered }
        .mapNotNull { module(it) }

fun Course.masteredCount(mastered: Set<String>): Int = modules.count { it.id in mastered }
