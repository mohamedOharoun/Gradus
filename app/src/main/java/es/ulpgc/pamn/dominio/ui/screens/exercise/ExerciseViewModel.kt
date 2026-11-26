package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.exercise.ExerciseState
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.StudyAccess
import es.ulpgc.pamn.dominio.course.progress.studyAccess
import es.ulpgc.pamn.dominio.course.study.ModuleStudy
import es.ulpgc.pamn.dominio.course.study.PhotoStep
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.data.submission.SubmissionPhotos
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.PhotoSubmission
import es.ulpgc.pamn.dominio.submission.SubmissionGateway
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

// Avisos de la foto que no son parte del estado del paso: se está preparando, o no se pudo leer.
enum class PhotoNotice { PREPARING, UNREADABLE }

// Guarda el estudio de un módulo mientras dura la pantalla (sobrevive al giro). Toda la lógica
// está en ModuleStudy (sharedLogic): aquí solo se le pasan los toques y, al aprobar la prueba de
// dominio, se apunta en el progreso (el camino lo escucha y desbloquea lo siguiente).
// Las tareas con foto (docs/tareas/ejercicio-foto.md): preparar la foto, entregarla y borrar las
// temporales que dejan de hacer falta (decisión K). `cleanup` es un ámbito que sobrevive al ViewModel:
// al cerrarse la pantalla, viewModelScope se cancela y no podría borrarlas.
class ExerciseViewModel(
    private val course: Course,
    moduleId: String,
    private val progress: ProgressStore,
    enrollment: EnrollmentStore,
    private val progressSync: ProgressSyncRequester,
    private val submissions: SubmissionGateway,
    private val sessions: SessionStore,
    private val photos: SubmissionPhotos,
    private val cleanup: CoroutineScope,
    private val random: Random = Random.Default,
) : ViewModel() {

    companion object {
        fun factory(
            course: Course,
            moduleId: String,
            progress: ProgressStore,
            enrollment: EnrollmentStore,
            progressSync: ProgressSyncRequester,
            submissions: SubmissionGateway,
            sessions: SessionStore,
            photos: SubmissionPhotos,
            cleanup: CoroutineScope,
            random: Random = Random.Default,
        ) = viewModelFactory {
            initializer {
                ExerciseViewModel(
                    course, moduleId, progress, enrollment, progressSync,
                    submissions, sessions, photos, cleanup, random,
                )
            }
        }
    }

    // La puerta: null mientras se lee la inscripción (no se enseña nada); NotEnrolled, la pantalla
    // vuelve atrás sin componer la práctica; Allowed, se enseña. Basta con el primer valor: no hay
    // «darse de baja» (decisión 7), así que quien está inscrito al abrir lo sigue estando.
    var access by mutableStateOf<StudyAccess?>(null)
        private set

    // null: el módulo no existe en el curso (la pantalla vuelve al camino).
    var study by mutableStateOf(ModuleStudy.start(course, moduleId, random))
        private set

    var photoNotice by mutableStateOf<PhotoNotice?>(null)
        private set

    // Decisión Z: un curso que no está en el servidor (el de ejemplo) no tiene versión a la que entregar.
    val canSubmitPhotos: Boolean get() = course.publication != null

    init {
        viewModelScope.launch { access = studyAccess(enrollment.enrolled(course.id).first()) }
    }

    fun onChoose(option: Int) = update { it.choose(option) }
    fun onLeftClick(index: Int) = update { it.tapLeft(index) }
    fun onRightClick(index: Int) = update { it.tapRight(index) }
    fun onCheck() = update { it.check() }

    fun onContinue() {
        val current = study ?: return
        val next = current.next(random)
        change(next)
        // Solo en el momento de aprobar (no en cada «Continuar» posterior).
        if (next.isPassed && !current.isPassed) {
            viewModelScope.launch {
                progress.markMastered(next.course.id, next.module.id)
                progressSync.requestSync() // sube lo dominado; si markMastered lanza, no se pide
            }
        }
    }

    fun onStartTest() = change(study?.startTest(random))

    fun onRetry() = change(study?.retry(random))

    // ── Tareas con foto ──

    // Una foto de la galería (`source`, una URI como texto): se prepara y pasa a ser la del paso.
    fun onPhotoChosen(source: String) = usePhoto { photos.prepare(source) }

    // Una foto de la cámara (`capture`, su fichero temporal): igual, y la de la cámara se tira. Si el paso
    // ya no acepta foto (se está entregando), se tira sin prepararla.
    fun onPhotoTaken(capture: String) {
        if (!usePhoto { photos.prepareCapture(capture) }) cleanup.launch { photos.discardCapture(capture) }
    }

    // Prepara la foto con `prepare` y la pone en el paso actual. false si el paso no la acepta ahora.
    private fun usePhoto(prepare: suspend () -> String?): Boolean {
        val photo = study?.currentPhoto ?: return false
        if (!photo.acceptsPhoto || photoNotice == PhotoNotice.PREPARING) return false
        photoNotice = PhotoNotice.PREPARING
        viewModelScope.launch {
            val path = prepare()
            photoNotice = if (path == null) PhotoNotice.UNREADABLE else null
            if (path != null) {
                change(study?.updatePhoto(photo.photo.id) { it.take(path) })
                // Si el paso no la aceptó (ya no está en uso), no se queda en el móvil.
                if (path !in study?.photoPaths.orEmpty()) cleanup.launch { photos.delete(path) }
            }
        }
        return true
    }

    fun onDiscardPhoto() = changePhoto { it.discard() }

    // «Entregar» o «Reintentar»: se envía la foto y la respuesta va a su paso, aunque llegue tarde.
    fun onSubmitPhoto() {
        val photo = study?.currentPhoto ?: return
        val path = photo.photoPath
        if (!canSubmitPhotos || !photo.canSend || path == null) return
        val stepId = photo.photo.id
        changePhoto { it.send() }
        viewModelScope.launch {
            val outcome = submit(stepId, path)
            change(study?.updatePhoto(stepId) { it.finished(outcome) })
        }
    }

    fun onSubmitAgain() = changePhoto { it.again() }

    // Un solo toque: la deja para más tarde y pasa al paso siguiente (decisión S). Solo avanza si de
    // verdad había una foto que dejar: un toque tardío en una lección no la salta.
    fun onSubmitPhotoLater() {
        val current = study ?: return
        val later = current.submitPhotoLater()
        if (later != current) change(later.next(random))
    }

    // Al salir del módulo: las fotos temporales que queden se borran (decisión K).
    fun onLeave() = deleteUnused(study?.photoPaths.orEmpty())

    override fun onCleared() = onLeave()

    private suspend fun submit(stepId: String, path: String): SubmitOutcome {
        // Decisión Y: sin sesión no se envía; se avisa como sesión caducada.
        val session = sessions.current() ?: return SubmitOutcome.SessionExpired
        // Android puede vaciar la caché: si la foto ya no está, hay que hacer otra.
        val bytes = photos.read(path)?.takeIf { it.isNotEmpty() } ?: return SubmitOutcome.PhotoRejected(PhotoRejection.MISSING)
        val version = course.publication?.version ?: return SubmitOutcome.NotFound
        return submissions.submit(session, PhotoSubmission(course.id, version, stepId, bytes))
    }

    private fun changePhoto(edit: (PhotoStep) -> PhotoStep) {
        val photo = study?.currentPhoto ?: return
        change(study?.updatePhoto(photo.photo.id, edit))
    }

    private fun update(edit: (ExerciseState) -> ExerciseState) = change(study?.update(edit))

    // Todo cambio pasa por aquí: las fotos que dejan de estar en uso se borran del móvil (decisión K).
    private fun change(next: ModuleStudy?) {
        val before = study?.photoPaths.orEmpty()
        study = next
        deleteUnused(before - next?.photoPaths.orEmpty())
    }

    private fun deleteUnused(paths: Set<String>) {
        if (paths.isNotEmpty()) cleanup.launch { paths.forEach { photos.delete(it) } }
    }
}
