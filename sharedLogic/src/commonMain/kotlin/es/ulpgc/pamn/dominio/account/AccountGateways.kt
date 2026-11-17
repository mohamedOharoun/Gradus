package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.account.http.HttpAccountGateway
import es.ulpgc.pamn.dominio.account.http.gradusHttpClient

// Factory methods: la única forma de obtener un AccountGateway real desde fuera de sharedLogic.
// Quien lo usa recibe la interfaz, nunca la clase concreta: cambiar a gRPC sería añadir grpc(…) aquí.
object AccountGateways {

    fun http(baseUrl: String): AccountGateway = HttpAccountGateway(gradusHttpClient(), baseUrl)
}
