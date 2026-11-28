package es.ulpgc.pamn.dominio.course.progress

// Ayudas de los tests de ProgressSync (el caso de uso entero: almacén real + gateway falso).

// La sesión con la que sincroniza «ana» (su token es «t-ana»).
internal val ANA = sessionOf("ana")

// Un 200: todo el progreso del servidor y, si se da, lo que omitió.
internal fun synced(remote: ProgressSnapshot = snapshot(), vararg skipped: SkippedProgress) =
    SyncOutcome.Synced(SyncResponse(remote, skipped.toList()))

// El caso de uso sobre el almacén de la fixture y el gateway dado.
internal fun ProgressFixture.syncWith(gateway: ProgressGateway) = ProgressSync(store, gateway)
