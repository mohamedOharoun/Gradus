package es.ulpgc.pamn.dominio.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.data.course.CourseCopyLauncher
import es.ulpgc.pamn.dominio.data.progress.WorkSyncRequester
import es.ulpgc.pamn.dominio.data.session.SessionMonitor
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// En los tests instrumentados GradusApplication ya ha llamado a AppContainer.init.
// Esto crea el RoomDatabase real, pero Room no abre el fichero hasta la primera consulta
// (build() solo configura), así que el test no toca ningún dato.
// Con la sesión pasa igual: DataStore no lee ni escribe hasta que alguien pide o guarda la sesión.
// Estos tests NUNCA leen ni guardan la sesión de AppContainer: es la real de la app instalada.
@RunWith(AndroidJUnit4::class)
class AppContainerTest {

    // Lo que la arquitectura pide conservar al mergear: si fueran dos objetos, inscribirse no
    // abriría nada.
    @Test
    fun progress_store_and_enrollment_store_are_the_same_object() {
        assertSame(AppContainer.progressStore, AppContainer.enrollmentStore)
    }

    // 11c + salida unificada: la sesión pasa por un único SessionMonitor sobre DataStoreSessionStore.
    @Test
    fun session_store_and_session_sign_out_are_the_same_session_monitor() {
        assertTrue(AppContainer.sessionStore is SessionMonitor)
        assertSame(AppContainer.sessionStore, AppContainer.sessionSignOut)
    }

    // DataStore no admite dos activos sobre el mismo fichero: tiene que ser un solo objeto.
    @Test
    fun session_store_is_a_single_instance() {
        assertSame(AppContainer.sessionStore, AppContainer.sessionStore)
    }

    // El Flow que usa el arranque sale del mismo SessionMonitor (y DataStore) que ve el resto de la app.
    @Test
    fun session_updates_come_from_the_same_monitor_as_the_session_store() {
        val monitor = AppContainer.sessionStore as SessionMonitor
        assertSame(monitor.updates, AppContainer.sessionUpdates)
    }

    // 13b: la sincronización pasa por WorkManager y es una sola instancia en la app.
    @Test
    fun progress_sync_is_work_sync_requester_single_instance() {
        assertTrue(AppContainer.progressSync is WorkSyncRequester)
        assertSame(AppContainer.progressSync, AppContainer.progressSync)
    }

    // El caso de uso que ejecuta el Worker sale del mismo ProgressModule.
    @Test
    fun session_progress_sync_is_a_single_instance() {
        assertSame(AppContainer.sessionProgressSync, AppContainer.sessionProgressSync)
    }

    // 12b-1: el catálogo envuelve Room (copia local) y caché en memoria en una sola instancia.
    @Test
    fun course_gateway_is_a_single_instance() {
        assertSame(AppContainer.courseGateway, AppContainer.courseGateway)
    }

    // 12b-2: la precarga de cursos al inscribirse corre en appScope y es una sola instancia.
    @Test
    fun course_copy_is_course_copy_launcher_single_instance() {
        assertTrue(AppContainer.courseCopy is CourseCopyLauncher)
        assertSame(AppContainer.courseCopy, AppContainer.courseCopy)
    }

    // Las notas con cámara (GradusDatabase separada) y los ajustes también son instancias únicas.
    @Test
    fun notes_store_and_settings_store_are_single_instances() {
        assertSame(AppContainer.notesStore, AppContainer.notesStore)
        assertSame(AppContainer.settingsStore, AppContainer.settingsStore)
    }
}
