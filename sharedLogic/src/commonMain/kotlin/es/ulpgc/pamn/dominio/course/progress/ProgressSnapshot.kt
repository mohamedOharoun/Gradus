package es.ulpgc.pamn.dominio.course.progress

// Lo que se envía al servidor o se recibe de él en una sincronización, con la forma del contrato
// (backend/docs/api-progreso.md): inscripciones con lo dominado dentro. Las listas no tienen un
// orden garantizado: nadie debe depender de él.
data class ProgressSnapshot(val enrollments: List<EnrollmentSnapshot>) {

    // Sin inscripciones: en lo pendiente, no hay nada que subir. ProgressSync llama al servidor igual
    // (B-10: así trae lo del servidor); sirve a quien quiera decidir si merece la pena sincronizar (D13).
    val isEmpty: Boolean get() = enrollments.isEmpty()
}

// Una inscripción y sus módulos dominados (en lo pendiente, solo los que faltan por subir).
data class EnrollmentSnapshot(val enrollment: Enrollment, val mastered: List<MasteredModule>)
