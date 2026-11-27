package es.ulpgc.pamn.dominio.ui.screens.mycourses

import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.data.publication.CourseDraftStorage
import es.ulpgc.pamn.dominio.data.publication.CourseUploadRequester
import es.ulpgc.pamn.dominio.data.publication.DraftImportResult
import kotlin.time.Clock

// Objetivo elegido por el usuario antes de abrir el selector SAF (curso nuevo o reemplazo/versión).
internal data class DraftUploadTarget(
    val targetCourseId: String?,
    val previousDraftId: String?,
    val previousFilePath: String?,
) {
    companion object {
        val NEW = DraftUploadTarget(targetCourseId = null, previousDraftId = null, previousFilePath = null)

        fun forItem(item: AuthorCourseItem) = DraftUploadTarget(
            targetCourseId = item.published?.id ?: item.draft?.targetCourseId,
            previousDraftId = item.draft?.id,
            previousFilePath = item.draft?.filePath,
        )
    }
}

// Resultado de importar un fichero JSON y encolar su validación.
internal sealed interface DraftImportOutcome {
    data object Started : DraftImportOutcome
    data object NoSession : DraftImportOutcome
    data object TooLarge : DraftImportOutcome
    data object Unreadable : DraftImportOutcome
}

// Patrón Facade / Command: encapsula las operaciones de escritura sobre borradores (importar,
// publicar y descartar) para que MyCoursesViewModel solo coordine estado de UI y ciclo de vida.
internal class CourseDraftCommands(
    private val drafts: CourseDraftStore,
    private val storage: CourseDraftStorage,
    private val uploads: CourseUploadRequester,
    private val sessions: SessionStore,
    private val clock: Clock,
    private val idGenerator: () -> String,
) {
    suspend fun importAndQueue(
        sourceUri: String,
        target: DraftUploadTarget,
        onReplacingPrevious: (String) -> Unit,
    ): DraftImportOutcome {
        val session = sessions.current() ?: return DraftImportOutcome.NoSession
        val newDraftId = idGenerator()
        return when (val imported = storage.import(newDraftId, sourceUri)) {
            DraftImportResult.TooLarge -> DraftImportOutcome.TooLarge
            DraftImportResult.Unreadable -> DraftImportOutcome.Unreadable
            is DraftImportResult.Imported -> {
                drafts.upsert(
                    CourseDraft(
                        id = newDraftId,
                        userId = session.account.id,
                        targetCourseId = target.targetCourseId,
                        filePath = imported.filePath,
                        title = imported.preview.title,
                        moduleCount = imported.preview.moduleCount,
                        status = DraftStatus.Pending(PendingAction.VALIDATING),
                        updatedAt = clock.now(),
                    ),
                )
                target.previousDraftId?.let { previousId ->
                    onReplacingPrevious(previousId)
                    discard(previousId, target.previousFilePath)
                }
                uploads.enqueue(newDraftId)
                DraftImportOutcome.Started
            }
        }
    }

    suspend fun publish(draftId: String): Boolean {
        val draft = drafts.find(draftId) ?: return false
        if (draft.status !is DraftStatus.Accepted) return false
        val updated = drafts.updateIfPresent(
            draft.copy(
                status = DraftStatus.Pending(PendingAction.PUBLISHING),
                updatedAt = clock.now(),
            ),
        )
        if (updated) uploads.enqueue(draftId)
        return updated
    }

    // Única fuente de verdad para descartar un borrador: cancela trabajo, borra fila y borra fichero.
    suspend fun discard(draftId: String, filePath: String?) {
        uploads.cancel(draftId)
        drafts.delete(draftId)
        filePath?.let { storage.delete(it) }
    }
}
