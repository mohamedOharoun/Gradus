package es.ulpgc.pamn.dominio.course

import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import es.ulpgc.pamn.dominio.course.http.HttpCourseGateway

// Factory methods: la única forma de obtener un CourseGateway real desde fuera de sharedLogic.
// Quien lo usa recibe la interfaz, nunca la clase concreta (como AccountGateways).
object CourseGateways {

    fun http(baseUrl: String): CourseGateway = HttpCourseGateway(gradusHttpClient(), baseUrl)
}
