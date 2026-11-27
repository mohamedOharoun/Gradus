package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Adaptador en memoria de CourseDraftStore para tests y previews.
// Usa MutableStateFlow con operaciones atómicas (update / getAndUpdate) y aísla los borradores
// por cuenta, capturando la sesión al empezar a escuchar igual que InMemoryProgressStore.
class InMemoryCourseDraftStore(
    private val sessions: SessionStore,
) : CourseDraftStore {

    private val draftsById = MutableStateFlow<Map<String, CourseDraft>>(emptyMap())

    override fun drafts(): Flow<List<CourseDraft>> = flow {
        val userId = sessions.current()?.account?.id
        if (userId == null) {
            emit(emptyList())
        } else {
            emitAll(
                draftsById
                    .map { all -> all.values.filter { it.userId == userId } }
                    .distinctUntilChanged(),
            )
        }
    }

    override suspend fun find(draftId: String): CourseDraft? = draftsById.value[draftId]

    override suspend fun upsert(draft: CourseDraft) {
        draftsById.update { current -> current + (draft.id to draft) }
    }

    override suspend fun updateIfPresent(draft: CourseDraft): Boolean {
        val previous = draftsById.getAndUpdate { current ->
            if (draft.id in current) current + (draft.id to draft) else current
        }
        return draft.id in previous
    }

    override suspend fun delete(draftId: String) {
        draftsById.update { current -> current - draftId }
    }

    override suspend fun clearForUser(userId: String): List<CourseDraft> {
        val previous = draftsById.getAndUpdate { current ->
            current.filterValues { it.userId != userId }
        }
        return previous.values.filter { it.userId == userId }
    }
}
