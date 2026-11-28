package es.ulpgc.pamn.dominio.account

// Puerto: dónde guarda la app la sesión iniciada en este dispositivo (una sola a la vez).
// Hoy la implementa InMemorySessionStore (se pierde al cerrar la app); en la tarea 6 de
// registro-cuentas-reales.md, otra que la guarde cifrada en el móvil. Las funciones son suspend
// para que esa versión, que lee y escribe en disco, quepa sin cambiar este puerto.
interface SessionStore {

    // Guarda la sesión; si ya había una, la sustituye.
    suspend fun save(session: Session)

    // La sesión guardada, o null si no hay ninguna.
    suspend fun current(): Session?

    // Olvida la sesión (al cerrar sesión). Si no había ninguna, no hace nada.
    suspend fun clear()
}
