package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.ProblemMessage
import es.ulpgc.pamn.dominio.ui.screens.mycourses.AuthorCourseCard
import es.ulpgc.pamn.dominio.ui.screens.mycourses.CourseFilterHeader
import es.ulpgc.pamn.dominio.ui.screens.mycourses.CourseReportSheet
import es.ulpgc.pamn.dominio.ui.screens.mycourses.DiscardDraftDialog
import es.ulpgc.pamn.dominio.ui.screens.mycourses.MyCoursesActions
import es.ulpgc.pamn.dominio.ui.screens.mycourses.MyCoursesInlineNotice
import es.ulpgc.pamn.dominio.ui.screens.mycourses.MyCoursesUiState
import es.ulpgc.pamn.dominio.ui.screens.mycourses.UploadHeroCard
import es.ulpgc.pamn.dominio.ui.screens.mycourses.toText
import es.ulpgc.pamn.dominio.ui.text.asString

// Pestaña «Mis cursos» (Figma «5 · Mis cursos»). Sin estado propio: recibe qué mostrar y avisa con
// `actions`. Las previews están en mycourses/ScreenMyCoursesPreviews.kt.
@Composable
fun ScreenMyCourses(
    state: MyCoursesUiState,
    actions: MyCoursesActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") { MyCoursesHeader() }
            item(key = "hero") {
                UploadHeroCard(
                    onUploadCourse = actions.onUploadCourse,
                    onOpenGuide = actions.onOpenGuide,
                )
            }
            state.inlineNotice?.let { notice ->
                item(key = "notice") {
                    InlineNoticeBanner(
                        notice = notice,
                        onRetry = actions.onRetry,
                        onDismiss = actions.onDismissNotice,
                    )
                }
            }
            if (state.allCount > 0) {
                item(key = "filter_header") {
                    CourseFilterHeader(
                        selected = state.filter,
                        counts = state.counts,
                        onSelectFilter = actions.onSelectFilter,
                    )
                }
            }
            when {
                state.isLoading && state.allCount == 0 -> {
                    item(key = "loading") { LoadingCoursesIndicator() }
                }
                state.allCount == 0 -> {
                    item(key = "empty") {
                        CenteredProblemMessage(
                            title = stringResource(R.string.my_courses_empty_title),
                            body = stringResource(R.string.my_courses_empty_body),
                        )
                    }
                }
                state.items.isEmpty() -> {
                    item(key = "filter_empty") {
                        CenteredProblemMessage(
                            title = stringResource(R.string.my_courses_filter_empty_title),
                            body = stringResource(R.string.my_courses_filter_empty_body),
                        )
                    }
                }
                else -> {
                    items(
                        items = state.items,
                        key = { "${it.id}_${it.draft?.id.orEmpty()}" },
                    ) { item ->
                        AuthorCourseCard(item = item, actions = actions)
                    }
                }
            }
        }
        state.openReport?.let { openReport ->
            CourseReportSheet(
                openReport = openReport,
                onDismiss = actions.onCloseReport,
            )
        }
        state.pendingDiscard?.let { draft ->
            DiscardDraftDialog(
                draft = draft,
                onConfirm = actions.onConfirmDiscard,
                onDismiss = actions.onDismissDiscard,
            )
        }
    }
}

@Composable
private fun MyCoursesHeader() {
    Column {
        Text(
            text = stringResource(R.string.tab_my_courses),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.my_courses_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InlineNoticeBanner(
    notice: MyCoursesInlineNotice,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canRetry = notice == MyCoursesInlineNotice.UNREACHABLE ||
        notice == MyCoursesInlineNotice.SERVER_FAILURE

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 14.dp,
                end = 4.dp,
                top = 6.dp,
                bottom = if (canRetry) 4.dp else 6.dp,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_error_outline),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = notice.toText().asString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.my_courses_notice_dismiss),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (canRetry) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 10.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.action_retry),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCoursesIndicator() {
    val loadingDescription = stringResource(R.string.my_courses_loading)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = loadingDescription },
        )
    }
}

@Composable
private fun CenteredProblemMessage(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        ProblemMessage(title = title, body = body)
    }
}
