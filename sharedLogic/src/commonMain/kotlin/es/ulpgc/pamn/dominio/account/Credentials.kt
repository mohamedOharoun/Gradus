package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.LoginForm

// Lo que la app envía para iniciar sesión. Como NewAccount, no sabe nada de JSON ni de HTTP.
data class Credentials(
    val email: String,
    val password: String,
) {
    // El toString generado mostraría la contraseña en los logs.
    override fun toString() = "Credentials(email=$email, password=****)"
}

// Mapper del formulario a las credenciales. null si el formulario no es válido: así nunca se envía
// uno a medias. El correo se recorta; la contraseña no (los espacios son parte de ella).
// Está en account/ (y no en LoginForm) para que model/ no dependa de account/.
fun LoginForm.toCredentials(): Credentials? =
    if (!isValid) null else Credentials(email = email.trim(), password = password)
