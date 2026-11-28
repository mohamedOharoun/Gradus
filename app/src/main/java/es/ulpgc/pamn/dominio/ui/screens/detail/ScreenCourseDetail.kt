package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.screens.course.CourseLoadingContent
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblemContent
import es.ulpgc.pamn.dominio.ui.text.asString

// La ficha de un curso (docs/tareas/ficha-curso-ui.md): de qué va, inscripción, progreso y baja.
// Sin estado: pinta CourseDetailState y avisa con CourseDetailActions. Las previews están en
// ScreenCourseDetailPreviews.kt.
@Composable
fun ScreenCourseDetail(state: CourseDetailState, actions: CourseDetailActions, modifier: Modifier = Modifier) {
    when (state) {
        // Descargando y problemas: las mismas pantallas que el camino (descargan el mismo curso).
        CourseDetailState.Loading -> CourseLoadingContent(onBack = actions.onBack, modifier = modifier)
        is CourseDetailState.Problem ->
            CourseProblemContent(state.problem, onRetry = actions.onRetry, onBack = actions.onBack, modifier = modifier)
        is CourseDetailState.Ready -> ReadyContent(state, actions, modifier)
    }
}

@Composable
private fun ReadyContent(state: CourseDetailState.Ready, actions: CourseDetailActions, modifier: Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        // La cabecera tintada llega hasta detrás de la barra de estado (statusBarsPadding en la
        // cabecera) y la barra inferior gestiona navigationBarsPadding().
        contentWindowInsets = WindowInsets(0),
        bottomBar = { DetailBottomBar(state, actions) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CourseDetailHeader(
                state = state,
                onBack = actions.onBack,
                onOpenUnenrollDialog = actions.onOpenUnenrollDialog,
            )
            // En horizontal o en tablet no se estira: como mucho 560 dp, centrado.
            CourseDetailBody(
                state = state,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            )
        }
    }

    if (state.unenrollDialog !is UnenrollDialogState.Closed) {
        UnenrollCourseDialog(
            dialogState = state.unenrollDialog,
            onConfirm = actions.onConfirmUnenroll,
            onDismiss = actions.onDismissUnenrollDialog,
        )
    }
}

// Barra inferior fija con el botón principal («Inscribirse al curso» / «Inscribiendo…» o «Ir al curso →»).
@Composable
private fun DetailBottomBar(state: CourseDetailState.Ready, actions: CourseDetailActions) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                PrimaryButton(
                    text = ctaButtonText(state.enrolled, state.enrolling).asString(),
                    onClick = if (state.enrolled) actions.onStart else actions.onEnroll,
                    enabled = state.enrolled || !state.enrolling,
                    trailingIcon = R.drawable.ic_arrow_forward.takeIf { state.enrolled },
                    modifier = Modifier.widthIn(max = 520.dp),
                )
            }
        }
    }
}
