package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.course.progress.StudyAccess
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
import es.ulpgc.pamn.dominio.ui.screens.search.SearchViewModel
import es.ulpgc.pamn.dominio.ui.screens.search.actions

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
        tab<Route.Home>(TopLevelDestination.HOME, navController) { ScreenHome(it) }
        // El ViewModel vive con la entrada de Buscar: al abrir un curso y volver, o al cambiar de
        // pestaña y volver (saveState/restoreState), la búsqueda sigue igual.
        tab<Route.Search>(TopLevelDestination.SEARCH, navController) { modifier ->
            val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(AppContainer.courseGateway))
            // Solo con la pantalla activa: dos toques rápidos no abren el curso dos veces.
            // Se recuerda para que sea la misma función en cada recomposición: si no, SearchActions
            // cambiaría con cada tecla y la lista y las tarjetas se volverían a dibujar.
            val lifecycleOwner = LocalLifecycleOwner.current
            val openCourse = remember(lifecycleOwner, navController) {
                lifecycleOwner.onlyWhenResumed { id: String -> navController.navigate(Route.Course(id)) }
            }
            ScreenSearch(
                text = searchViewModel.text,
                search = searchViewModel.search,
                filters = searchViewModel.filters,
                actions = searchViewModel.actions(onCourseClick = openCourse),
                modifier = modifier,
            )
        }
        tab<Route.MyCourses>(TopLevelDestination.MY_COURSES, navController) { ScreenMyCourses(it) }
        tab<Route.Settings>(TopLevelDestination.SETTINGS, navController) { ScreenSettings(it) }
        // Sin barra: no es una pestaña, se entra desde una y «atrás» vuelve a ella.
        composable<Route.Course> { entry ->
            val route = entry.toRoute<Route.Course>()
            val courseViewModel: CourseViewModel = viewModel(
                factory = CourseViewModel.factory(
                    route.courseId, AppContainer.courseGateway, AppContainer.progressStore, AppContainer.enrollmentStore,
                ),
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
                factory = ExerciseViewModel.factory(
                    course, route.moduleId, AppContainer.progressStore, AppContainer.enrollmentStore,
                ),
            )
            // La puerta la decide el ViewModel: mientras se comprueba no se enseña nada; sin inscripción
            // se vuelve al camino sin componer la práctica (y así nunca se llega a markMastered).
            when (exerciseViewModel.access) {
                null -> return@composable
                StudyAccess.NotEnrolled -> {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                StudyAccess.Allowed -> Unit
            }
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

// Como dropUnlessResumed, pero para acciones que reciben un valor (el id del curso tocado).
// Mira el estado en el momento del toque: al navegar, esta pantalla deja de estar RESUMED y un
// segundo toque se descarta. Es `internal` para poder probarlo (OnlyWhenResumedTest).
internal fun <T> LifecycleOwner.onlyWhenResumed(block: (T) -> Unit): (T) -> Unit = { value ->
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) block(value)
}
