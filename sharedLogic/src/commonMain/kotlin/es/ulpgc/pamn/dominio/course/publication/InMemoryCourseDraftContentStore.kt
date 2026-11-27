package es.ulpgc.pamn.dominio.course.publication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Adaptador en memoria de CourseDraftContentStore para tests y previews.
// Respaldado por MutableStateFlow con copias defensivas para garantizar seguridad ante accesos
// concurrentes y mutaciones externas del ByteArray.
class InMemoryCourseDraftContentStore(
    initial: Map<String, ByteArray> = emptyMap(),
) : CourseDraftContentStore {

    private val files = MutableStateFlow(initial.mapValues { (_, bytes) -> bytes.copyOf() })

    fun put(filePath: String, content: ByteArray) {
        val copy = content.copyOf()
        files.update { current -> current + (filePath to copy) }
    }

    override suspend fun read(filePath: String): ByteArray? = files.value[filePath]?.copyOf()

    override suspend fun delete(filePath: String) {
        files.update { current -> current - filePath }
    }
}
