package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.account.http.gradusHttpClient
import es.ulpgc.pamn.dominio.course.publication.http.HttpCoursePublicationGateway

// Factory method: única entrada pública para crear el adaptador HTTP de publicación desde fuera
// de sharedLogic, manteniendo oculta la implementación concreta.
object CoursePublicationGateways {

    fun http(baseUrl: String): CoursePublicationGateway =
        HttpCoursePublicationGateway(gradusHttpClient(), baseUrl)
}
