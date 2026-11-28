package es.ulpgc.pamn.dominio.course

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Implementación en memoria de CourseCopyStore: sirve para tests y como referencia de comportamiento.
// Usa MutableStateFlow.update para actualizaciones atómicas sin depender de APIs de la JVM.
class InMemoryCourseCopyStore : CourseCopyStore {

    private val copies = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun save(courseId: String, json: String) {
        copies.update { it + (courseId to json) }
    }

    override suspend fun load(courseId: String): String? = copies.value[courseId]
}
