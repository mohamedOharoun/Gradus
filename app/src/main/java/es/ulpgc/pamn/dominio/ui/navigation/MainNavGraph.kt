package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.SearchOutcome
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.components.MainScaffold
import es.ulpgc.pamn.dominio.ui.screens.ScreenHome
import es.ulpgc.pamn.dominio.ui.screens.ScreenMyCourses
import es.ulpgc.pamn.dominio.ui.screens.ScreenSearch
import es.ulpgc.pamn.dominio.ui.screens.ScreenSettings
import es.ulpgc.pamn.dominio.ui.screens.course.CourseActions
import es.ulpgc.pamn.dominio.ui.screens.course.CourseScreenState
import es.ulpgc.pamn.dominio.ui.screens.course.CourseViewModel
import es.ulpgc.pamn.dominio.ui.screens.course.ScreenCourse
import es.ulpgc.pamn.dominio.ui.screens.exercise.ExerciseActions
import es.ulpgc.pamn.dominio.ui.screens.exercise.ExerciseViewModel
import es.ulpgc.pamn.dominio.ui.screens.exercise.ScreenExercise
import kotlinx.coroutines.launch

// Grafo anidado de las pantallas con sesión (docs/tareas/barra-navegacion.md).
// Va en su propio fichero para que GradusNavHost solo tenga que llamar a mainGraph(...).
// Sin animación entre pestañas: cada pestaña pinta su propia barra y, con el fundido por defecto,
// la barra parpadearía al cruzarse con la de la pestaña anterior.
fun NavGraphBuilder.mainGraph(navController: NavController) {
    navigation<Route.Main>(
        startDestination = Route.Home,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        tab<Route.Home>(TopLevelDestination.HOME, navController) { modifier ->
            // Provisional hasta que exista Buscar (la hace el compañero): abre el curso más reciente del servidor.
            val scope = rememberCoroutineScope()
            var notice by remember { mutableStateOf<String?>(null) }
            ScreenHome(
                onOpenCourse = {
                    scope.launch {
                        notice = null
                        latestCourseId(AppContainer.courseGateway)
                            .onSuccess { navController.navigate(Route.Course(it)) }
                            .onFailure { notice = it.message }
                    }
                },
                notice = notice,
                modifier = modifier,
            )
        }
        tab<Route.Search>(TopLevelDestination.SEARCH, navController) { ScreenSearch(it) }
        tab<Route.MyCourses>(TopLevelDestination.MY_COURSES, navController) { ScreenMyCourses(it) }
        tab<Route.Settings>(TopLevelDestination.SETTINGS, navController) { ScreenSettings(it) }
        // Sin barra: no es una pestaña, se entra desde una y «atrás» vuelve a ella.
        composable<Route.Course> { entry ->
            val route = entry.toRoute<Route.Course>()
            val courseViewModel: CourseViewModel = viewModel(
                factory = CourseViewModel.factory(route.courseId, AppContainer.courseGateway, AppContainer.progressStore),
            )
            // Módulo desbloqueado tocado: se abre su práctica y se borra el evento.
            val practiceToOpen = (courseViewModel.state as? CourseScreenState.Ready)?.ui?.practiceToOpen
            practiceToOpen?.let { moduleId ->
                LaunchedEffect(moduleId) {
                    navController.navigate(Route.Exercise(route.courseId, moduleId))
                    courseViewModel.onPracticeOpened()
                }
            }
            ScreenCourse(
                state = courseViewModel.state,
                actions = CourseActions(
                    onModuleClick = courseViewModel::onModuleClick,
                    onNoticeShown = courseViewModel::onNoticeShown,
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    onRetry = courseViewModel::onRetry,
                ),
            )
        }
        // Estudiar un módulo (lecciones, ejercicios y prueba de dominio). Sin barra; «atrás», cerrar y
        // «Volver al camino» vuelven al camino, que ya enseña lo dominado.
        composable<Route.Exercise> { entry ->
            val route = entry.toRoute<Route.Exercise>()
            // El camino ya descargó el curso: se lee de la caché. Si no está (la app se cerró en
            // segundo plano y Android la ha vuelto a abrir aquí), se vuelve al camino, que lo descarga.
            val course = AppContainer.courseGateway.cached(route.courseId)
            if (course == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val exerciseViewModel: ExerciseViewModel = viewModel(
                factory = ExerciseViewModel.factory(course, route.moduleId, AppContainer.progressStore),
            )
            val study = exerciseViewModel.study
            if (study == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            ScreenExercise(
                study = study,
                actions = ExerciseActions(
                    onChoose = exerciseViewModel::onChoose,
                    onLeftClick = exerciseViewModel::onLeftClick,
                    onRightClick = exerciseViewModel::onRightClick,
                    onCheck = exerciseViewModel::onCheck,
                    onContinue = exerciseViewModel::onContinue,
                    onStartTest = exerciseViewModel::onStartTest,
                    onRetry = exerciseViewModel::onRetry,
                    onClose = dropUnlessResumed { navController.popBackStack() },
                ),
            )
        }
    }
}

// Una pestaña: su pantalla dentro del armazón con la barra, marcando esa pestaña.
private inline fun <reified T : Any> NavGraphBuilder.tab(
    destination: TopLevelDestination,
    navController: NavController,
    crossinline screen: @Composable (Modifier) -> Unit,
) {
    composable<T> {
        MainScaffold(selected = destination, onSelect = navController::navigateToTab) { screen(it) }
    }
}

// Cambio de pestaña con el patrón recomendado por Android para la navegación inferior:
// - popUpTo(Home) + saveState: debajo solo queda Inicio, y se guarda el estado de lo que se quita.
// - launchSingleTop: pulsar la pestaña en la que ya estás no apila otra copia.
// - restoreState: al volver a una pestaña, recupera cómo estaba.
// Así «atrás» desde cualquier pestaña lleva a Inicio y, desde Inicio, cierra la app.
private fun NavController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo<Route.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Provisional (con el botón de Inicio): el id del curso publicado más reciente, o el motivo para no abrirlo.
private suspend fun latestCourseId(gateway: CourseGateway): Result<String> =
    when (val outcome = gateway.search(query = "")) {
        is SearchOutcome.Found -> outcome.page.items.firstOrNull()?.let { Result.success(it.id) }
            ?: Result.failure(IllegalStateException("Todavía no hay cursos publicados en el servidor."))
        SearchOutcome.Unreachable -> Result.failure(IllegalStateException("No se ha podido conectar con el servidor."))
        SearchOutcome.ServerFailure -> Result.failure(IllegalStateException("El servidor no ha respondido bien."))
    }
