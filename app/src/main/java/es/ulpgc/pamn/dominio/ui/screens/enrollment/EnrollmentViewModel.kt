package es.ulpgc.pamn.dominio.ui.screens.enrollment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.data.progress.ProgressSyncRequester
import es.ulpgc.pamn.dominio.ui.components.EnrollmentState
import kotlinx.coroutines.launch

// Lógica del botón de inscribirse: escucha enrolled(courseId) y expone un solo estado. No navega y
// no sabe en qué pantalla está (se puede usar en cualquier sitio). Recibe el puerto por constructor.
class EnrollmentViewModel(
    private val courseId: String,
    private val enrollment: EnrollmentStore,
    private val progressSync: ProgressSyncRequester,
) : ViewModel() {

    companion object {
        fun factory(courseId: String, enrollment: EnrollmentStore, progressSync: ProgressSyncRequester) =
            viewModelFactory { initializer { EnrollmentViewModel(courseId, enrollment, progressSync) } }
    }

    // null hasta que el almacén responde.
    private var enrolled: Boolean? = null
    private var enrolling = false

    var state by mutableStateOf<EnrollmentState>(EnrollmentState.Unknown)
        private set

    init {
        viewModelScope.launch {
            enrollment.enrolled(courseId).collect {
                enrolled = it
                refresh()
            }
        }
    }

    // Solo actúa con el botón activo (NotEnrolled). Marca «inscribiéndose» antes de lanzar la
    // corrutina: se ejecuta en el hilo principal, así que el segundo toque ya ve Enrolling y no hace
    // nada. enroll sin sesión lanza IllegalStateException (fallo de programación): no se captura.
    fun onEnroll() {
        if (state != EnrollmentState.NotEnrolled) return
        enrolling = true
        refresh()
        // enrolling no se apaga al terminar: lo apaga el Flow al emitir true (sin parpadeo a NotEnrolled).
        // Tras inscribirse se pide sincronizar; si enroll lanza, no se pide.
        viewModelScope.launch {
            enrollment.enroll(courseId)
            progressSync.requestSync()
        }
    }

    private fun refresh() {
        state = enrollmentStateOf(enrolled, enrolling)
    }
}

// Prioridad: inscrito (venga de donde venga) > inscribiéndose > no inscrito > desconocido.
private fun enrollmentStateOf(enrolled: Boolean?, enrolling: Boolean): EnrollmentState = when {
    enrolled == true -> EnrollmentState.Enrolled
    enrolling -> EnrollmentState.Enrolling
    enrolled == false -> EnrollmentState.NotEnrolled
    else -> EnrollmentState.Unknown
}
