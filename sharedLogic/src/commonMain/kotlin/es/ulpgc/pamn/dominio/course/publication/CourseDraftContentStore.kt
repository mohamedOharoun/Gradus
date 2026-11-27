package es.ulpgc.pamn.dominio.course.publication

// Puerto KMP (Strategy): abstrae la lectura y borrado de los bytes del JSON local de un borrador
// sin acoplar `sharedLogic` a `java.io.File` ni `android.net.Uri`.
interface CourseDraftContentStore {

    // Lee los bytes del fichero local o devuelve null si falta o no se puede leer.
    suspend fun read(filePath: String): ByteArray?

    // Borra el fichero local si existe (idempotente).
    suspend fun delete(filePath: String)
}
