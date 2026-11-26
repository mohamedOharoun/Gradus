package es.ulpgc.pamn.dominio.data

import android.content.Context
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.AccountGateways
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.CachingCourseGateway
import es.ulpgc.pamn.dominio.course.CourseGateways
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.data.progress.ProgressDatabase
import es.ulpgc.pamn.dominio.data.progress.ProgressModule

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

    private var appContext: Context? = null

    // La llama GradusApplication.onCreate. Solo la primera vez cuenta.
    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    // Progreso e inscripciones, guardados en el móvil (Room). Un solo objeto Room con varias caras: el
    // MISMO almacén como ProgressStore y como EnrollmentStore.
    private val progress by lazy {
        val context = checkNotNull(appContext) { "AppContainer.init(context) must be called first (GradusApplication)" }
        ProgressModule(ProgressDatabase.create(context), sessionStore)
    }
    val progressStore: ProgressStore get() = progress.progressStore
    val enrollmentStore: EnrollmentStore get() = progress.enrollmentStore

    // La sesión iniciada. De momento en memoria: se pierde al cerrar la app (tarea 6: guardarla cifrada).
    val sessionStore: SessionStore by lazy { InMemorySessionStore() }
}
