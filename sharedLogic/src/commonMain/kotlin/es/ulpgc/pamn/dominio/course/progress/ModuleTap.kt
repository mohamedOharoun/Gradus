package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.course.missingFor
import es.ulpgc.pamn.dominio.course.stateOf

// Qué pasa al tocar un módulo del camino. Sin textos: la interfaz pone el mensaje.
sealed interface ModuleTap {
    data object OpenPractice : ModuleTap

    // `missing`: los prerrequisitos directos que faltan por dominar, en el orden de `requires`.
    data class Locked(val missing: List<Module>) : ModuleTap

    data object NotEnrolled : ModuleTap
}

// Función pura: combina el acceso (inscripción) con el estado del módulo. Sin inscripción gana
// siempre «sin inscripción», incluso sobre «bloqueado» (C-6). `null` si el módulo no existe.
fun Course.tapModule(moduleId: String, mastered: Set<String>, access: StudyAccess): ModuleTap? {
    if (module(moduleId) == null) return null
    return when (access) {
        StudyAccess.NotEnrolled -> ModuleTap.NotEnrolled
        StudyAccess.Allowed -> when (stateOf(moduleId, mastered)) {
            ModuleState.MASTERED, ModuleState.FRONTIER -> ModuleTap.OpenPractice
            ModuleState.LOCKED -> ModuleTap.Locked(missingFor(moduleId, mastered))
        }
    }
}
