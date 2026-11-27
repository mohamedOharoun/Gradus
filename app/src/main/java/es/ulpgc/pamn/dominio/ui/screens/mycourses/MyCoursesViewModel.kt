package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.clearIfCurrent
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseFilter
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.CoursePublicationGateway
import es.ulpgc.pamn.dominio.course.publication.DraftStatus
import es.ulpgc.pamn.dominio.course.publication.MyCoursesOutcome
import es.ulpgc.pamn.dominio.course.publication.PendingAction
import es.ulpgc.pamn.dominio.course.publication.countByFilter
import es.ulpgc.pamn.dominio.course.publication.filterBy
import es.ulpgc.pamn.dominio.course.publication.mergeAuthorCourses
import es.ulpgc.pamn.dominio.data.publication.CourseDraftStorage
import es.ulpgc.pamn.dominio.data.publication.CourseUploadRequester
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Clock

// ViewModel de «Mis cursos»: coordina estado de UI, flujo de borradores y refresco del servidor;
// delega los comandos de escritura sobre borradores en CourseDraftCommands (patrón Facade).
class MyCoursesViewModel(
    private val gateway: CoursePublicationGateway,
    private val drafts: CourseDraftStore,
    storage: CourseDraftStorage,
    uploads: CourseUploadRequester,
    private val sessions: SessionStore,
    clock: Clock = Clock.System,
    idGenerator: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    companion object {
        // Factory Method: permite crear el ViewModel con dependencias inyectadas desde AppContainer.
        fun factory(
            gateway: CoursePublicationGateway,
            drafts: CourseDraftStore,
            storage: CourseDraftStorage,
            uploads: CourseUploadRequester,
            sessions: SessionStore,
        ) = viewModelFactory {
            initializer { MyCoursesViewModel(gateway, drafts, storage, uploads, sessions) }
        }
    }

    var uiState by mutableStateOf(MyCoursesUiState(isLoading = true))
        private set

    private val commands = CourseDraftCommands(drafts, storage, uploads, sessions, clock, idGenerator)
    private var published: List<CourseSummary> = emptyList()
    private var draftList: List<CourseDraft> = emptyList()
    private var hasLoadedOnce = false
    private var isLoading = true
    private var filter = AuthorCourseFilter.ALL
    private var inlineNotice: MyCoursesInlineNotice? = null
    private var openReportDraftId: String? = null
    private var openReport: OpenCourseReport? = null
    private var pendingDiscard: CourseDraft? = null
    private var pendingUploadTarget: DraftUploadTarget? = null
    private var previousPublishingIds: Set<String> = emptySet()
    private val discardingIds = mutableSetOf<String>()
    private var refreshJob: Job? = null

    init {
        observeDrafts()
        refreshCourses(showLoading = true)
    }

    fun onResume() {
        if (hasLoadedOnce && refreshJob?.isActive != true) refreshCourses(showLoading = false)
    }

    fun onRetry() = refreshCourses(showLoading = published.isEmpty() && draftList.isEmpty())

    fun onSelectFilter(filter: AuthorCourseFilter) {
        if (this.filter != filter) updateState { this.filter = filter }
    }

    fun onPrepareUploadNew() { pendingUploadTarget = DraftUploadTarget.NEW }

    fun onPrepareUploadVersion(item: AuthorCourseItem) {
        pendingUploadTarget = DraftUploadTarget.forItem(item)
    }

    fun onFileSelectionCancelled() { pendingUploadTarget = null }

    fun onFileSelected(sourceUri: String) {
        val target = pendingUploadTarget ?: return
        pendingUploadTarget = null
        viewModelScope.launch {
            val outcome = commands.importAndQueue(sourceUri, target) { previousId ->
                discardingIds += previousId
                clearDraftDialogsIfMatching(previousId)
            }
            when (outcome) {
                DraftImportOutcome.NoSession -> return@launch
                DraftImportOutcome.TooLarge -> updateState { inlineNotice = MyCoursesInlineNotice.FILE_TOO_LARGE }
                DraftImportOutcome.Unreadable -> updateState { inlineNotice = MyCoursesInlineNotice.FILE_UNREADABLE }
                DraftImportOutcome.Started -> updateState {
                    clearNoticeIf(MyCoursesInlineNotice.FILE_TOO_LARGE, MyCoursesInlineNotice.FILE_UNREADABLE)
                }
            }
        }
    }

    fun onPublish(draftId: String) {
        viewModelScope.launch {
            if (commands.publish(draftId) && openReportDraftId == draftId) updateState(::closeReportState)
        }
    }

    fun onOpenReport(item: AuthorCourseItem) {
        val draft = item.draft ?: return
        val nextReport = item.toOpenReport() ?: return
        updateState {
            openReportDraftId = draft.id
            openReport = nextReport
        }
    }

    fun onCloseReport() {
        if (openReport != null || openReportDraftId != null) updateState(::closeReportState)
    }

    fun onRequestDiscard(draft: CourseDraft) = updateState { pendingDiscard = draft }

    fun onDismissDiscard() {
        if (pendingDiscard != null) updateState { pendingDiscard = null }
    }

    fun onConfirmDiscard() {
        val draft = pendingDiscard ?: return
        updateState {
            pendingDiscard = null
            if (openReportDraftId == draft.id) closeReportState()
            discardingIds += draft.id
        }
        viewModelScope.launch { commands.discard(draft.id, draft.filePath) }
    }

    fun onDismissNotice() {
        if (inlineNotice != null) updateState { inlineNotice = null }
    }

    private fun observeDrafts() {
        viewModelScope.launch {
            drafts.drafts().collect { incoming ->
                val currentIds = incoming.mapTo(HashSet()) { it.id }
                val publishedInBackground = (previousPublishingIds - currentIds).any { it !in discardingIds }

                discardingIds.removeAll { it !in currentIds }
                previousPublishingIds = incoming.mapNotNullTo(HashSet()) { draft ->
                    draft.id.takeIf { (draft.status as? DraftStatus.Pending)?.action == PendingAction.PUBLISHING }
                }

                updateState {
                    draftList = incoming
                    if (pendingDiscard?.id?.let { it !in currentIds } == true) pendingDiscard = null
                    if (openReportDraftId?.let { it !in currentIds } == true) closeReportState()
                }
                if (publishedInBackground) refreshCourses(showLoading = false, cancelOngoing = true)
            }
        }
    }

    private fun refreshCourses(showLoading: Boolean, cancelOngoing: Boolean = false) {
        if (!cancelOngoing && refreshJob?.isActive == true) return
        refreshJob?.cancel()
        if (showLoading) updateState { isLoading = true }
        refreshJob = viewModelScope.launch {
            sessions.current()?.let { session ->
                when (val outcome = gateway.myCourses(session)) {
                    is MyCoursesOutcome.Found -> {
                        published = outcome.courses
                        clearNoticeIf(MyCoursesInlineNotice.UNREACHABLE, MyCoursesInlineNotice.SERVER_FAILURE)
                    }
                    MyCoursesOutcome.Unreachable -> inlineNotice = MyCoursesInlineNotice.UNREACHABLE
                    MyCoursesOutcome.ServerFailure -> inlineNotice = MyCoursesInlineNotice.SERVER_FAILURE
                    MyCoursesOutcome.SessionExpired -> sessions.clearIfCurrent(session)
                }
            }
            updateState {
                hasLoadedOnce = true
                isLoading = false
            }
        }
    }

    private fun clearDraftDialogsIfMatching(draftId: String) {
        if (pendingDiscard?.id == draftId) pendingDiscard = null
        if (openReportDraftId == draftId) closeReportState()
    }

    private fun closeReportState() {
        openReportDraftId = null
        openReport = null
    }

    private fun clearNoticeIf(vararg matching: MyCoursesInlineNotice) {
        if (inlineNotice in matching) inlineNotice = null
    }

    private inline fun updateState(change: () -> Unit) {
        change()
        val merged = mergeAuthorCourses(published, draftList)
        uiState = MyCoursesUiState(
            items = merged.filterBy(filter),
            allCount = merged.size,
            filter = filter,
            counts = merged.countByFilter(),
            isLoading = isLoading,
            inlineNotice = inlineNotice,
            openReport = openReport,
            pendingDiscard = pendingDiscard,
        )
    }
}

// Conecta las acciones de ScreenMyCourses a este ViewModel (patrón Parameter Object): la navegación
// y el selector de ficheros llegan de fuera para mantener el ViewModel libre de dependencias Android.
fun MyCoursesViewModel.actions(
    onUploadCourse: () -> Unit,
    onUploadNewVersion: (AuthorCourseItem) -> Unit,
    onOpenGuide: () -> Unit,
): MyCoursesActions = MyCoursesActions(
    onUploadCourse = onUploadCourse,
    onOpenGuide = onOpenGuide,
    onSelectFilter = this::onSelectFilter,
    onUploadNewVersion = onUploadNewVersion,
    onPublish = this::onPublish,
    onOpenReport = this::onOpenReport,
    onCloseReport = this::onCloseReport,
    onRequestDiscard = this::onRequestDiscard,
    onConfirmDiscard = this::onConfirmDiscard,
    onDismissDiscard = this::onDismissDiscard,
    onRetry = this::onRetry,
    onDismissNotice = this::onDismissNotice,
)
