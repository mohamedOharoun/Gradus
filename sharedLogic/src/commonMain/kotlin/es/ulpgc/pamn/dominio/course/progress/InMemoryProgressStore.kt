package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Instant

// ProgressStore y EnrollmentStore en memoria: el progreso dura mientras la app está abierta
// (como InMemorySessionStore). Un solo objeto cumple los dos puertos.
// Un solo MutableStateFlow con el progreso de cada usuario: id de la cuenta de la sesión →
// UserProgress. La sesión y el reloj se inyectan (el reloj, para fijar las fechas en los tests).
class InMemoryProgressStore(
    private val sessions: SessionStore,
    private val clock: Clock = Clock.System,
) : ProgressStore, EnrollmentStore {

    private val progress = MutableStateFlow<Map<String, UserProgress>>(emptyMap())

    override fun mastered(courseId: String): Flow<Set<String>> =
        watch(whenNoSession = emptySet()) { it.masteredIds(courseId) }

    override fun enrolled(courseId: String): Flow<Boolean> =
        watch(whenNoSession = false) { it.isEnrolledIn(courseId) }

    override suspend fun enroll(courseId: String) {
        change { user, now -> user.enrolledIn(courseId, now) }
    }

    override suspend fun markMastered(courseId: String, moduleId: String) {
        change { user, now -> user.masteredIn(courseId, moduleId, now) }
    }

    // Solo para los tests: deja ver las fechas, que los puertos no exponen. Solo lectura.
    // En la tarea 4c se decidirá si lo sustituye el puerto de pendientes.
    internal fun progressOf(userId: String): UserProgress = progress.value[userId] ?: UserProgress()

    // La sesión se lee al empezar a escuchar (el bloque de flow se ejecuta entonces): quien ya
    // escucha sigue con su usuario aunque luego entre otro. Sin sesión emite el valor vacío y termina.
    // distinctUntilChanged evita avisar cuando cambia otro curso.
    private fun <T> watch(whenNoSession: T, select: (UserProgress) -> T): Flow<T> = flow {
        val userId = sessions.current()?.account?.id
        if (userId == null) emit(whenNoSession)
        else emitAll(progress.map { select(it[userId] ?: UserProgress()) }.distinctUntilChanged())
    }

    // current() y clock.now() se leen antes de update: current() es suspend y la lambda de update
    // puede repetirse si dos escrituras coinciden, así que debe ser pura. Si la lambda lanza
    // (p. ej. dominar sin inscripción), update no cambia el valor: no queda nada escrito.
    private suspend fun change(transform: (UserProgress, now: Instant) -> UserProgress) {
        val userId = checkNotNull(sessions.current()) { "There is no session: log in first" }.account.id
        val now = clock.now()
        progress.update { all -> all + (userId to transform(all[userId] ?: UserProgress(), now)) }
    }
}
