package es.ulpgc.pamn.dominio.data.progress

import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressSyncStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

// ProgressStore, EnrollmentStore y ProgressSyncStore guardados en el móvil con Room (Adapter de los
// puertos). Se comporta igual que InMemoryProgressStore: por usuario (session.account.id), primera
// fecha, pending solo al insertar, sin sesión lee vacío y escribir lanza.
// enroll y markMastered van sin transacción: no hay baja (decisión 7), así que una inscripción
// comprobada no desaparece antes del INSERT, y dos INSERT OR IGNORE de la misma fila dejan una sola.
// pending y settle sí van cada uno en su transacción (en ProgressDao).
class RoomProgressStore(
    private val dao: ProgressDao,
    private val sessions: SessionStore,
    private val clock: Clock = Clock.System,
) : ProgressStore, EnrollmentStore, ProgressSyncStore {

    override fun mastered(courseId: String): Flow<Set<String>> =
        watch(whenNoSession = emptySet()) { userId -> dao.watchMasteredIds(userId, courseId).map { it.toSet() } }

    override fun enrolled(courseId: String): Flow<Boolean> =
        watch(whenNoSession = false) { userId -> dao.watchEnrolled(userId, courseId) }

    // Si ya estaba inscrito, INSERT OR IGNORE no toca la fila: se conservan la fecha y pending.
    override suspend fun enroll(courseId: String) {
        val userId = currentUserId()
        dao.insertEnrollmentIfAbsent(EnrollmentEntity(userId, courseId, clock.now(), pending = true))
    }

    // La inscripción se comprueba antes de escribir: así lanza la IllegalStateException del puerto
    // (con el curso, nunca el token) y no la de la clave foránea, que queda como segunda capa.
    override suspend fun markMastered(courseId: String, moduleId: String) {
        val userId = currentUserId()
        checkNotNull(dao.enrollment(userId, courseId)) { "Not enrolled in course $courseId: enroll before mastering it" }
        dao.insertMasteredModuleIfAbsent(MasteredModuleEntity(userId, courseId, moduleId, clock.now(), pending = true))
    }

    // El usuario va por parámetro (decisión 11): estos dos no leen la sesión.
    override suspend fun pending(userId: String): ProgressSnapshot = dao.pendingRows(userId).toSnapshot()

    override suspend fun settle(userId: String, sent: ProgressSnapshot, remote: ProgressSnapshot) =
        dao.settle(sent = sent.toRows(userId), remote = remote.toRows(userId))

    // La sesión se lee al empezar a escuchar (el bloque de flow se ejecuta entonces): quien ya escucha
    // sigue con su usuario aunque luego entre otro. Sin sesión emite el valor vacío y termina.
    // Room vuelve a consultar con cualquier cambio en la tabla; distinctUntilChanged evita avisar
    // cuando cambia otro curso u otro usuario.
    private fun <T> watch(whenNoSession: T, query: (userId: String) -> Flow<T>): Flow<T> = flow {
        val userId = sessions.current()?.account?.id
        if (userId == null) emit(whenNoSession) else emitAll(query(userId).distinctUntilChanged())
    }

    private suspend fun currentUserId(): String =
        checkNotNull(sessions.current()) { "There is no session: log in first" }.account.id
}
