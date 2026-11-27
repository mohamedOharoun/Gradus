package es.ulpgc.pamn.dominio.data.publication

import es.ulpgc.pamn.dominio.course.publication.CourseDraftContentStore
import es.ulpgc.pamn.dominio.course.publication.CourseDraftStore

// Implementación del Command ClearUserDrafts: elimina atómicamente las filas del usuario, cancela
// cada trabajo pendiente en segundo plano y borra los ficheros JSON locales asociados.
class UserDraftsCleaner(
    private val drafts: CourseDraftStore,
    private val contents: CourseDraftContentStore,
    private val cancelUpload: (String) -> Unit = {},
) : ClearUserDrafts {

    override suspend fun clearForUser(userId: String) {
        val removed = drafts.clearForUser(userId)
        removed.forEach { draft ->
            cancelUpload(draft.id)
            contents.delete(draft.filePath)
        }
    }
}
