package es.ulpgc.pamn.dominio.model

// Los campos del inicio de sesión.
enum class LoginField { EMAIL, PASSWORD }

// Un error que ha detectado el servidor, ya traducido a la app. field = null significa que es del
// formulario entero (correo o contraseña incorrectos: no se dice cuál de los dos).
// La traducción desde el código del servidor está en account/LogInApplyingOutcome.kt.
data class LoginProblem(val field: LoginField?, val error: FieldError)
