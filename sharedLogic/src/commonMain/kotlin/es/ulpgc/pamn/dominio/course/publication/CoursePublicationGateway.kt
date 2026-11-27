package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.CourseSummary

// Puerto (arquitectura hexagonal): operaciones de «Mis cursos» contra el servidor (listar cursos
// propios, validar y publicar cursos nuevos o nuevas versiones). Nunca lanza excepciones: cualquier
// fallo se representa en el resultado sellado correspondiente.
interface CoursePublicationGateway {

    suspend fun myCourses(session: Session): MyCoursesOutcome

    suspend fun validateNew(session: Session, courseJson: ByteArray): ValidationOutcome

    suspend fun validateVersion(session: Session, courseId: String, courseJson: ByteArray): ValidationOutcome

    suspend fun publishNew(session: Session, courseJson: ByteArray): PublishOutcome

    suspend fun publishVersion(session: Session, courseId: String, courseJson: ByteArray): PublishOutcome
}

sealed interface MyCoursesOutcome {
    data class Found(val courses: List<CourseSummary>) : MyCoursesOutcome
    data object SessionExpired : MyCoursesOutcome
    data object Unreachable : MyCoursesOutcome
    data object ServerFailure : MyCoursesOutcome
}

sealed interface ValidationOutcome {
    data class Validated(val report: CourseReport) : ValidationOutcome
    data object TooLarge : ValidationOutcome
    data object IdenticalToLatest : ValidationOutcome
    data object NotAuthor : ValidationOutcome
    data object NotFound : ValidationOutcome
    data object SessionExpired : ValidationOutcome
    data object Unreachable : ValidationOutcome
    data object ServerFailure : ValidationOutcome
}

sealed interface PublishOutcome {
    data class Published(
        val course: CourseSummary,
        val warnings: List<CourseIssue>,
    ) : PublishOutcome

    data class Invalid(val report: CourseReport) : PublishOutcome {
        init {
            require(!report.isValid) { "Invalid publish outcome requires at least one error" }
        }
    }

    data object TooLarge : PublishOutcome
    data object IdenticalToLatest : PublishOutcome
    data object NotAuthor : PublishOutcome
    data object NotFound : PublishOutcome
    data object SessionExpired : PublishOutcome
    data object Unreachable : PublishOutcome
    data object ServerFailure : PublishOutcome
}
