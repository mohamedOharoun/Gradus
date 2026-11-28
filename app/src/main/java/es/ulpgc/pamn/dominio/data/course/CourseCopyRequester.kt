package es.ulpgc.pamn.dominio.data.course

// Pide descargar y guardar la copia local de un curso sin esperar al resultado (no es suspend):
// quien la llama sigue a lo suyo. Es lo único que recibe CourseDetailViewModel; la implementa
// CourseCopyLauncher (sobre appScope, para que no se cancele al salir de la pantalla).
// Patrón Command (fun interface): en los tests basta una lambda, p. ej. CourseCopyRequester { copies += it }.
fun interface CourseCopyRequester {
    fun requestCopy(courseId: String)
}
