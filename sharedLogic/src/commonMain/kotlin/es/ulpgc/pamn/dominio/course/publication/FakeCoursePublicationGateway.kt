package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.CourseSummary

// Registro inmutable de una llamada de validación o publicación sobre una nueva versión.
class VersionUploadCall(
    val session: Session,
    val courseId: String,
    val courseJson: ByteArray,
)

// Gateway falso en memoria, sin red: para tests y previews. Sus lambdas son `suspend` para poder
// pausar peticiones en vuelo (p. ej. con CompletableDeferred) al probar cancelaciones o descartes.
class FakeCoursePublicationGateway(
    private val myCoursesAnswer: suspend (Session) -> MyCoursesOutcome = { MyCoursesOutcome.Found(emptyList()) },
    private val validateNewAnswer: suspend (Session, ByteArray) -> ValidationOutcome = { _, _ ->
        ValidationOutcome.Validated(CourseReport.EMPTY)
    },
    private val validateVersionAnswer: suspend (Session, String, ByteArray) -> ValidationOutcome = { _, _, _ ->
        ValidationOutcome.Validated(CourseReport.EMPTY)
    },
    private val publishNewAnswer: suspend (Session, ByteArray) -> PublishOutcome = defaultPublishNew(),
    private val publishVersionAnswer: suspend (Session, String, ByteArray) -> PublishOutcome = defaultPublishVersion(),
) : CoursePublicationGateway {

    val myCoursesCalls: List<Session> get() = recordedMyCourses.toList()
    val validateNewCalls: List<Pair<Session, ByteArray>> get() = recordedValidateNew.toList()
    val validateVersionCalls: List<VersionUploadCall> get() = recordedValidateVersion.toList()
    val publishNewCalls: List<Pair<Session, ByteArray>> get() = recordedPublishNew.toList()
    val publishVersionCalls: List<VersionUploadCall> get() = recordedPublishVersion.toList()

    private val recordedMyCourses = mutableListOf<Session>()
    private val recordedValidateNew = mutableListOf<Pair<Session, ByteArray>>()
    private val recordedValidateVersion = mutableListOf<VersionUploadCall>()
    private val recordedPublishNew = mutableListOf<Pair<Session, ByteArray>>()
    private val recordedPublishVersion = mutableListOf<VersionUploadCall>()

    override suspend fun myCourses(session: Session): MyCoursesOutcome {
        recordedMyCourses += session
        return myCoursesAnswer(session)
    }

    override suspend fun validateNew(session: Session, courseJson: ByteArray): ValidationOutcome {
        val copy = courseJson.copyOf()
        recordedValidateNew += session to copy
        return validateNewAnswer(session, copy)
    }

    override suspend fun validateVersion(
        session: Session,
        courseId: String,
        courseJson: ByteArray,
    ): ValidationOutcome {
        val copy = courseJson.copyOf()
        recordedValidateVersion += VersionUploadCall(session, courseId, copy)
        return validateVersionAnswer(session, courseId, copy)
    }

    override suspend fun publishNew(session: Session, courseJson: ByteArray): PublishOutcome {
        val copy = courseJson.copyOf()
        recordedPublishNew += session to copy
        return publishNewAnswer(session, copy)
    }

    override suspend fun publishVersion(
        session: Session,
        courseId: String,
        courseJson: ByteArray,
    ): PublishOutcome {
        val copy = courseJson.copyOf()
        recordedPublishVersion += VersionUploadCall(session, courseId, copy)
        return publishVersionAnswer(session, courseId, copy)
    }

    private companion object {
        fun defaultPublishNew(): suspend (Session, ByteArray) -> PublishOutcome {
            var count = 0
            return { session, _ ->
                count++
                PublishOutcome.Published(
                    course = fakeSummary(id = "course-$count", version = 1, session = session),
                    warnings = emptyList(),
                )
            }
        }

        fun defaultPublishVersion(): suspend (Session, String, ByteArray) -> PublishOutcome {
            val versions = mutableMapOf<String, Int>()
            return { session, courseId, _ ->
                val nextVersion = (versions[courseId] ?: 1) + 1
                versions[courseId] = nextVersion
                PublishOutcome.Published(
                    course = fakeSummary(id = courseId, version = nextVersion, session = session),
                    warnings = emptyList(),
                )
            }
        }

        fun fakeSummary(id: String, version: Int, session: Session) = CourseSummary(
            id = id,
            version = version,
            author = Author(id = session.account.id, name = session.account.name),
            publishedAt = "2026-11-27T10:00:00Z",
            title = "Curso de prueba",
            description = "",
            language = "es",
            tags = emptyList(),
            moduleCount = 1,
        )
    }
}
