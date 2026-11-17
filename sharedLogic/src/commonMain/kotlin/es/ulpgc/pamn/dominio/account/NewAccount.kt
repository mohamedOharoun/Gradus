package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.RegisterForm

// Lo que la app pide crear. No sabe nada de JSON ni de HTTP: el adaptador lo traduce a su formato.
// La repetición de la contraseña no viaja: es solo una comprobación de la interfaz.
data class NewAccount(
    val name: String,
    val email: String,
    val password: String,
    val acceptedTerms: Boolean,
) {
    // El toString generado mostraría la contraseña en los logs.
    override fun toString() = "NewAccount(name=$name, email=$email, password=****, acceptedTerms=$acceptedTerms)"
}

// Mapper del formulario a la petición. null si todavía no se puede registrar: así nunca se envía
// un formulario a medias. Nombre y correo se recortan; la contraseña no (los espacios son parte de ella).
fun RegisterForm.toNewAccount(): NewAccount? =
    if (!canRegister) null
    else NewAccount(name = name.trim(), email = email.trim(), password = password, acceptedTerms = acceptedTerms)
