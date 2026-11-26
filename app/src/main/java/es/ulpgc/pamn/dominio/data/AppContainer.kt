package es.ulpgc.pamn.dominio.data

import android.content.Context
import androidx.room.Room
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.AccountGateways
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.CachingCourseGateway
import es.ulpgc.pamn.dominio.course.CourseGateways
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.data.notes.AndroidPhotoFiles
import es.ulpgc.pamn.dominio.data.notes.GradusDatabase
import es.ulpgc.pamn.dominio.data.notes.RoomNotesStore
import es.ulpgc.pamn.dominio.data.settings.AndroidLanguageSetting
import es.ulpgc.pamn.dominio.data.settings.LanguageSetting
import es.ulpgc.pamn.dominio.data.submission.AndroidSubmissionPhotos
import es.ulpgc.pamn.dominio.data.submission.SubmissionPhotos
import es.ulpgc.pamn.dominio.notes.NotesStore
import es.ulpgc.pamn.dominio.settings.SettingsStore
import es.ulpgc.pamn.dominio.submission.SubmissionGateway
import es.ulpgc.pamn.dominio.submission.SubmissionGateways
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    // El catálogo de cursos (buscar y descargar). No necesita sesión. Con caché: el camino y la
    // práctica comparten el curso descargado sin volver a pedirlo.
    val courseGateway: CachingCourseGateway by lazy { CachingCourseGateway(CourseGateways.http(BASE_URL)) }

    // Qué módulos ha dominado el usuario. De momento en memoria: se pierde al cerrar la app.
    val progressStore: ProgressStore by lazy { InMemoryProgressStore() }

    // La sesión iniciada. De momento en memoria: se pierde al cerrar la app (tarea 6: guardarla cifrada).
    val sessionStore: SessionStore by lazy { InMemorySessionStore() }

    // Guardar en el móvil necesita el Context de la app. MainActivity llama a init() al arrancar,
    // antes de pedir nada; applicationContext vive lo que la app (no se queda con la Activity).
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // La base de datos SQLite del móvil (Room), una sola para toda la app. Hoy guarda las notas con
    // cámara (docs/tareas/notas-camara.md); las fotos van aparte, como ficheros.
    private val database: GradusDatabase by lazy {
        Room.databaseBuilder(appContext, GradusDatabase::class.java, "gradus.db").build()
    }

    // Las notas con cámara de cada módulo. Se quedan en el móvil.
    val notesStore: NotesStore by lazy { RoomNotesStore(database.notes()) }

    // Las fotos de las notas, como ficheros en la carpeta privada de la app (files/notes/…).
    // Las de la cámara se hacen primero en la caché (captures/) y pasan a files/notes/ al guardarlas.
    val notePhotos: AndroidPhotoFiles by lazy {
        AndroidPhotoFiles(appContext.contentResolver, appContext.filesDir, captures = File(appContext.cacheDir, "captures"))
    }

    // Entregas con foto (docs/tareas/ejercicio-foto.md): el servidor y las fotos preparadas, temporales
    // en la caché (cache/submissions/), que se borran al entregarlas o salir del paso (decisión K).
    val submissionGateway: SubmissionGateway by lazy { SubmissionGateways.http(BASE_URL) }
    val submissionPhotos: SubmissionPhotos by lazy {
        // La cámara escribe en cache/captures/, la misma carpeta que la de las notas (AndroidPhotoFiles).
        AndroidSubmissionPhotos(
            appContext.contentResolver,
            File(appContext.cacheDir, "submissions"),
            captures = File(appContext.cacheDir, "captures"),
        )
    }

    // Trabajo que debe acabar aunque se cierre la pantalla que lo pidió (borrar fotos temporales).
    // SupervisorJob: si una tarea falla, las demás siguen.
    val backgroundScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // El idioma de la app (docs/tareas/ajustes.md, fase 3b). Lo recuerda Android (LocaleManager).
    val languageSetting: LanguageSetting by lazy { AndroidLanguageSetting(appContext) }

    // Preferencias de Ajustes (modo oscuro). Se recuerdan al cerrar la app.
    val settingsStore: SettingsStore by lazy {
        SharedPreferencesSettingsStore(appContext.getSharedPreferences("settings", Context.MODE_PRIVATE))
    }
}
