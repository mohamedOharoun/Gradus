package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.RegisterProblem
import es.ulpgc.pamn.dominio.model.RegisterStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegisterProblemTest {

    private fun problemOf(field: String, code: String) = ServerFieldError(field, code).toRegisterProblem()

    // ── ApiErrorCode ──

    @Test
    fun `api error codes mirror ErrorCode in the backend`() {
        assertEquals(
            listOf(
                "REQUIRED", "TOO_SHORT", "TOO_LONG", "INVALID_EMAIL", "MISSING_LETTER", "MISSING_DIGIT",
                "MISSING_SPECIAL_CHARACTER", "TERMS_NOT_ACCEPTED", "EMAIL_TAKEN", "MALFORMED_BODY",
                "INVALID_CREDENTIALS", "UNAUTHENTICATED",
                // Cursos (backend/docs/api-cursos.md).
                "INVALID_PARAMETER", "COURSE_NOT_FOUND", "NOT_COURSE_AUTHOR", "IDENTICAL_TO_LATEST",
                // Ajustes (docs/tareas/ajustes.md, fase 2).
                "WRONG_PASSWORD", "AUTHOR_OF_COURSES",
                // Entregas con foto (docs/tareas/ejercicio-foto.md, decisión P).
                "INVALID_PHOTO", "PHOTO_TOO_LARGE", "EXERCISE_NOT_FOUND", "SUBMISSION_NOT_FOUND", "NOT_SUBMISSION_OWNER",
            ),
            ApiErrorCode.entries.filter { it != ApiErrorCode.UNKNOWN }.map { it.name },
        )
    }

    @Test
    fun `a known code is read by its name`() {
        assertEquals(ApiErrorCode.EMAIL_TAKEN, ServerFieldError("email", "EMAIL_TAKEN").apiCode)
    }

    @Test
    fun `a code the app does not know yet is unknown`() {
        assertEquals(ApiErrorCode.UNKNOWN, ServerFieldError("email", "SOMETHING_NEW").apiCode)
    }

    // ── Una prueba por fila de «Códigos de error» ──

    @Test
    fun `REQUIRED is an empty field in its step`() {
        assertEquals(RegisterProblem(RegisterStep.NAME, FieldError.EMPTY), problemOf("name", "REQUIRED"))
        assertEquals(RegisterProblem(RegisterStep.EMAIL, FieldError.EMPTY), problemOf("email", "REQUIRED"))
        assertEquals(RegisterProblem(RegisterStep.PASSWORD, FieldError.EMPTY), problemOf("password", "REQUIRED"))
    }

    @Test
    fun `TOO_SHORT is too short for the name and a weak password for the password`() {
        assertEquals(RegisterProblem(RegisterStep.NAME, FieldError.TOO_SHORT), problemOf("name", "TOO_SHORT"))
        assertEquals(RegisterProblem(RegisterStep.PASSWORD, FieldError.WEAK_PASSWORD), problemOf("password", "TOO_SHORT"))
    }

    @Test
    fun `TOO_LONG is too long in the name and email steps`() {
        assertEquals(RegisterProblem(RegisterStep.NAME, FieldError.TOO_LONG), problemOf("name", "TOO_LONG"))
        assertEquals(RegisterProblem(RegisterStep.EMAIL, FieldError.TOO_LONG), problemOf("email", "TOO_LONG"))
    }

    @Test
    fun `INVALID_EMAIL is an invalid email in the email step`() {
        assertEquals(RegisterProblem(RegisterStep.EMAIL, FieldError.INVALID_EMAIL), problemOf("email", "INVALID_EMAIL"))
    }

    @Test
    fun `missing letter, digit or special character is a weak password`() {
        listOf("MISSING_LETTER", "MISSING_DIGIT", "MISSING_SPECIAL_CHARACTER").forEach { code ->
            assertEquals(RegisterProblem(RegisterStep.PASSWORD, FieldError.WEAK_PASSWORD), problemOf("password", code))
        }
    }

    @Test
    fun `TERMS_NOT_ACCEPTED goes to the password step, where the terms checkbox is`() {
        assertEquals(
            RegisterProblem(RegisterStep.PASSWORD, FieldError.TERMS_NOT_ACCEPTED),
            problemOf("acceptedTerms", "TERMS_NOT_ACCEPTED"),
        )
    }

    @Test
    fun `EMAIL_TAKEN goes back to the email step`() {
        assertEquals(RegisterProblem(RegisterStep.EMAIL, FieldError.EMAIL_TAKEN), problemOf("email", "EMAIL_TAKEN"))
    }

    @Test
    fun `MALFORMED_BODY is not a problem of any field`() {
        assertNull(problemOf("body", "MALFORMED_BODY"))
    }

    @Test
    fun `login and session codes are not register problems`() {
        assertNull(problemOf("credentials", "INVALID_CREDENTIALS"))
        assertNull(problemOf("authorization", "UNAUTHENTICATED"))
    }

    // ── Lo que la app no conoce no rompe nada ──

    @Test
    fun `an unknown code is not a field problem`() {
        assertNull(problemOf("email", "SOMETHING_NEW"))
    }

    @Test
    fun `an unknown field is not a problem of any step`() {
        assertNull(problemOf("nickname", "REQUIRED"))
    }

    @Test
    fun `a code that makes no sense for the field is not a problem`() {
        assertNull(problemOf("name", "INVALID_EMAIL"))
    }

    // ── Varios errores a la vez ──

    @Test
    fun `the first problem is the one in the earliest step`() {
        val errors = listOf(
            ServerFieldError("password", "MISSING_DIGIT"),
            ServerFieldError("body", "MALFORMED_BODY"),
            ServerFieldError("email", "INVALID_EMAIL"),
        )
        assertEquals(RegisterProblem(RegisterStep.EMAIL, FieldError.INVALID_EMAIL), errors.firstRegisterProblem())
    }

    @Test
    fun `without known problems there is no first problem`() {
        assertNull(listOf(ServerFieldError("body", "MALFORMED_BODY")).firstRegisterProblem())
        assertNull(emptyList<ServerFieldError>().firstRegisterProblem())
    }
}
