package es.ulpgc.pamn.dominio.course.progress

import kotlin.time.Instant

// Un estudiante inscrito en un curso, y desde cuándo.
data class Enrollment(val courseId: String, val enrolledAt: Instant)
