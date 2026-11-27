package es.ulpgc.pamn.dominio.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseItem
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.screens.ScreenMyCourses
import es.ulpgc.pamn.dominio.ui.screens.mycourses.MyCoursesViewModel
import es.ulpgc.pamn.dominio.ui.screens.mycourses.ScreenCourseGuide
import es.ulpgc.pamn.dominio.ui.screens.mycourses.actions

// Algunos dispositivos no reconocen `application/json` en el selector SAF; incluir `text/plain` y
// `*/*` evita bloquear la selección mientras CourseDraftFiles valida el contenido real.
private val COURSE_DOCUMENT_MIME_TYPES = arrayOf("application/json", "text/plain", "*/*")

// Pestaña «Mis cursos» conectada a MyCoursesViewModel y al selector de documentos SAF.
// Vive en su propio fichero para mantener mínimos los cambios en MainNavGraph.kt.
@Composable
internal fun MyCoursesRoute(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: MyCoursesViewModel = viewModel(
        factory = MyCoursesViewModel.factory(
            gateway = AppContainer.coursePublicationGateway,
            drafts = AppContainer.courseDraftStore,
            storage = AppContainer.courseDraftFiles,
            uploads = AppContainer.courseUploadRequester,
            sessions = AppContainer.sessionStore,
        ),
    )
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.onFileSelected(uri.toString())
        } else {
            viewModel.onFileSelectionCancelled()
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val onUploadCourse = remember(lifecycleOwner, viewModel, openDocument) {
        {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                viewModel.onPrepareUploadNew()
                openDocument.launch(COURSE_DOCUMENT_MIME_TYPES)
            }
        }
    }
    val onUploadNewVersion = remember(lifecycleOwner, viewModel, openDocument) {
        lifecycleOwner.onlyWhenResumed { item: AuthorCourseItem ->
            viewModel.onPrepareUploadVersion(item)
            openDocument.launch(COURSE_DOCUMENT_MIME_TYPES)
        }
    }
    val onOpenGuide = dropUnlessResumed { navController.navigate(Route.CourseGuide) }

    ScreenMyCourses(
        state = viewModel.uiState,
        actions = viewModel.actions(
            onUploadCourse = onUploadCourse,
            onUploadNewVersion = onUploadNewVersion,
            onOpenGuide = onOpenGuide,
        ),
        modifier = modifier,
    )
}

// Pantalla provisional de la guía de IA, dentro del grafo Main y sin barra inferior.
internal fun NavGraphBuilder.courseGuideDestination(navController: NavController) {
    composable<Route.CourseGuide> {
        ScreenCourseGuide(
            onBack = dropUnlessResumed { navController.popBackStack() },
        )
    }
}
