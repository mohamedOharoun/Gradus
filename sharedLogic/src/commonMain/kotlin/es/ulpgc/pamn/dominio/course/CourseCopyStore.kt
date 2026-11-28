package es.ulpgc.pamn.dominio.course

// Puerto (arquitectura hexagonal): almacén local de copias de cursos descargados, guardados como
// JSON con la forma de GET /api/courses/{id}. Lo implementan RoomCourseCopyStore (en app) e
// InMemoryCourseCopyStore (en tests).
interface CourseCopyStore {

    // Guarda la copia de `courseId`, sobrescribiendo la anterior si ya había una.
    suspend fun save(courseId: String, json: String)

    // Devuelve el JSON guardado de `courseId`, o null si no hay copia.
    suspend fun load(courseId: String): String?
}
