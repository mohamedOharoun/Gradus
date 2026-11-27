package es.ulpgc.pamn.dominio.data.publication

import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.publication.CourseDraft
import es.ulpgc.pamn.dominio.course.publication.CourseDraftStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

// Patrón Adapter: adapta `CourseDraftDao` y `SessionStore` al puerto KMP `CourseDraftStore`.
// Se comporta igual que `InMemoryCourseDraftStore` y `RoomProgressStore`: la sesión se lee al
// empezar a escuchar `drafts()`, y sin sesión emite lista vacía y termina.
class RoomCourseDraftStore(
    private val dao: CourseDraftDao,
    private val sessions: SessionStore,
) : CourseDraftStore {

    override fun drafts(): Flow<List<CourseDraft>> = flow {
        val userId = sessions.current()?.account?.id
        if (userId == null) {
            emit(emptyList())
        } else {
            emitAll(
                dao.watchByUser(userId)
                    .map { rows -> rows.map(CourseDraftEntity::toDomain) }
                    .distinctUntilChanged(),
            )
        }
    }

    override suspend fun find(draftId: String): CourseDraft? =
        dao.find(draftId)?.toDomain()

    override suspend fun upsert(draft: CourseDraft) {
        dao.upsert(draft.toEntity())
    }

    override suspend fun updateIfPresent(draft: CourseDraft): Boolean =
        dao.updateIfPresent(draft.toEntity()) > 0

    override suspend fun delete(draftId: String) {
        dao.delete(draftId)
    }

    override suspend fun clearForUser(userId: String): List<CourseDraft> =
        dao.clearForUser(userId).map(CourseDraftEntity::toDomain)
}
