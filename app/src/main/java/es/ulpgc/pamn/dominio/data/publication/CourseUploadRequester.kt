package es.ulpgc.pamn.dominio.data.publication

// Puerto Command en app/: pide encolar o cancelar el trabajo en segundo plano de un borrador
// (validar o publicar) sin esperar al resultado. Desacopla los ViewModels y UserDraftsCleaner de
// WorkManager; la implementa WorkUploadRequester.
interface CourseUploadRequester {
    fun enqueue(draftId: String)
    fun cancel(draftId: String)
}
