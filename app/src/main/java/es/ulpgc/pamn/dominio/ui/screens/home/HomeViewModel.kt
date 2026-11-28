package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.account.initials
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.EnrollmentsReader
import es.ulpgc.pamn.dominio.home.EnrolledCourse
import es.ulpgc.pamn.dominio.home.HomeSummary
import es.ulpgc.pamn.dominio.home.greetingName
import es.ulpgc.pamn.dominio.home.homeSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Lo que pinta Inicio (docs/tareas/inicio.md). `name`: el primer nombre, o null (solo «Hola»).
// `unavailable`: cursos inscritos que no se han podido cargar (sin red y sin copia); no salen en la lista.
data class HomeUiState(
    val name: String?,
    val initials: String,
    val summary: HomeSummary,
    val unavailable: Int = 0,
) {
    // «Todavía no estás en ningún curso»: solo si no hay ninguno. Con cursos que no se han podido cargar
    // (sin red), no: se avisa de eso (unavailableText).
    val isEmpty: Boolean get() = summary.courses.isEmpty() && unavailable == 0
}

sealed interface HomeScreenState {
    data object Loading : HomeScreenState
    data class Ready(val ui: HomeUiState) : HomeScreenState
}

// Junta la sesión (saludo), las inscripciones (EnrollmentsReader) y cada curso (CourseGateway: la red o la
// copia guardada), y deja el cálculo a homeSummary (sharedLogic). Escucha las inscripciones: al dominar un
// módulo o inscribirse, Inicio se actualiza solo. Recibe los puertos por constructor.
class HomeViewModel(
    private val sessions: SessionStore,
    private val enrollments: EnrollmentsReader,
    private val gateway: CourseGateway,
) : ViewModel() {

    companion object {
        fun factory(sessions: SessionStore, enrollments: EnrollmentsReader, gateway: CourseGateway) =
            viewModelFactory { initializer { HomeViewModel(sessions, enrollments, gateway) } }
    }

    var state by mutableStateOf<HomeScreenState>(HomeScreenState.Loading)
        private set

    // Los cursos ya descargados: un cambio de progreso no los vuelve a pedir. Los que fallaron no se
    // guardan, así que se piden otra vez en el siguiente cambio (puede que ya haya red).
    private val courses = mutableMapOf<String, Course>()

    // El saludo (primer nombre e iniciales). Aparte del resto: onResume lo relee y cada actualización de los
    // cursos usa el último, así un cambio de nombre no se pierde con el siguiente cambio de progreso.
    private var header = Header(name = null, initials = "?")

    init {
        viewModelScope.launch {
            header = readHeader()
            // collectLatest: si llega otro cambio mientras se descargan cursos, se deja esa descarga y se
            // empieza con lo nuevo (lo ya descargado se conserva en `courses`).
            enrollments.enrollments().collectLatest { list ->
                downloadMissing(list.map { it.enrollment.courseId })
                state = HomeScreenState.Ready(
                    HomeUiState(header.name, header.initials, homeSummary(list.known()), list.count { it.unknown }),
                )
            }
        }
    }

    // Al volver a Inicio (p. ej. de «Editar perfil», que guarda la sesión con el nombre nuevo) se relee la
    // sesión, como en Ajustes. Solo cambia el saludo: los cursos ni se tocan ni se vuelven a descargar.
    fun onResume() {
        viewModelScope.launch {
            header = readHeader()
            (state as? HomeScreenState.Ready)?.let { ready ->
                state = HomeScreenState.Ready(ready.ui.copy(name = header.name, initials = header.initials))
            }
        }
    }

    private suspend fun readHeader(): Header {
        val account = sessions.current()?.account
        return Header(name = account?.let { greetingName(it.name) }, initials = account?.initials ?: "?")
    }

    private data class Header(val name: String?, val initials: String)

    // Los que faltan, a la vez (async): Inicio espera al más lento, no a la suma de todos.
    private suspend fun downloadMissing(ids: List<String>) = coroutineScope {
        ids.filterNot { it in courses }
            .map { id -> async { id to gateway.course(id) } }
            .awaitAll()
            .forEach { (id, outcome) -> if (outcome is CourseOutcome.Found) courses[id] = outcome.course }
    }

    private fun List<EnrollmentSnapshot>.known(): List<EnrolledCourse> = mapNotNull { snapshot ->
        courses[snapshot.enrollment.courseId]?.let { EnrolledCourse(it, snapshot.enrollment, snapshot.mastered) }
    }

    private val EnrollmentSnapshot.unknown: Boolean get() = enrollment.courseId !in courses
}
