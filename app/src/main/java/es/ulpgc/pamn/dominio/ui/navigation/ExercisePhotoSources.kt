package es.ulpgc.pamn.dominio.ui.navigation

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import es.ulpgc.pamn.dominio.ui.screens.exercise.ExerciseViewModel

// De dónde sale la foto de una entrega (docs/tareas/ejercicio-foto.md, paso 5d). `onTakePhoto` es null si
// el móvil no tiene cámara (se puede instalar sin ella): entonces solo queda la galería.
internal class ExercisePhotoSources(val onTakePhoto: (() -> Unit)?, val onPickPhoto: () -> Unit)

// La cámara (otra pantalla, que devuelve la foto por el savedStateHandle de `entry`) y la galería (el
// selector de fotos de Android, sin permisos), conectadas al ViewModel del ejercicio. En su propio fichero
// para que MainNavGraph.kt, que también edita el compañero, solo tenga una línea más.
@Composable
internal fun rememberExercisePhotoSources(
    entry: NavBackStackEntry,
    navController: NavController,
    route: Route.Exercise,
    viewModel: ExerciseViewModel,
): ExercisePhotoSources {
    // La foto que dejó la cámara al volver: se pasa al ViewModel una sola vez y se quita.
    val taken by entry.savedStateHandle.getStateFlow<String?>(TAKEN_PHOTO_KEY, null).collectAsStateWithLifecycle()
    LaunchedEffect(taken) {
        taken?.let { capture ->
            entry.savedStateHandle[TAKEN_PHOTO_KEY] = null
            viewModel.onPhotoTaken(capture)
        }
    }
    // Decisión U de notas-camara.md: el selector de fotos no pide permisos; si se cierra sin elegir, null.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoChosen(it.toString()) }
    }
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    return ExercisePhotoSources(
        onTakePhoto = if (hasCamera) dropUnlessResumed { navController.navigate(Route.TakeSubmissionPhoto(route.courseId, route.moduleId)) } else null,
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}
