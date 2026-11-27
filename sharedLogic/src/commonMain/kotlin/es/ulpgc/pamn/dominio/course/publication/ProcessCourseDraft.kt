package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.clearIfCurrent
import es.ulpgc.pamn.dominio.course.CourseCacheInvalidator
import kotlin.time.Clock

// Coordinador puro del ciclo de vida de un borrador pendiente (VALIDATING o PUBLISHING).
// Aplica las reglas de sesión, propiedad de cuenta, fichero local, descarte en vuelo, reintentos
// e invalidación de caché sin depender de Android ni de WorkManager.
class ProcessCourseDraft(
    private val drafts: CourseDraftStore,
    private val contents: CourseDraftContentStore,
    private val sessions: SessionStore,
    private val gateway: CoursePublicationGateway,
    private val cacheInvalidator: CourseCacheInvalidator,
    private val clock: Clock = Clock.System,
) {

    suspend fun process(draftId: String, attempt: Int = 0): DraftWorkDecision {
        require(attempt >= 0) { "attempt must be non-negative: $attempt" }

        val draft = drafts.find(draftId) ?: return DraftWorkDecision.Done
        val pending = (draft.status as? DraftStatus.Pending) ?: return DraftWorkDecision.Done

        val session = sessions.current()
        if (session == null) {
            markPendingNotice(draft, pending.action, PendingNotice.SESSION_EXPIRED)
            return DraftWorkDecision.GiveUp
        }
        if (session.account.id != draft.userId) return DraftWorkDecision.GiveUp

        val bytes = contents.read(draft.filePath)
            ?: return reject(draft, DraftRejection.FileMissing)

        return when (pending.action) {
            PendingAction.VALIDATING -> processValidation(draft, session, bytes, attempt)
            PendingAction.PUBLISHING -> processPublication(draft, session, bytes, attempt)
        }
    }

    private suspend fun processValidation(
        draft: CourseDraft,
        session: Session,
        bytes: ByteArray,
        attempt: Int,
    ): DraftWorkDecision {
        val outcome = if (draft.targetCourseId == null) {
            gateway.validateNew(session, bytes)
        } else {
            gateway.validateVersion(session, draft.targetCourseId, bytes)
        }
        return when (outcome) {
            is ValidationOutcome.Validated -> {
                val nextStatus = if (outcome.report.isValid) {
                    DraftStatus.Accepted(outcome.report)
                } else {
                    DraftStatus.Rejected(DraftRejection.ValidationReport(outcome.report))
                }
                updateStatus(draft, nextStatus)
                DraftWorkDecision.Done
            }
            ValidationOutcome.TooLarge -> reject(draft, DraftRejection.TooLarge)
            ValidationOutcome.IdenticalToLatest -> reject(draft, DraftRejection.IdenticalToLatest)
            ValidationOutcome.NotAuthor -> reject(draft, DraftRejection.NotAuthor)
            ValidationOutcome.NotFound -> reject(draft, DraftRejection.CourseNotFound)
            ValidationOutcome.SessionExpired -> onSessionExpired(draft, PendingAction.VALIDATING, session)
            ValidationOutcome.Unreachable -> onNetworkFailure(draft, PendingAction.VALIDATING)
            ValidationOutcome.ServerFailure -> onServerFailure(draft, PendingAction.VALIDATING, attempt)
        }
    }

    private suspend fun processPublication(
        draft: CourseDraft,
        session: Session,
        bytes: ByteArray,
        attempt: Int,
    ): DraftWorkDecision {
        val outcome = if (draft.targetCourseId == null) {
            gateway.publishNew(session, bytes)
        } else {
            gateway.publishVersion(session, draft.targetCourseId, bytes)
        }
        return when (outcome) {
            is PublishOutcome.Published -> {
                drafts.delete(draft.id)
                contents.delete(draft.filePath)
                cacheInvalidator.invalidate(outcome.course.id)
                DraftWorkDecision.Done
            }
            is PublishOutcome.Invalid -> reject(draft, DraftRejection.ValidationReport(outcome.report))
            PublishOutcome.TooLarge -> reject(draft, DraftRejection.TooLarge)
            PublishOutcome.IdenticalToLatest -> reject(draft, DraftRejection.IdenticalToLatest)
            PublishOutcome.NotAuthor -> reject(draft, DraftRejection.NotAuthor)
            PublishOutcome.NotFound -> reject(draft, DraftRejection.CourseNotFound)
            PublishOutcome.SessionExpired -> onSessionExpired(draft, PendingAction.PUBLISHING, session)
            PublishOutcome.Unreachable -> onNetworkFailure(draft, PendingAction.PUBLISHING)
            PublishOutcome.ServerFailure -> onServerFailure(draft, PendingAction.PUBLISHING, attempt)
        }
    }

    private suspend fun reject(draft: CourseDraft, reason: DraftRejection): DraftWorkDecision {
        updateStatus(draft, DraftStatus.Rejected(reason))
        return DraftWorkDecision.Done
    }

    private suspend fun onSessionExpired(
        draft: CourseDraft,
        action: PendingAction,
        session: Session,
    ): DraftWorkDecision {
        sessions.clearIfCurrent(session)
        markPendingNotice(draft, action, PendingNotice.SESSION_EXPIRED)
        return DraftWorkDecision.GiveUp
    }

    private suspend fun onNetworkFailure(draft: CourseDraft, action: PendingAction): DraftWorkDecision =
        if (markPendingNotice(draft, action, PendingNotice.NETWORK)) {
            DraftWorkDecision.Retry
        } else {
            DraftWorkDecision.Done
        }

    private suspend fun onServerFailure(
        draft: CourseDraft,
        action: PendingAction,
        attempt: Int,
    ): DraftWorkDecision {
        if (!markPendingNotice(draft, action, PendingNotice.SERVER_FAILURE)) {
            return DraftWorkDecision.Done
        }
        return if (attempt < MAX_UPLOAD_SERVER_FAILURE_ATTEMPTS - 1) {
            DraftWorkDecision.Retry
        } else {
            DraftWorkDecision.GiveUp
        }
    }

    private suspend fun markPendingNotice(
        draft: CourseDraft,
        action: PendingAction,
        notice: PendingNotice,
    ): Boolean = updateStatus(draft, DraftStatus.Pending(action, notice))

    private suspend fun updateStatus(draft: CourseDraft, status: DraftStatus): Boolean =
        drafts.updateIfPresent(draft.copy(status = status, updatedAt = clock.now()))
}
