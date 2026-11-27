package es.ulpgc.pamn.dominio.data.publication

import es.ulpgc.pamn.dominio.course.publication.CourseDraftContentStore
import es.ulpgc.pamn.dominio.course.publication.CourseDraftPreview

// Resultado de importar un fichero JSON de curso al almacenamiento privado de la app.
sealed interface DraftImportResult {
    data class Imported(
        val filePath: String,
        val preview: CourseDraftPreview,
    ) : DraftImportResult

    data object TooLarge : DraftImportResult
    data object Unreadable : DraftImportResult
}

// Puerto (Strategy): extiende CourseDraftContentStore con la importación desde una URI de Android
// y el borrado en lote, manteniendo la lógica compartida desacoplada de ficheros y ContentResolver.
interface CourseDraftStorage : CourseDraftContentStore {

    // Copia el contenido de `sourceUri` al almacenamiento local bajo `draftId`, acotado a MAX_COURSE_BYTES.
    suspend fun import(draftId: String, sourceUri: String): DraftImportResult

    // Borra todos los ficheros indicados (ignora los ausentes o fuera de la carpeta de borradores).
    suspend fun deleteAll(filePaths: Iterable<String>)
}
