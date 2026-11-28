package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.home.HomeActions
import es.ulpgc.pamn.dominio.ui.screens.home.HomeViewModel
import es.ulpgc.pamn.dominio.ui.screens.home.ScreenHome
import java.time.LocalDate

// La pestaña Inicio conectada (docs/tareas/inicio.md, paso 4d). En su propio fichero, como SettingsRoute,
// para que en MainNavGraph.kt (que también edita el compañero) solo cambie la línea de Inicio.
// - Abrir un curso (Continuar o una fila) va a su camino: ya está inscrito.
// - `onSearch` lo pasa MainNavGraph: es su navigateToTab (privada allí), la misma que usa la barra.
@Composable
internal fun HomeRoute(navController: NavController, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(AppContainer.sessionStore, AppContainer.enrollmentsReader, AppContainer.courseGateway),
    )
    // Solo con la pantalla activa: dos toques rápidos no abren el curso dos veces (como en Buscar).
    val lifecycleOwner = LocalLifecycleOwner.current
    val openCourse = remember(lifecycleOwner, navController) {
        lifecycleOwner.onlyWhenResumed { id: String -> navController.navigate(Route.Course(id)) }
    }
    // Al volver de «Editar perfil» (o de otra pestaña) se relee la sesión: el nombre puede haber cambiado.
    LifecycleResumeEffect(homeViewModel) {
        homeViewModel.onResume()
        onPauseOrDispose {}
    }
    ScreenHome(
        state = homeViewModel.state,
        today = LocalDate.now(),
        actions = HomeActions(onOpenCourse = openCourse, onSearch = onSearch),
        modifier = modifier,
    )
}
