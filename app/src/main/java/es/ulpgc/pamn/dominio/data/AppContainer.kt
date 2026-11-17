package es.ulpgc.pamn.dominio.data

import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.AccountGateways

// Composition root (inyección manual): el único sitio de la app que elige las implementaciones
// concretas, como App.java en el backend. El resto recibe interfaces por parámetro.
// Para probar la app sin backend: AccountGateway = FakeAccountGateway().
object AppContainer {

    // 10.0.2.2 es «el ordenador» visto desde el emulador de Android; 7070 es el puerto del backend.
    private const val BASE_URL = "http://10.0.2.2:7070"

    // lazy: se crea la primera vez que alguien lo pide, y una sola vez para toda la app.
    val accountGateway: AccountGateway by lazy { AccountGateways.http(BASE_URL) }
}
