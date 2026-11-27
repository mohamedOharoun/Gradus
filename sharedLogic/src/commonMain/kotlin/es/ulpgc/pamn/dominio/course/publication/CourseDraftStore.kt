package es.ulpgc.pamn.dominio.course.publication

import kotlinx.coroutines.flow.Flow

// Puerto (arquitectura hexagonal / Strategy): almacenamiento local de borradores de cursos por
// usuario. Permite intercambiar InMemoryCourseDraftStore (tests/previews) y RoomCourseDraftStore
// (SQLite en Android) sin acoplar la lógica compartida a Room.
interface CourseDraftStore {

    // Borradores del usuario de la sesión actual. La sesión se lee al empezar a escuchar (igual
    // que ProgressStore); sin sesión emite una lista vacía y termina.
    fun drafts(): Flow<List<CourseDraft>>

    // Busca un borrador por identificador, o null si no existe o ya fue descartado.
    suspend fun find(draftId: String): CourseDraft?

    // Inserta un borrador nuevo o sustituye el existente con el mismo id.
    suspend fun upsert(draft: CourseDraft)

    // Actualiza atómicamente solo si `draft.id` sigue presente y devuelve true. Si fue descartado
    // en vuelo, no inserta nada y devuelve false (evita resucitar borradores descartados).
    suspend fun updateIfPresent(draft: CourseDraft): Boolean

    // Elimina el borrador si existe (idempotente).
    suspend fun delete(draftId: String)

    // Elimina atómicamente todos los borradores de `userId` y devuelve la lista eliminada (para
    // limpiar ficheros y trabajos pendientes sin carreras al borrar la cuenta).
    suspend fun clearForUser(userId: String): List<CourseDraft>
}
