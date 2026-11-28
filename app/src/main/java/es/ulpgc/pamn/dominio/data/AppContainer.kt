package es.ulpgc.pamn.dominio.data

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.AccountGateways
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.CachingCourseGateway
import es.ulpgc.pamn.dominio.course.CourseGateways
import es.ulpgc.pamn.dominio.course.OfflineCourseGateway
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressGateways
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.SessionProgressSync
import es.ulpgc.pamn.dominio.course.progress.UnenrollFromCourse
import es.ulpgc.pamn.dominio.course.publication.CourseDraftStore
import es.ulpgc.pamn.dominio.course.publication.CoursePublicationGateway
import es.ulpgc.pamn.dominio.course.publication.CoursePublicationGateways
import es.ulpgc.pamn.dominio.course.publication.ProcessCourseDraft
import es.ulpgc.pamn.dominio.data.course.CourseCopyLauncher
import es.ulpgc.pamn.dominio.data.course.CourseCopyRequester
import es.ulpgc.pamn.dominio.data.course.RoomCourseCopyStore
import es.ulpgc.pamn.dominio.data.notes.AndroidPhotoFiles
import es.ulpgc.pamn.dominio.data.notes.GradusDatabase
import es.ulpgc.pamn.dominio.data.notes.RoomNotesStore
import es.ulpgc.pamn.dominio.data.progress.ProgressDatabase
import es.ulpgc.pamn.dominio.data.progress.ProgressModule
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.data.progress.WorkSyncRequester
import es.ulpgc.pamn.dominio.data.publication.ClearUserDrafts
import es.ulpgc.pamn.dominio.data.publication.CourseDraftFiles
import es.ulpgc.pamn.dominio.data.publication.CourseDraftStorage
import es.ulpgc.pamn.dominio.data.publication.CourseUploadRequester
import es.ulpgc.pamn.dominio.data.publication.RoomCourseDraftStore
import es.ulpgc.pamn.dominio.data.publication.UserDraftsCleaner
import es.ulpgc.pamn.dominio.data.publication.WorkUploadRequester
import es.ulpgc.pamn.dominio.data.session.DataStoreSessionStore
import es.ulpgc.pamn.dominio.data.session.SessionMonitor
import es.ulpgc.pamn.dominio.data.session.SessionSignOut
import es.ulpgc.pamn.dominio.data.session.SessionUpdate
import es.ulpgc.pamn.dominio.data.settings.AndroidLanguageSetting
import es.ulpgc.pamn.dominio.data.settings.LanguageSetting
import es.ulpgc.pamn.dominio.data.submission.AndroidSubmissionPhotos
import es.ulpgc.pamn.dominio.data.submission.SubmissionPhotos
import es.ulpgc.pamn.dominio.notes.NotesStore
import es.ulpgc.pamn.dominio.settings.SettingsStore
import es.ulpgc.pamn.dominio.submission.SubmissionGateway
import es.ulpgc.pamn.dominio.submission.SubmissionGateways
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import java.io.File

// Composition root (inyección manual): el único sitio de la app que elige las implementaciones
// concretas, como App.java en el backend. El resto recibe interfaces por parámetro.
// Para probar la app sin backend: AccountGateway = FakeAccountGateway() y
// CachingCourseGateway(FakeCourseGateway(listOf(demoCourse))).
object AppContainer {

    // Backend local a través de «adb reverse tcp:7070 tcp:7070» (lo hace gradus.sh / gradus.ps1):
    // el localhost del móvil lleva al del ordenador. No usamos 10.0.2.2 porque desde Android 17
    // (targetSdk 37) la red local necesita el permiso ACCESS_LOCAL_NETWORK; localhost no.
    // Sirve igual en el emulador y en un móvil real conectado por USB.
    private const val BASE_URL = "http://localhost:7070"

    // lazy: se crea la primera vez que alguien lo pide, y una sola vez para toda la app.
    val accountGateway: AccountGateway by lazy { AccountGateways.http(BASE_URL) }

    // El catálogo de cursos (buscar y descargar). No necesita sesión, pero guarda copias en Room
    // (necesita init). Cadena Decorator: caché en memoria → copia local en Room → HTTP.
    val courseGateway: CachingCourseGateway by lazy {
        val copies = RoomCourseCopyStore(progressDatabase.courseCopyDao())
        CachingCourseGateway(OfflineCourseGateway(CourseGateways.http(BASE_URL), copies))
    }

    private var appContext: Context? = null

    private fun requireContext(): Context =
        checkNotNull(appContext) { "AppContainer.init(context) must be called first (GradusApplication)" }

    // La llama GradusApplication.onCreate. Solo la primera vez cuenta.
    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    // Única instancia de la base Room compartida entre el progreso y las copias de cursos (12b-1),
    // sin acoplar courseGateway a sessionStore. Esquema congelado v2 en app/schemas/.
    private val progressDatabase: ProgressDatabase by lazy {
        ProgressDatabase.create(requireContext())
    }

    // Progreso e inscripciones, guardados en el móvil (Room). Un solo objeto Room con varias caras: el
    // MISMO almacén como ProgressStore y como EnrollmentStore.
    private val progress by lazy {
        ProgressModule(progressDatabase, sessionStore, ProgressGateways.http(BASE_URL))
    }
    val progressStore: ProgressStore get() = progress.progressStore
    val enrollmentStore: EnrollmentStore get() = progress.enrollmentStore
    val sessionProgressSync: SessionProgressSync get() = progress.sessionSync
    val unenrollFromCourse: UnenrollFromCourse get() = progress.unenroll

