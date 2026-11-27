package es.ulpgc.pamn.dominio.data

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// En la JVM nadie llama a AppContainer.init (no hay GradusApplication): sirve para comprobar qué
// pasa si se usa el progreso antes de tiempo. Estos tests no deben llamar nunca a init.
class AppContainerInitTest {

    @Test
    fun progress_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.progressStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun enrollment_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.enrollmentStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    // La sesión también necesita Context (11c): se guarda en un fichero (DataStore).
    @Test
    fun session_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.sessionStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun session_updates_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.sessionUpdates }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun session_sign_out_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.sessionSignOut }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun notes_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.notesStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun settings_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.settingsStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun progress_sync_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.progressSync }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun session_progress_sync_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.sessionProgressSync }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    // 12b-1: el catálogo guarda copias en Room, así que también necesita Context.
    @Test
    fun course_gateway_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.courseGateway }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    // 12b-2: la precarga de cursos usa courseGateway (Room), así que también necesita Context.
    @Test
    fun course_copy_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.courseCopy }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    // Subida de cursos (3b): borradores en Room, ficheros privados y WorkManager necesitan Context.
    @Test
    fun course_draft_store_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.courseDraftStore }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun course_draft_files_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.courseDraftFiles }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun course_upload_requester_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.courseUploadRequester }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun process_course_draft_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.processCourseDraft }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }

    @Test
    fun clear_user_drafts_before_init_fails_saying_what_to_call() {
        val failure = assertThrows(IllegalStateException::class.java) { AppContainer.clearUserDrafts }

        assertTrue(failure.message.orEmpty().contains("AppContainer.init(context)"))
    }
}
