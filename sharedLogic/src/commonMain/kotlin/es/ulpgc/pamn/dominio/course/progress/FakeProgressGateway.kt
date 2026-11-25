package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Session

// Gateway falso, sin red: para los tests de ProgressSync. Por defecto responde «sincronizado, sin nada»;
// con `answer` se le dice qué responder (puede depender de la sesión y de lo enviado). `answer` puede
// suspender (p. ej. esperar un CompletableDeferred): así un test decide cuándo llega la respuesta.
// La llamada se apunta en `calls` antes de responder.
class FakeProgressGateway(
    private val answer: suspend (session: Session, pending: ProgressSnapshot) -> SyncOutcome =
        { _, _ -> SyncOutcome.Synced(SyncResponse(ProgressSnapshot(emptyList()), emptyList())) },
) : ProgressGateway {

    // Lo que le han enviado, en orden (para comprobarlo en los tests).
    val calls: List<SyncCall> get() = received.toList()
    private val received = mutableListOf<SyncCall>()

    override suspend fun sync(session: Session, pending: ProgressSnapshot): SyncOutcome {
        // Se guarda el usuario, no la sesión: el token no se queda en nada que el Fake pueda mostrar.
        received += SyncCall(userId = session.account.id, sent = pending)
        return answer(session, pending)
    }
}

// Una llamada recibida por FakeProgressGateway: quién (su id) y qué subió.
data class SyncCall(val userId: String, val sent: ProgressSnapshot)
