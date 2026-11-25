package es.ulpgc.pamn.dominio.ui.screens.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.data.notes.PhotoFiles
import es.ulpgc.pamn.dominio.notes.CaptionCheck
import es.ulpgc.pamn.dominio.notes.Note
import es.ulpgc.pamn.dominio.notes.NotesStore
import es.ulpgc.pamn.dominio.notes.checkCaption
import kotlinx.coroutines.launch
import java.util.UUID

// En qué punto está la cámara (docs/tareas/notas-camara.md, boceto 3). Máquina de estados:
// NeedsPermission → Ready → Capturing → Reviewing → (Guardar) Closed  /  (Usar foto) Used  /  (Repetir) Ready.
sealed interface CameraStage {

    // Falta el permiso de cámara (decisión X). `denied`: ya se pidió y se dijo que no;
    // `permanently`: Android ya no lo volverá a preguntar (solo queda ir a los ajustes del sistema).
    data class NeedsPermission(val denied: Boolean = false, val permanently: Boolean = false) : CameraStage

    // Vista previa encendida, esperando a que se pulse el disparador.
    data object Ready : CameraStage

    // La cámara está escribiendo la foto en `capture` (un fichero temporal).
    data class Capturing(val capture: String) : CameraStage

    // Revisar la foto hecha: «Repetir» o «Guardar», con el pie opcional (decisión Y).
    data class Reviewing(
        val capture: String,
        val caption: String = "",
        val captionTooLong: Boolean = false,
        val saving: Boolean = false,
        val saveFailed: Boolean = false,
    ) : CameraStage

    // Guardada o abandonada: la navegación vuelve a «Mis notas».
    data object Closed : CameraStage

    // La foto de una entrega, ya elegida: la navegación la lleva al paso con foto, que ahora es su dueño.
    data class Used(val capture: String) : CameraStage
}

// Para qué es la foto (patrón Strategy: el mismo flujo de cámara, con distinto final al «Guardar»).
sealed interface CameraPurpose {
    // Una nota de «Mis notas»: pie opcional y «Guardar» la guarda en las notas del módulo.
    data class Note(val courseId: String, val moduleId: String, val notes: NotesStore) : CameraPurpose

    // La foto de una entrega (docs/tareas/ejercicio-foto.md, decisión W): sin pie; «Usar foto» la devuelve.
    data object Submission : CameraPurpose
}

// `captureFailed`: la última foto no se pudo hacer (se avisa y se puede volver a disparar).
// `forSubmission`: la foto es para entregar (sin pie, «Usar foto» y otro texto al pedir el permiso).
data class CameraUiState(
    val stage: CameraStage = CameraStage.NeedsPermission(),
    val captureFailed: Boolean = false,
    val forSubmission: Boolean = false,
)

// La cámara de las notas de un módulo o de la foto de una entrega (`purpose`). No sabe nada de CameraX:
// la pantalla hace la foto donde le dice `Capturing.capture` y avisa con onCaptured / onCaptureFailed.
// Así se prueba en el ordenador.
class CameraViewModel(
    private val purpose: CameraPurpose,
    private val photos: PhotoFiles,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    companion object {
        fun factory(purpose: CameraPurpose, photos: PhotoFiles) =
            viewModelFactory { initializer { CameraViewModel(purpose, photos) } }
    }

    var state by mutableStateOf(CameraUiState(forSubmission = purpose is CameraPurpose.Submission))
        private set

    private var stage: CameraStage
        get() = state.stage
        set(value) {
            state = state.copy(stage = value)
        }

    // Lo que contestó Android al pedir el permiso (o al volver de sus ajustes). Solo cuenta mientras
    // se está pidiendo: con una foto a medias, no se deshace nada.
    fun onPermission(granted: Boolean, canAskAgain: Boolean = true) {
        if (stage !is CameraStage.NeedsPermission) return
        stage = if (granted) CameraStage.Ready else CameraStage.NeedsPermission(denied = true, permanently = !canAskAgain)
    }

    // Disparar: solo con la cámara lista (dos toques rápidos hacen una foto).
    fun onShutter() {
        if (stage != CameraStage.Ready) return
        state = state.copy(stage = CameraStage.Capturing(photos.newCapture()), captureFailed = false)
    }

    fun onCaptured() {
        val capturing = stage as? CameraStage.Capturing ?: return
        stage = CameraStage.Reviewing(capturing.capture)
    }

    // La cámara no pudo hacer la foto: se tira lo que haya escrito y se vuelve a la vista previa.
    fun onCaptureFailed() {
        val capturing = stage as? CameraStage.Capturing ?: return
        state = state.copy(stage = CameraStage.Ready, captureFailed = true)
        viewModelScope.launch { photos.discardCapture(capturing.capture) }
    }

    // «Repetir»: la foto temporal se tira.
    fun onRetake() {
        val review = stage as? CameraStage.Reviewing ?: return
        if (review.saving) return
        stage = CameraStage.Ready
        viewModelScope.launch { photos.discardCapture(review.capture) }
    }

    fun onCaptionChange(caption: String) {
        val review = stage as? CameraStage.Reviewing ?: return
        stage = review.copy(caption = caption, captionTooLong = false)
    }

    // «Guardar» (nota): con un pie válido, la foto pasa a las notas y se guarda la nota.
    // «Usar foto» (entrega): la foto temporal pasa tal cual al paso; ni se guarda ni se tira.
    fun onSave() {
        val review = stage as? CameraStage.Reviewing ?: return
        if (review.saving) return
        when (purpose) {
            is CameraPurpose.Note -> saveNote(review, purpose)
            CameraPurpose.Submission -> stage = CameraStage.Used(review.capture)
        }
    }

    private fun saveNote(review: CameraStage.Reviewing, note: CameraPurpose.Note) {
        val caption = when (val check = checkCaption(review.caption)) {
            is CaptionCheck.Valid -> check.caption
            CaptionCheck.TooLong -> return run { stage = review.copy(captionTooLong = true) }
        }
        stage = review.copy(saving = true, saveFailed = false)
        viewModelScope.launch {
            val path = photos.keepCapture(review.capture)
            if (path == null) {
                stage = review.copy(saving = false, saveFailed = true)
                return@launch
            }
            note.notes.add(Note(newId(), note.courseId, note.moduleId, path, caption, clock()))
            stage = CameraStage.Closed
        }
    }

    // Salir (atrás o ✕). Una foto sin guardar se tira; mientras se guarda, se espera a que termine.
    fun onLeave() {
        when (val current = stage) {
            is CameraStage.Reviewing -> {
                if (current.saving) return
                stage = CameraStage.Closed
                viewModelScope.launch { photos.discardCapture(current.capture) }
            }
            is CameraStage.Capturing -> {
                stage = CameraStage.Closed
                viewModelScope.launch { photos.discardCapture(current.capture) }
            }
            else -> stage = CameraStage.Closed
        }
    }
}
