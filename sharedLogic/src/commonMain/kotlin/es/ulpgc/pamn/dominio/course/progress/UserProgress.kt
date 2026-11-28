package es.ulpgc.pamn.dominio.course.progress

import kotlin.time.Instant

// El progreso de un curso: la inscripción y, dentro de ella, lo dominado (como en el contrato con
// el servidor). Al estar anidado, no puede existir un módulo dominado sin inscripción: lo impone
// la propia estructura.
// Lo pendiente de subir (estado interno, no va en Enrollment ni en MasteredModule):
// - enrollmentPending: la inscripción falta por subir.
// - pendingModules: ids de los módulos que faltan por subir. Invariante: pendingModules ⊆ mastered.keys.
// Por defecto nada está pendiente (así entra lo que llega del servidor); lo que se hace en el
// móvil (enrolledIn, masteredIn) lo marca.
internal data class CourseProgress(
    val enrollment: Enrollment,
    val mastered: Map<String, MasteredModule> = emptyMap(),
    val enrollmentPending: Boolean = false,
    val pendingModules: Set<String> = emptySet(),
)

// Todo el progreso de un usuario: courseId → su progreso en ese curso.
// Valor inmutable: cada función devuelve una copia nueva (o this si no hay nada que cambiar),
// así que se puede usar dentro de MutableStateFlow.update sin efectos secundarios.
internal data class UserProgress(val courses: Map<String, CourseProgress> = emptyMap()) {

    // Inscribe en el curso y deja la inscripción pendiente de subir.
    // Si ya estaba, devuelve this: se conserva la primera fecha.
    fun enrolledIn(courseId: String, at: Instant): UserProgress =
        if (courseId in courses) this
        else UserProgress(courses + (courseId to CourseProgress(Enrollment(courseId, at), enrollmentPending = true)))

    // Apunta el módulo como dominado y pendiente de subir. Si el curso no está inscrito, lanza IllegalStateException.
    // Si el módulo ya estaba dominado, devuelve this: se conserva la primera fecha.
    fun masteredIn(courseId: String, moduleId: String, at: Instant): UserProgress {
        val course = checkNotNull(courses[courseId]) {
            "Cannot master a module of $courseId: the student is not enrolled in that course"
        }
        if (moduleId in course.mastered) return this
        val updated = course.copy(
            mastered = course.mastered + (moduleId to MasteredModule(moduleId, at)),
            pendingModules = course.pendingModules + moduleId,
        )
        return UserProgress(courses + (courseId to updated))
    }

    // Quita el curso completo (inscripción, módulos dominados y marcas pendientes) sin mutar este valor.
    // Si no estaba inscrito, devuelve this.
    fun withoutCourse(courseId: String): UserProgress =
        if (courseId !in courses) this else UserProgress(courses - courseId)

    fun isEnrolledIn(courseId: String): Boolean = courseId in courses

    fun masteredIds(courseId: String): Set<String> = courses[courseId]?.mastered?.keys.orEmpty()
}
