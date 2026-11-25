package es.ulpgc.pamn.dominio.course.progress

import kotlin.time.Instant

// El progreso de un curso: la inscripción y, dentro de ella, lo dominado (como en el contrato con
// el servidor). Al estar anidado, no puede existir un módulo dominado sin inscripción: lo impone
// la propia estructura.
internal data class CourseProgress(
    val enrollment: Enrollment,
    val mastered: Map<String, MasteredModule> = emptyMap(),
)

// Todo el progreso de un usuario: courseId → su progreso en ese curso.
// Valor inmutable: cada función devuelve una copia nueva (o this si no hay nada que cambiar),
// así que se puede usar dentro de MutableStateFlow.update sin efectos secundarios.
internal data class UserProgress(val courses: Map<String, CourseProgress> = emptyMap()) {

    // Inscribe en el curso. Si ya estaba, devuelve this: se conserva la primera fecha.
    fun enrolledIn(courseId: String, at: Instant): UserProgress =
        if (courseId in courses) this
        else UserProgress(courses + (courseId to CourseProgress(Enrollment(courseId, at))))

    // Apunta el módulo como dominado. Si el curso no está inscrito, lanza IllegalStateException.
    // Si el módulo ya estaba dominado, devuelve this: se conserva la primera fecha.
    fun masteredIn(courseId: String, moduleId: String, at: Instant): UserProgress {
        val course = checkNotNull(courses[courseId]) {
            "Cannot master a module of $courseId: the student is not enrolled in that course"
        }
        if (moduleId in course.mastered) return this
        val updated = course.copy(mastered = course.mastered + (moduleId to MasteredModule(moduleId, at)))
        return UserProgress(courses + (courseId to updated))
    }

    fun isEnrolledIn(courseId: String): Boolean = courseId in courses

    fun masteredIds(courseId: String): Set<String> = courses[courseId]?.mastered?.keys.orEmpty()
}
