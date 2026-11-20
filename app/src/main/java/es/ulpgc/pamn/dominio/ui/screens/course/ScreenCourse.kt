package es.ulpgc.pamn.dominio.ui.screens.course

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.masteredCount
import es.ulpgc.pamn.dominio.course.statesFor
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.ui.components.CourseHeader
import es.ulpgc.pamn.dominio.ui.components.CourseMap
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Lo que la pantalla avisa hacia fuera (objeto de parámetros, como RegisterActions).
data class CourseActions(
    val onModuleClick: (moduleId: String) -> Unit = {},
    val onNoticeShown: () -> Unit = {},
    val onBack: () -> Unit = {},
)

// El camino de un curso: la cabecera y, debajo, el camino; todo se desplaza junto.
@Composable
fun ScreenCourse(
    state: CourseUiState,
    actions: CourseActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Cuando hay un aviso nuevo, se enseña; al irse, se avisa para borrarlo del estado.
    // Si llega otro antes de que se vaya, LaunchedEffect cancela el anterior y enseña el nuevo.
    state.notice?.let { notice ->
        LaunchedEffect(notice) {
            snackbarHostState.showSnackbar(notice.toMessage())
            actions.onNoticeShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // La cabecera se desplaza con el camino: en horizontal, una cabecera fija dejaba sin sitio al camino.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                // rememberScrollState se guarda solo: al girar la pantalla se conserva el scroll.
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CourseHeader(
                title = state.course.title,
                masteredCount = state.masteredCount,
                total = state.course.modules.size,
                onBack = actions.onBack,
            )
            CourseMap(
                course = state.course,
                states = state.states,
                onModuleClick = actions.onModuleClick,
                // En horizontal o en tablet, el camino no se estira: mismo ancho que en un móvil, centrado.
                modifier = Modifier.widthIn(max = 480.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@LightDarkPreviews
@Composable
private fun ScreenCoursePreview() {
    GradusTheme {
        ScreenCourse(
            state = CourseUiState(demoCourse, demoCourse.statesFor(demoMastered), demoCourse.masteredCount(demoMastered)),
            actions = CourseActions(),
        )
    }
}
