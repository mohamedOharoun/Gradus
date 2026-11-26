package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.BackButton
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.screens.course.CourseLoadingContent
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblemContent
import es.ulpgc.pamn.dominio.ui.text.asString

// La ficha de un curso (docs/tareas/ficha-curso.md): de qué va antes de entrar en su camino.
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
        // El botón queda fijo abajo: se ve aunque la descripción sea larga.
        bottomBar = { StartBar(state, actions.onStart) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // En horizontal o en tablet no se estira: como mucho 560 dp, centrado (como la práctica).
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BackButton(onClick = actions.onBack)
                CourseDetailHeader(state)
                LearnList(state)
            }
        }
    }
}

// Abajo: el progreso (si hay) y «Empezar / Continuar / Repasar el curso».
@Composable
private fun StartBar(state: CourseDetailState.Ready, onStart: () -> Unit) {
    val total = state.course.modules.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        progressText(state.masteredCount, total)?.let {
            Text(
                it.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        PrimaryButton(
            text = startButtonText(state.masteredCount, total).asString(),
            onClick = onStart,
            modifier = Modifier.widthIn(max = 520.dp),
            trailingIcon = R.drawable.ic_arrow_forward,
        )
    }
}
