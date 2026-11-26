package es.ulpgc.pamn.dominio.data

import android.content.Context
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.AccountGateways
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.CachingCourseGateway
import es.ulpgc.pamn.dominio.course.CourseGateways
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressGateways
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.data.progress.ProgressDatabase
import es.ulpgc.pamn.dominio.data.progress.ProgressModule
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncLauncher
import es.ulpgc.pamn.dominio.data.session.DataStoreSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

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

    private fun requireContext(): Context =
        checkNotNull(appContext) { "AppContainer.init(context) must be called first (GradusApplication)" }

    // La llama GradusApplication.onCreate. Solo la primera vez cuenta.
    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    // Progreso e inscripciones, guardados en el móvil (Room). Un solo objeto Room con varias caras: el
    // MISMO almacén como ProgressStore y como EnrollmentStore.
    private val progress by lazy {
        ProgressModule(ProgressDatabase.create(requireContext()), sessionStore, ProgressGateways.http(BASE_URL))
    }
    val progressStore: ProgressStore get() = progress.progressStore
    val enrollmentStore: EnrollmentStore get() = progress.enrollmentStore

    // Ámbito de la app para lo que no debe cancelarse al cerrar una pantalla. SupervisorJob: si un
    // hijo falla, no cancela a los demás.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Pide sincronizar el progreso (los ViewModels lo reciben como ProgressSyncRequester).
    val progressSyncLauncher: ProgressSyncLauncher by lazy {
        ProgressSyncLauncher(progress.sync, sessionStore, appScope)
    }

    // La sesión iniciada: guardada cifrada en el móvil (DataStore + Keystore), así que sobrevive a cerrar
    // la app. Un solo objeto para toda la app: DataStore no admite dos activos sobre el mismo fichero.
    private val sessions: DataStoreSessionStore by lazy { DataStoreSessionStore.create(requireContext()) }

    // El resto de la app solo ve el puerto SessionStore.
    val sessionStore: SessionStore get() = sessions

    // Cada cambio de la sesión (fuera del puerto): solo lo usa el arranque (StartViewModel). Es el
    // Flow del MISMO DataStore que sessionStore.
    val sessionUpdates: Flow<Session?> get() = sessions.session
}
