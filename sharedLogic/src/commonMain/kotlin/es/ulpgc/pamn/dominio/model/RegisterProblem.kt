package es.ulpgc.pamn.dominio.model

// Un error que ha detectado el servidor, ya traducido a la app: en qué paso del registro se
// muestra y qué error es. La traducción desde el código del servidor está en account/RegisterProblem.kt.
data class RegisterProblem(val step: RegisterStep, val error: FieldError)
