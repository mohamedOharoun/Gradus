package es.ulpgc.pamn.dominio.course.progress

// Puerto (patrón Strategy): con lo que la sincronización habla con el almacén del progreso.
// El usuario va por parámetro (lo pone quien sincroniza, que ya tiene la sesión).
// Hoy lo implementa InMemoryProgressStore; la versión Room (parte C) tiene que cumplir lo mismo.
interface ProgressSyncStore {

    // Lo que falta por subir del usuario: cada inscripción pendiente y cada módulo pendiente.
    // - Un módulo pendiente va dentro de su inscripción aunque esa inscripción ya estuviera subida
    //   (el contrato no admite módulos sueltos; reenviarla es inofensivo: el servidor fusiona).
    // - En cada inscripción solo van los módulos pendientes, no todos los dominados.
    // - Un usuario sin nada guardado no tiene nada pendiente (snapshot vacío).
    suspend fun pending(userId: String): ProgressSnapshot

    // Tras un 200: sent es lo que se envió y remote, todo el progreso del usuario en el servidor.
    // Room debe hacerlo en UNA sola transacción, con estas dos partes:
    // 1. Todo lo de sent sale de la cola, por clave (courseId; courseId + moduleId), nunca por fecha
    //    (el servidor redondea a microsegundos). También lo que el servidor omitió (skipped): sale de
    //    la cola y se conserva en el móvil. Lo que sent tenga y el móvil no, se ignora.
    // 2. remote se fusiona: unión; en lo que ya estaba, gana la fecha más antigua y su marca de
    //    pendiente no cambia; lo nuevo no queda pendiente. Nunca se borra nada local.
    // Lo inscrito o dominado mientras la petición viajaba no está en sent: sigue pendiente.
    // Aplicarlo dos veces con lo mismo deja el mismo estado. Quien escucha mastered o enrolled
    // se entera de lo que llega del servidor.
    suspend fun settle(userId: String, sent: ProgressSnapshot, remote: ProgressSnapshot)
}
