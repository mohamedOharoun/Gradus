package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session

// Gateway falso, sin red: para los tests de progreso. Por defecto responde «sincronizado, sin nada»
// en sync y «dado de baja» en unenroll; ambas lambdas pueden suspender (p. ej. esperar un
// CompletableDeferred) para decidir en los tests cuándo llega la respuesta.
// Cada llamada se apunta en `calls` / `unenrollCalls` antes de suspender.
class FakeProgressGateway(
    private val answer: suspend (session: Session, pending: ProgressSnapshot) -> SyncOutcome =
        { _, _ -> SyncOutcome.Synced(SyncResponse(ProgressSnapshot(emptyList()), emptyList())) },
    private val unenrollAnswer: suspend (session: Session, courseId: String) -> UnenrollOutcome =
        { _, _ -> UnenrollOutcome.Unenrolled },
) : ProgressGateway {

    // Lo que le han enviado en sync, en orden (para comprobarlo en los tests).
    val calls: List<SyncCall> get() = received.toList()
    private val received = mutableListOf<SyncCall>()

    // Las bajas pedidas, en orden (sin sesión ni token).
    val unenrollCalls: List<UnenrollCall> get() = receivedUnenrolls.toList()
    private val receivedUnenrolls = mutableListOf<UnenrollCall>()

    override suspend fun sync(session: Session, pending: ProgressSnapshot): SyncOutcome {
        // Se guarda el usuario, no la sesión: el token no se queda en nada que el Fake pueda mostrar.
        received += SyncCall(userId = session.account.id, sent = pending)
        return answer(session, pending)
    }

    override suspend fun unenroll(session: Session, courseId: String): UnenrollOutcome {
        receivedUnenrolls += UnenrollCall(userId = session.account.id, courseId = courseId)
        return unenrollAnswer(session, courseId)
    }
}

// Una llamada de sincronización recibida por FakeProgressGateway: quién (su id) y qué subió.
data class SyncCall(val userId: String, val sent: ProgressSnapshot)

// Una llamada de baja recibida por FakeProgressGateway: quién (su id) y de qué curso.
data class UnenrollCall(val userId: String, val courseId: String)
