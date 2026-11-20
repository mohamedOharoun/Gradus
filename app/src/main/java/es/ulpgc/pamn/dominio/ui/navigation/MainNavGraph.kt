package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.ui.components.MainScaffold
import es.ulpgc.pamn.dominio.ui.screens.ScreenHome
import es.ulpgc.pamn.dominio.ui.screens.ScreenMyCourses
import es.ulpgc.pamn.dominio.ui.screens.ScreenSearch
import es.ulpgc.pamn.dominio.ui.screens.ScreenSettings
import es.ulpgc.pamn.dominio.ui.screens.course.CourseActions
import es.ulpgc.pamn.dominio.ui.screens.course.CourseViewModel
import es.ulpgc.pamn.dominio.ui.screens.course.ScreenCourse
import es.ulpgc.pamn.dominio.ui.screens.exercise.ExerciseActions
import es.ulpgc.pamn.dominio.ui.screens.exercise.ExerciseViewModel
import es.ulpgc.pamn.dominio.ui.screens.exercise.ScreenExercise

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
        tab<Route.Home>(TopLevelDestination.HOME, navController) {
            ScreenHome(
                onOpenCourse = dropUnlessResumed { navController.navigate(Route.Course(demoCourse.id)) },
                modifier = it,
            )
        }
        tab<Route.Search>(TopLevelDestination.SEARCH, navController) { ScreenSearch(it) }
        tab<Route.MyCourses>(TopLevelDestination.MY_COURSES, navController) { ScreenMyCourses(it) }
        tab<Route.Settings>(TopLevelDestination.SETTINGS, navController) { ScreenSettings(it) }
        // Sin barra: no es una pestaña, se entra desde una y «atrás» vuelve a ella.
        composable<Route.Course> {
            // Provisional: solo existe el curso de ejemplo, así que no hace falta leer el courseId.
            // Con el backend, el ViewModel pedirá el curso de la ruta a un CourseGateway.
            val courseViewModel: CourseViewModel = viewModel(factory = CourseViewModel.factory(demoCourse, demoMastered))
            // Módulo desbloqueado tocado: se abre su práctica y se borra el evento.
            courseViewModel.state.practiceToOpen?.let { moduleId ->
                LaunchedEffect(moduleId) {
                    navController.navigate(Route.Exercise(demoCourse.id, moduleId))
                    courseViewModel.onPracticeOpened()
                }
            }
            ScreenCourse(
                state = courseViewModel.state,
                actions = CourseActions(
                    onModuleClick = courseViewModel::onModuleClick,
                    onNoticeShown = courseViewModel::onNoticeShown,
                    onBack = dropUnlessResumed { navController.popBackStack() },
                ),
            )
        }
        // La práctica de un módulo. Sin barra; «atrás», cerrar y «Volver al camino» vuelven al camino.
        composable<Route.Exercise> { entry ->
            val route = entry.toRoute<Route.Exercise>()
            // Provisional, como en Route.Course: solo existe el curso de ejemplo.
            val exerciseViewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.factory(demoCourse, route.moduleId))
            ScreenExercise(
                state = exerciseViewModel.state,
                actions = ExerciseActions(
                    onChoose = exerciseViewModel::onChoose,
                    onLeftClick = exerciseViewModel::onLeftClick,
                    onRightClick = exerciseViewModel::onRightClick,
                    onCheck = exerciseViewModel::onCheck,
                    onContinue = exerciseViewModel::onContinue,
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
