package es.ulpgc.pamn.dominio.data.progress

// Pide sincronizar el progreso con el servidor sin esperar al resultado (no es suspend): quien la
// llama sigue a lo suyo. Es lo único que reciben los ViewModels; la implementa ProgressSyncLauncher.
// fun interface: en los tests basta una lambda, p. ej. ProgressSyncRequester { requests++ }.
fun interface ProgressSyncRequester {
    fun requestSync()
}
