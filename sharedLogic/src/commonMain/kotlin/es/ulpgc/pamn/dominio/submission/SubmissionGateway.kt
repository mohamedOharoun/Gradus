package es.ulpgc.pamn.dominio.submission

import es.ulpgc.pamn.dominio.account.Session

// Puerto (arquitectura hexagonal): lo que la app necesita del servidor para las entregas con foto
// (docs/tareas/ejercicio-foto.md, decisión T). De momento solo entregar; listar y descargar, cuando hagan falta.
interface SubmissionGateway {

    // Entrega la foto con la sesión del alumno (decisión L: hace falta sesión). No lanza excepciones:
    // cualquier fallo llega como un SubmitOutcome.Problem.
    suspend fun submit(session: Session, submission: PhotoSubmission): SubmitOutcome
}
