package es.ulpgc.pamn.dominio.account

// Lo que se envía para cambiar la contraseña: la actual (para demostrar que eres tú) y la nueva.
data class PasswordChange(
    val currentPassword: String,
    val newPassword: String,
) {
    // El toString generado mostraría las dos contraseñas en los logs.
    override fun toString() = "PasswordChange(currentPassword=****, newPassword=****)"
}
