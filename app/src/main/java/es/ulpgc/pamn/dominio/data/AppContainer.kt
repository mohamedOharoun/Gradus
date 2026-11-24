package es.ulpgc.pamn.dominio.data

import android.content.Context
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.AccountGateways
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.CachingCourseGateway
import es.ulpgc.pamn.dominio.course.CourseGateways
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.settings.SettingsStore

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

    // Preferencias de Ajustes (modo oscuro). Se recuerdan al cerrar la app.
    val settingsStore: SettingsStore by lazy {
        SharedPreferencesSettingsStore(appContext.getSharedPreferences("settings", Context.MODE_PRIVATE))
    }
}
