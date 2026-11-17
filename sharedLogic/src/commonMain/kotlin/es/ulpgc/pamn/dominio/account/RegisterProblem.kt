package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.account.ApiErrorCode.EMAIL_TAKEN
import es.ulpgc.pamn.dominio.account.ApiErrorCode.INVALID_EMAIL
import es.ulpgc.pamn.dominio.account.ApiErrorCode.MISSING_DIGIT
import es.ulpgc.pamn.dominio.account.ApiErrorCode.MISSING_LETTER
import es.ulpgc.pamn.dominio.account.ApiErrorCode.MISSING_SPECIAL_CHARACTER
import es.ulpgc.pamn.dominio.account.ApiErrorCode.REQUIRED
import es.ulpgc.pamn.dominio.account.ApiErrorCode.TERMS_NOT_ACCEPTED
import es.ulpgc.pamn.dominio.account.ApiErrorCode.TOO_LONG
import es.ulpgc.pamn.dominio.account.ApiErrorCode.TOO_SHORT
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.RegisterProblem
import es.ulpgc.pamn.dominio.model.RegisterStep

// Tabla de traducción (campo, código) → problema. Es la tabla «Códigos de error» de
// docs/tareas/registro-cuentas-reales.md: si el servidor añade un caso, se añade una línea aquí.
// Lo que no está en la tabla (MALFORMED_BODY, códigos desconocidos…) no es culpa de un campo.
private val translations: Map<Pair<String, ApiErrorCode>, RegisterProblem> = mapOf(
    ("name" to REQUIRED) to RegisterProblem(RegisterStep.NAME, FieldError.EMPTY),
    ("name" to TOO_SHORT) to RegisterProblem(RegisterStep.NAME, FieldError.TOO_SHORT),
    ("name" to TOO_LONG) to RegisterProblem(RegisterStep.NAME, FieldError.TOO_LONG),

    ("email" to REQUIRED) to RegisterProblem(RegisterStep.EMAIL, FieldError.EMPTY),
    ("email" to INVALID_EMAIL) to RegisterProblem(RegisterStep.EMAIL, FieldError.INVALID_EMAIL),
    ("email" to TOO_LONG) to RegisterProblem(RegisterStep.EMAIL, FieldError.TOO_LONG),
    ("email" to EMAIL_TAKEN) to RegisterProblem(RegisterStep.EMAIL, FieldError.EMAIL_TAKEN),

    // En la app los requisitos de la contraseña se ven al escribir: el servidor solo confirma que es débil.
    ("password" to REQUIRED) to RegisterProblem(RegisterStep.PASSWORD, FieldError.EMPTY),
    ("password" to TOO_SHORT) to RegisterProblem(RegisterStep.PASSWORD, FieldError.WEAK_PASSWORD),
    ("password" to MISSING_LETTER) to RegisterProblem(RegisterStep.PASSWORD, FieldError.WEAK_PASSWORD),
    ("password" to MISSING_DIGIT) to RegisterProblem(RegisterStep.PASSWORD, FieldError.WEAK_PASSWORD),
    ("password" to MISSING_SPECIAL_CHARACTER) to RegisterProblem(RegisterStep.PASSWORD, FieldError.WEAK_PASSWORD),

    // La casilla de los términos está en el paso de la contraseña.
    ("acceptedTerms" to TERMS_NOT_ACCEPTED) to RegisterProblem(RegisterStep.PASSWORD, FieldError.TERMS_NOT_ACCEPTED),
)

// null si el error no corresponde a ningún campo del registro.
fun ServerFieldError.toRegisterProblem(): RegisterProblem? = translations[field to apiCode]

// El problema del paso más temprano: es donde el registro debe volver para que se corrija.
fun List<ServerFieldError>.firstRegisterProblem(): RegisterProblem? =
    mapNotNull { it.toRegisterProblem() }.minByOrNull { it.step }
