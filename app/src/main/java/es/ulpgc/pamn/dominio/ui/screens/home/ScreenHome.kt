package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.home.HomeSummary
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import java.time.LocalDate

// Lo que se puede hacer en Inicio. La pantalla no navega: avisa (state hoisting).
class HomeActions(
    // «Continuar» en «Continúa donde lo dejaste» y tocar una fila de «Tus cursos»: abrir ese curso.
    val onOpenCourse: (courseId: String) -> Unit = {},
    // «Buscar cursos» del aviso sin cursos: ir a la pestaña Buscar.
    val onSearch: () -> Unit = {},
)

// La pestaña Inicio (Figma «Inicio», docs/tareas/inicio.md): cabecera, «Continúa donde lo dejaste» y «Tus
// cursos». Sin cursos, un aviso con «Buscar cursos». Los cursos que no se han podido cargar se avisan abajo.
// `today` llega por parámetro: la ruta pasa hoy; las previews y los tests, un día fijo.
@Composable
fun ScreenHome(state: HomeScreenState, today: LocalDate, actions: HomeActions, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state) {
            HomeScreenState.Loading -> HomeLoading()
            is HomeScreenState.Ready -> HomeContent(state.ui, today, actions)
        }
    }
}

@Composable
private fun HomeContent(ui: HomeUiState, today: LocalDate, actions: HomeActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        HomeHeader(today, ui.name, ui.initials)
        ui.summary.continueWith?.let { course ->
            ContinueSection(course, onContinue = { actions.onOpenCourse(course.course.id) })
        }
        // Sin el de «Continúa» (decisión J); si no queda ninguno, la sección no sale.
        if (ui.summary.otherCourses.isNotEmpty()) {
            HomeCoursesSection(ui.summary.otherCourses, onOpenCourse = actions.onOpenCourse)
        }
        if (ui.isEmpty) HomeEmpty(onSearch = actions.onSearch)
        unavailableText(ui.unavailable)?.let { UnavailableNotice(it.asString()) }
    }
}

// Mientras llegan los cursos. TalkBack dice qué se está cargando.
@Composable
private fun HomeLoading() {
    val description = stringResource(R.string.home_loading)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

// Sin cursos: qué pasa y qué hacer («Buscar cursos» lleva a la pestaña Buscar).
@Composable
private fun HomeEmpty(onSearch: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = stringResource(R.string.search_title), onClick = onSearch, modifier = Modifier.padding(top = 16.dp))
    }
}

// «No se ha podido cargar 1 curso…». liveRegion: si aparece con la pantalla abierta, TalkBack lo lee.
@Composable
private fun UnavailableNotice(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private val previewDay = LocalDate.of(2026, 10, 28)

@LightDarkPreviews
@Composable
private fun ScreenHomePreview() {
    GradusTheme {
        ScreenHome(
            state = HomeScreenState.Ready(HomeUiState("Nombre", "NM", HomePreviewData.summary)),
            today = previewDay,
            actions = HomeActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenHomeEmptyPreview() {
    GradusTheme {
        ScreenHome(
            state = HomeScreenState.Ready(HomeUiState("Nombre", "NM", HomeSummary(continueWith = null, courses = emptyList()))),
            today = previewDay,
            actions = HomeActions(),
        )
    }
}

@LightDarkPreviews
@Composable
private fun ScreenHomeLoadingPreview() {
    GradusTheme { ScreenHome(HomeScreenState.Loading, previewDay, HomeActions()) }
}
