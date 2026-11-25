package es.ulpgc.pamn.dominio.submission

import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import es.ulpgc.pamn.dominio.submission.http.HttpSubmissionGateway

// Factory method: la única forma de obtener un SubmissionGateway real desde fuera de sharedLogic
// (como AccountGateways). Quien lo usa recibe la interfaz, nunca la clase concreta.
object SubmissionGateways {

    fun http(baseUrl: String): SubmissionGateway = HttpSubmissionGateway(gradusHttpClient(), baseUrl)
}
