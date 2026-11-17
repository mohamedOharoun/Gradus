package es.ulpgc.pamn.dominio.backend.domain.validation;

/**
 * Códigos de error que devuelve la API. Son parte del contrato con la app, que los traduce a sus
 * propios textos: se pueden añadir códigos nuevos, pero los existentes no se renombran ni se borran.
 */
public enum ErrorCode {
    /** El campo no viene en la petición. */
    REQUIRED,
    /** Nombre o contraseña con menos caracteres del mínimo. */
    TOO_SHORT,
    /** Nombre o correo con más caracteres del máximo. */
    TOO_LONG,
    INVALID_EMAIL,
    MISSING_LETTER,
    MISSING_DIGIT,
    MISSING_SPECIAL_CHARACTER,
    TERMS_NOT_ACCEPTED,
    /** Ya existe una cuenta con ese correo (respuesta 409). */
    EMAIL_TAKEN,
    /** El cuerpo no es un JSON válido o no encaja con lo esperado (campo «body»). */
    MALFORMED_BODY,
    /** Correo o contraseña incorrectos al iniciar sesión; nunca se dice cuál de los dos (respuesta 401). */
    INVALID_CREDENTIALS,
    /** Falta el token, no existe o ha caducado: hay que volver a iniciar sesión (respuesta 401). */
    UNAUTHENTICATED
}
