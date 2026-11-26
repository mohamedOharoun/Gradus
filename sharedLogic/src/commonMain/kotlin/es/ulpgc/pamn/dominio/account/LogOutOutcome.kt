package es.ulpgc.pamn.dominio.account

// Todos los desenlaces de cerrar sesión, también los fallos de red: son valores, no excepciones.
// Quien lo usa (Ajustes) borra la sesión del móvil en todos los casos: el usuario quiere salir, y
// si el servidor no se ha enterado, esa sesión caduca sola (docs/tareas/ajustes.md, decisión C).
sealed interface LogOutOutcome {

    // 204: sesión cerrada. También 401: el token ya no valía (caducado o cerrado en otro sitio).
    data object LoggedOut : LogOutOutcome

    // No se ha podido hablar con el servidor (sin red, apagado o tiempo agotado).
    data object Unreachable : LogOutOutcome

    // El servidor ha respondido algo inesperado (5xx u otro código).
    data object ServerFailure : LogOutOutcome
}
