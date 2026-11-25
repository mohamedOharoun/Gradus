package es.ulpgc.pamn.dominio.data.submission

// Las fotos de las entregas (docs/tareas/ejercicio-foto.md, decisiones K y X). Puerto pequeño, como
// PhotoFiles en las notas: el ViewModel se prueba en el ordenador con uno falso, sin ficheros de verdad.
// Son temporales: no van a «Mis notas» y se borran al entregarlas, cambiarlas o salir del paso.
interface SubmissionPhotos {

    // Prepara la foto de `source` (una URI como texto: de la galería o el fichero de la cámara) para
    // entregarla: derecha según su EXIF, con el lado mayor de 2000 px como mucho y en JPEG. Devuelve la
    // ruta del fichero preparado, o null si no se pudo leer o no es una imagen. No toca `source`.
    suspend fun prepare(source: String): String?

    // Como prepare, con la foto que hizo la cámara (un fichero temporal de la carpeta de la cámara), que
    // después se tira: ya no hace falta. null si no se pudo leer.
    suspend fun prepareCapture(capture: String): String?

    // Tira una foto de la cámara que no se va a usar. Fuera de la carpeta de la cámara, no hace nada.
    suspend fun discardCapture(capture: String)

    // Los bytes de una foto preparada, para enviarla; null si ya no existe.
    suspend fun read(path: String): ByteArray?

    // Borra una foto preparada. Fuera de su carpeta, o si no existe, no hace nada.
    suspend fun delete(path: String)
}
