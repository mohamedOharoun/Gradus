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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.ui.components.CourseHeader
import es.ulpgc.pamn.dominio.ui.components.CourseMap
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.text.ResourcesResolver
import es.ulpgc.pamn.dominio.ui.text.resolve
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Lo que la pantalla avisa hacia fuera (objeto de parámetros, como RegisterActions).
data class CourseActions(
    val onModuleClick: (moduleId: String) -> Unit = {},
    val onNoticeShown: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    // La hoja del módulo (notas-camara.md, tarea 3): estudiar, abrir sus notas o cerrarla.
    val sheet: ModuleSheetActions = ModuleSheetActions(),
)

// El curso descargado del servidor: mientras se descarga, la rueda; si falla, el problema; y si
// llega, su camino.
@Composable
fun ScreenCourse(
    state: CourseScreenState,
    actions: CourseActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CourseScreenState.Loading -> CourseLoadingContent(onBack = actions.onBack, modifier = modifier)
        is CourseScreenState.Problem ->
            CourseProblemContent(state.problem, onRetry = actions.onRetry, onBack = actions.onBack, modifier = modifier)
        is CourseScreenState.Ready -> CourseContent(state.ui, actions, modifier)
    }
}

// El camino de un curso: la cabecera y, debajo, el camino; todo se desplaza junto.
@Composable
private fun CourseContent(
    state: CourseUiState,
    actions: CourseActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // El aviso se escribe fuera de la composición (en LaunchedEffect): con los Resources del idioma de la app.
    val resolver = ResourcesResolver(LocalResources.current)
    // Cuando hay un aviso nuevo, se enseña; al irse, se avisa para borrarlo del estado.
    // Si llega otro antes de que se vaya, LaunchedEffect cancela el anterior y enseña el nuevo.
    state.notice?.let { notice ->
        LaunchedEffect(notice) {
            snackbarHostState.showSnackbar(notice.toMessage().resolve(resolver))
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
    // La hoja del módulo tocado, encima del camino.
    state.sheet?.let { moduleId ->
        val module = state.course.module(moduleId) ?: return@let
        ModuleSheet(
            title = module.title,
            state = state.states.getValue(moduleId),
            noteCount = state.noteCounts[moduleId] ?: 0,
            actions = actions.sheet,
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenCoursePreview() {
    GradusTheme {
        ScreenCourse(
            state = CourseScreenState.Ready(CourseUiState(demoCourse, demoMastered)),
            actions = CourseActions(),
        )
    }
}
