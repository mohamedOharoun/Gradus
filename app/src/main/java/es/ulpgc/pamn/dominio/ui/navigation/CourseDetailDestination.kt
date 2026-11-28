package es.ulpgc.pamn.dominio.ui.navigation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.detail.CourseDetailActions
import es.ulpgc.pamn.dominio.ui.screens.detail.CourseDetailViewModel
import es.ulpgc.pamn.dominio.ui.screens.detail.ScreenCourseDetail

// La ficha de un curso, dentro del grafo Main y sin barra (como el camino). Va en su propio fichero
// para que MainNavGraph.kt, que también edita el compañero, solo tenga una línea más
// (`courseDetailDestination(navController)`) y no haya conflictos al fusionar.
// Se llega desde Buscar (Route.CourseDetail(id)); «Empezar el curso» abre el camino y «atrás»
// vuelve a donde se estaba.
internal fun NavGraphBuilder.courseDetailDestination(navController: NavController) {
    composable<Route.CourseDetail> { entry ->
        val route = entry.toRoute<Route.CourseDetail>()
        val detailViewModel: CourseDetailViewModel = viewModel(
            factory = CourseDetailViewModel.factory(
                courseId = route.courseId,
                gateway = AppContainer.courseGateway,
                enrollment = AppContainer.enrollmentStore,
                progress = AppContainer.progressStore,
                progressSync = AppContainer.progressSync,
                courseCopy = AppContainer.courseCopy,
                unenroll = AppContainer.unenrollFromCourse,
            ),
        )
        ScreenCourseDetail(
            state = detailViewModel.state,
            actions = CourseDetailActions(
                // dropUnlessResumed: dos toques rápidos no abren el camino dos veces.
                onStart = dropUnlessResumed { navController.navigate(Route.Course(route.courseId)) },
                onRetry = detailViewModel::onRetry,
                onBack = dropUnlessResumed { navController.popBackStack() },
            ),
        )
    }
}
