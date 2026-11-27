package es.ulpgc.pamn.dominio.data.publication

// Puerto ligero en app/ (patrón Command, igual que SessionSignOut): limpia los borradores locales
// de una cuenta al eliminarla, sin acoplar DeleteAccountViewModel a Room ni a WorkManager.
fun interface ClearUserDrafts {
    suspend fun clearForUser(userId: String)
}
