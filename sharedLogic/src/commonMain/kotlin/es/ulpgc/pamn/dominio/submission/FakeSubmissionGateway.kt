package es.ulpgc.pamn.dominio.submission

import es.ulpgc.pamn.dominio.account.Session

// Gateway falso, sin red: para previews, tests del ViewModel y probar la app sin backend. Por defecto
// acepta cada foto como una entrega nueva (decisión I); también se le puede decir qué responder.
class FakeSubmissionGateway(
    private val answer: (Session, PhotoSubmission) -> SubmitOutcome,
) : SubmissionGateway {

    // Responde siempre lo mismo, p. ej. FakeSubmissionGateway(SubmitOutcome.Unreachable).
    constructor(outcome: SubmitOutcome) : this({ _, _ -> outcome })

    // Sin argumentos: acepta todas, cada una con su id («entrega-1», «entrega-2»…).
    constructor() : this(answer = acceptingAll())

    // Lo que le han pedido, en orden (para comprobarlo en los tests).
    val received: List<PhotoSubmission> get() = requests.map { it.second }
    val sessions: List<Session> get() = requests.map { it.first }
    private val requests = mutableListOf<Pair<Session, PhotoSubmission>>()

    override suspend fun submit(session: Session, submission: PhotoSubmission): SubmitOutcome {
        requests += session to submission
        return answer(session, submission)
    }

    private companion object {
        fun acceptingAll(): (Session, PhotoSubmission) -> SubmitOutcome {
            var count = 0
            return { _, _ ->
                count++
                // Un minuto después cada una (hasta 59 seguidas: de sobra para previews y tests).
                val minute = (count % 60).toString().padStart(2, '0')
                SubmitOutcome.Submitted(Submission(id = "entrega-$count", submittedAt = "2026-11-25T10:$minute:00Z"))
            }
        }
    }
}
