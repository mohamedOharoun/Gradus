package es.ulpgc.pamn.dominio.data.progress

import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.ProgressGateway
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressSync
import es.ulpgc.pamn.dominio.course.progress.ProgressSyncStore

// Facade: una sola puerta al progreso guardado en el móvil. Crea UN RoomProgressStore y lo enseña
// por los tres puertos, que por tanto son el mismo objeto (si fueran dos, inscribirse no abriría
// nada). Recibe la base ya creada, y no el Context, para poder probarlo con una base en memoria.
class ProgressModule(database: ProgressDatabase, sessions: SessionStore, gateway: ProgressGateway) {

    private val store = RoomProgressStore(database.progressDao(), sessions)

    val progressStore: ProgressStore get() = store
    val enrollmentStore: EnrollmentStore get() = store
    val syncStore: ProgressSyncStore get() = store

    // La sincronización usa el MISMO store: lo fusionado se ve por los otros dos puertos.
    val sync = ProgressSync(store, gateway)
}
