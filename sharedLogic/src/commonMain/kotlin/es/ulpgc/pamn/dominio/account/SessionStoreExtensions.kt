package es.ulpgc.pamn.dominio.account

// Borra la sesión guardada solo si es la de `session`; si hay otra (alguien acaba de iniciar sesión) o
// ninguna, no hace nada. Se compara solo el token, no la sesión entera: un 401 invalida el token sea
// cual sea su expiresAt, así que la misma llave con otra fecha sigue siendo la que ha caducado.
// Es una extensión: el puerto SessionStore no cambia.
// Límite: leer y borrar son dos pasos y el puerto no tiene uno atómico; si otra corrutina guarda una
// sesión justo entre los dos, se borraría esa. La ventana es mínima; el caso realista (guardar mientras
// la petición viaja) sí queda protegido.
suspend fun SessionStore.clearIfCurrent(session: Session) {
    if (current()?.token == session.token) clear()
}