    // Pide sincronizar el progreso con WorkManager (los ViewModels lo reciben como ProgressSyncRequester).
    val progressSync: ProgressSyncRequester by lazy {
        WorkSyncRequester(WorkManager.getInstance(requireContext()))
    }

    // Ámbito de la app para tareas en segundo plano que no deben cancelarse al salir de una
    // pantalla (precarga de copias de cursos tras inscribirse).
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Descarga y guarda la copia local del curso tras inscribirse (los ViewModels reciben la interfaz).
    val courseCopy: CourseCopyRequester by lazy { CourseCopyLauncher(courseGateway, appScope) }

    // La sesión iniciada: guardada cifrada en el móvil (DataStore + Keystore), así que sobrevive a cerrar
    // la app. Un solo objeto para toda la app: DataStore no admite dos activos sobre el mismo fichero.
    private val persistentSessions: DataStoreSessionStore by lazy { DataStoreSessionStore.create(requireContext()) }

    // Decorator sobre DataStoreSessionStore: distingue cierre voluntario (Ajustes / Eliminar cuenta) de
    // pérdida inesperada (401) sin tocar el puerto SessionStore de sharedLogic.
    private val sessionMonitor: SessionMonitor by lazy { SessionMonitor(persistentSessions, persistentSessions.session) }

    // El resto de la app solo ve el puerto SessionStore.
    val sessionStore: SessionStore get() = sessionMonitor

    // Comando de salida voluntaria: solo lo reciben SettingsViewModel y DeleteAccountViewModel.
    val sessionSignOut: SessionSignOut get() = sessionMonitor

    // Cada cambio de la sesión (fuera del puerto): solo lo usa el arranque (StartViewModel). Es el
    // Flow del MISMO DataStore que sessionStore, enriquecido con el motivo de salida.
    val sessionUpdates: Flow<SessionUpdate> get() = sessionMonitor.updates

    // Base Room separada para las notas con cámara (docs/tareas/notas-camara.md); las fotos van aparte.
    private val database: GradusDatabase by lazy {
        Room.databaseBuilder(requireContext(), GradusDatabase::class.java, "gradus.db").build()
    }

    // Las notas con cámara de cada módulo. Se quedan en el móvil.
    val notesStore: NotesStore by lazy { RoomNotesStore(database.notes()) }

    // Las fotos de las notas, como ficheros en la carpeta privada de la app (files/notes/…).
    // Las de la cámara se hacen primero en la caché (captures/) y pasan a files/notes/ al guardarlas.
    val notePhotos: AndroidPhotoFiles by lazy {
        val context = requireContext()
        AndroidPhotoFiles(context.contentResolver, context.filesDir, captures = File(context.cacheDir, "captures"))
    }

    // Entregas con foto (docs/tareas/ejercicio-foto.md): el servidor y las fotos preparadas, temporales
    // en la caché (cache/submissions/), que se borran al entregarlas o salir del paso (decisión K).
    val submissionGateway: SubmissionGateway by lazy { SubmissionGateways.http(BASE_URL) }
    val submissionPhotos: SubmissionPhotos by lazy {
        val context = requireContext()
        // La cámara escribe en cache/captures/, la misma carpeta que la de las notas (AndroidPhotoFiles).
        AndroidSubmissionPhotos(
            context.contentResolver,
            File(context.cacheDir, "submissions"),
            captures = File(context.cacheDir, "captures"),
        )
    }

    // Trabajo que debe acabar aunque se cierre la pantalla que lo pidió (borrar fotos temporales).
    // SupervisorJob: si una tarea falla, las demás siguen.
    val backgroundScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // El idioma de la app (docs/tareas/ajustes.md, fase 3b). Lo recuerda Android (LocaleManager).
    val languageSetting: LanguageSetting by lazy { AndroidLanguageSetting(requireContext()) }

    // Preferencias de Ajustes (modo oscuro). Se recuerdan al cerrar la app.
    val settingsStore: SettingsStore by lazy {
        SharedPreferencesSettingsStore(requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE))
    }

    // Subida de cursos (docs/tareas/subida-cursos.md, parte C): red, borradores locales en Room y
    // ficheros privados, WorkManager por borrador, coordinador puro y limpieza al eliminar cuenta.
    val coursePublicationGateway: CoursePublicationGateway by lazy {
        CoursePublicationGateways.http(BASE_URL)
    }

    val courseDraftStore: CourseDraftStore by lazy {
        RoomCourseDraftStore(progressDatabase.courseDraftDao(), sessionStore)
    }

    val courseDraftFiles: CourseDraftStorage by lazy {
        CourseDraftFiles.android(requireContext())
    }

    val courseUploadRequester: CourseUploadRequester by lazy {
        WorkUploadRequester(WorkManager.getInstance(requireContext()))
    }

    val processCourseDraft: ProcessCourseDraft by lazy {
        ProcessCourseDraft(
            drafts = courseDraftStore,
            contents = courseDraftFiles,
            sessions = sessionStore,
            gateway = coursePublicationGateway,
            cacheInvalidator = courseGateway,
        )
    }

    val clearUserDrafts: ClearUserDrafts by lazy {
        UserDraftsCleaner(courseDraftStore, courseDraftFiles, courseUploadRequester::cancel)
    }
}
