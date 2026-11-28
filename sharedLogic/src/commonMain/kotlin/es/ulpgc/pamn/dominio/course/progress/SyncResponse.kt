package es.ulpgc.pamn.dominio.course.progress

// Un 200 de POST /api/progress/sync: todo el progreso del usuario en el servidor y lo que no se
// guardó de lo enviado. Las listas no tienen un orden garantizado.
data class SyncResponse(val remote: ProgressSnapshot, val skipped: List<SkippedProgress>)
