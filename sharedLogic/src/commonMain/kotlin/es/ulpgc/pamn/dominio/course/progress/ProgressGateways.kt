package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import es.ulpgc.pamn.dominio.course.progress.http.HttpProgressGateway

// Factory methods: la única forma de obtener un ProgressGateway real desde fuera de sharedLogic.
// Quien lo usa recibe la interfaz, nunca la clase concreta (como CourseGateways).
object ProgressGateways {

    fun http(baseUrl: String): ProgressGateway = HttpProgressGateway(gradusHttpClient(), baseUrl)
}
