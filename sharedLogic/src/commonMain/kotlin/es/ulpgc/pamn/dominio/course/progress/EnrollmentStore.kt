package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.Flow

// Puerto (patrón Strategy): dónde guarda la app en qué cursos está inscrito el estudiante.
// Hoy lo implementa InMemoryProgressStore (junto con ProgressStore); la parte C, con Room, tendrá
// que cumplir también lo que dicen estos comentarios.
interface EnrollmentStore {

    // Si el estudiante de la sesión actual está inscrito en el curso. Es un Flow: quien lo escucha
    // recibe el valor actual y, después, cada cambio. La sesión se lee al empezar a escuchar; sin
    // sesión emite false y termina.
    fun enrolled(courseId: String): Flow<Boolean>

    // Inscribe al estudiante de la sesión actual en el curso.
    // - Si ya estaba inscrito, no da error y se conserva la primera fecha de inscripción.
    // - Sin sesión lanza IllegalStateException (es un fallo de programación: la interfaz no debe
    //   ofrecer inscribirse sin haber iniciado sesión).
    suspend fun enroll(courseId: String)

    // Da de baja localmente al usuario indicado del curso.
    // - Recibe userId explícito y no lee SessionStore: así, si la sesión cambia o se cierra
    //   mientras la petición viaja por la red, borra solo el progreso del usuario original y no
    //   lanza por falta de sesión.
    // - Es idempotente: si el usuario o la inscripción no existen, no hace nada ni da error.
    // - Borra inscripción, módulos dominados y marcas pendientes de (userId, courseId) sin tocar
    //   otros cursos ni otros usuarios, y avisa a los Flow activos.
    suspend fun unenroll(userId: String, courseId: String)
}
