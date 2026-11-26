package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.data.submission.SubmissionPhotos

// SubmissionPhotos falso, sin ficheros: «prepara» cada origen como «preparada-N.jpg» con unos bytes de
// JPEG, salvo los que se le digan como ilegibles. Recuerda qué ha preparado, borrado y tirado.
class FakeSubmissionPhotos(private val unreadable: Set<String> = emptySet()) : SubmissionPhotos {

    val files = mutableMapOf<String, ByteArray>()
    val prepared = mutableListOf<String>()
    val deleted = mutableListOf<String>()

    override suspend fun prepare(source: String): String? {
        if (source in unreadable) return null
        val path = "preparada-${prepared.size + 1}.jpg"
        prepared += source
        files[path] = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), prepared.size.toByte())
        return path
    }

    // Las fotos de la cámara: las preparadas (y tiradas tras prepararlas) y las tiradas sin preparar.
    val preparedCaptures = mutableListOf<String>()
    val discardedCaptures = mutableListOf<String>()

    override suspend fun prepareCapture(capture: String): String? {
        val path = prepare(capture) ?: return null
        preparedCaptures += capture
        return path
    }

    override suspend fun discardCapture(capture: String) {
        discardedCaptures += capture
    }

    override suspend fun read(path: String): ByteArray? = files[path]

    override suspend fun delete(path: String) {
        deleted += path
        files.remove(path)
    }
}
