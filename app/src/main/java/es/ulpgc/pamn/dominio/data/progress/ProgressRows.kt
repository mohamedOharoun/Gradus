package es.ulpgc.pamn.dominio.data.progress

import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot

// Filas de las dos tablas del progreso, tal cual (patrón Mapper: las funciones de abajo traducen entre
// ellas y ProgressSnapshot, la forma del contrato). Las usan pending y settle de ProgressDao.
data class ProgressRows(val enrollments: List<EnrollmentEntity>, val modules: List<MasteredModuleEntity>)

// Del snapshot a filas del usuario dado, sin pendiente: así entra lo que llega del servidor. En `sent`
// solo se usan las claves (usuario, curso, módulo); sus fechas y su pending no se escriben nunca.
fun ProgressSnapshot.toRows(userId: String): ProgressRows = ProgressRows(
    enrollments = enrollments.map { EnrollmentEntity(userId, it.enrollment.courseId, it.enrollment.enrolledAt, pending = false) },
    modules = enrollments.flatMap { course ->
        course.mastered.map { MasteredModuleEntity(userId, course.enrollment.courseId, it.moduleId, it.masteredAt, pending = false) }
    },
)

// De filas al snapshot: cada inscripción con los módulos de su mismo usuario y curso. Un módulo sin su
// inscripción en la lista se queda fuera (el contrato no admite módulos sueltos).
fun ProgressRows.toSnapshot(): ProgressSnapshot {
    val modulesByCourse = modules.groupBy { it.userId to it.courseId }
    return ProgressSnapshot(
        enrollments.map { enrollment ->
            EnrollmentSnapshot(
                Enrollment(enrollment.courseId, enrollment.enrolledAt),
                modulesByCourse[enrollment.userId to enrollment.courseId].orEmpty()
                    .map { MasteredModule(it.moduleId, it.masteredAt) },
            )
        },
    )
}
