package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.course.CourseSummary
import kotlin.time.Instant

// Filtro por estado visible en el desplegable de «Mis cursos».
enum class AuthorCourseFilter {
    ALL,
    PUBLISHED,
    ACCEPTED,
    PENDING,
    REJECTED,
}

// Una tarjeta en «Mis cursos»: puede representar un curso publicado sin borrador, un borrador
// sin curso publicado asociado, o la fusión de ambos (una sola tarjeta por curso).
data class AuthorCourseItem(
    val published: CourseSummary? = null,
    val draft: CourseDraft? = null,
) {
    init {
        require(published != null || draft != null) {
            "AuthorCourseItem requires at least a published course or a draft"
        }
        if (published != null && draft != null) {
            require(draft.targetCourseId == published.id) {
                "Merged draft targetCourseId (${draft.targetCourseId}) must match published id (${published.id})"
            }
        }
    }

    val id: String get() = published?.id ?: draft!!.id

    val publishedVersion: Int? get() = published?.version

    val activityAt: Instant
        get() = draft?.updatedAt ?: published!!.publishedAtInstant()

    val displayTitle: String
        get() = draft?.title?.takeIf { it.isNotBlank() } ?: published?.title.orEmpty()

    val displayModuleCount: Int?
        get() = draft?.moduleCount ?: published?.moduleCount

    val statusFilter: AuthorCourseFilter
        get() = when (draft?.status) {
            null -> AuthorCourseFilter.PUBLISHED
            is DraftStatus.Accepted -> AuthorCourseFilter.ACCEPTED
            is DraftStatus.Pending -> AuthorCourseFilter.PENDING
            is DraftStatus.Rejected -> AuthorCourseFilter.REJECTED
        }

    fun matches(filter: AuthorCourseFilter): Boolean =
        filter == AuthorCourseFilter.ALL || statusFilter == filter
}

// Combina cursos publicados y borradores en una lista ordenada de más reciente a más antigua
// (desempate determinista por id ascendente). Si llegan varios borradores para un mismo curso
// publicado, asocia el más reciente y conserva los adicionales sin perder datos.
fun mergeAuthorCourses(
    published: List<CourseSummary>,
    drafts: List<CourseDraft>,
): List<AuthorCourseItem> {
    val publishedIds = published.mapTo(HashSet()) { it.id }
    val (matchingDrafts, standaloneDrafts) = drafts.partition { draft ->
        draft.targetCourseId != null && draft.targetCourseId in publishedIds
    }
    val sortedByTarget = matchingDrafts
        .groupBy { it.targetCourseId!! }
        .mapValues { (_, group) ->
            group.sortedWith(compareByDescending<CourseDraft> { it.updatedAt }.thenBy { it.id })
        }

    val mergedPublished = published.map { course ->
        AuthorCourseItem(published = course, draft = sortedByTarget[course.id]?.firstOrNull())
    }
    val extraDrafts = sortedByTarget.values.flatMap { it.drop(1) }
    val mergedDrafts = (standaloneDrafts + extraDrafts).map { draft ->
        AuthorCourseItem(published = null, draft = draft)
    }

    return (mergedPublished + mergedDrafts).sortedWith(
        compareByDescending<AuthorCourseItem> { it.activityAt }
            .thenBy { it.id }
            .thenBy { it.draft?.id.orEmpty() },
    )
}

fun List<AuthorCourseItem>.filterBy(filter: AuthorCourseFilter): List<AuthorCourseItem> =
    if (filter == AuthorCourseFilter.ALL) this else filter { it.matches(filter) }

// Incluye todos los filtros del enum aunque su contador sea 0.
fun List<AuthorCourseItem>.countByFilter(): Map<AuthorCourseFilter, Int> {
    val countsByStatus = groupingBy { it.statusFilter }.eachCount()
    return AuthorCourseFilter.entries.associateWith { filter ->
        if (filter == AuthorCourseFilter.ALL) size else countsByStatus[filter] ?: 0
    }
}

private fun CourseSummary.publishedAtInstant(): Instant =
    Instant.parseOrNull(publishedAt) ?: Instant.DISTANT_PAST
