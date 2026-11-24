package es.ulpgc.pamn.dominio.backend.web.progress;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Fechas del cuerpo: ISO-8601 con {@link Instant#parse}, y solo con el año (en UTC) entre 0001 y 9999,
 * para que {@code Instant.toString()} y PostgreSQL (4713 a. C. – 294276 d. C.) las puedan guardar y devolver
 * sin sorpresas. Función pura: ninguna excepción sale de aquí; lo que no se entiende es un {@code Optional} vacío.
 */
final class IsoInstants {

    private static final Instant FIRST = LocalDate.of(1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Instant AFTER_LAST = LocalDate.of(10_000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

    private IsoInstants() {
    }

    static Optional<Instant> parse(String text) {
        try {
            return Optional.of(Instant.parse(text)).filter(IsoInstants::inSupportedYears);
        } catch (DateTimeException e) { // DateTimeParseException es una subclase; también cubre desbordes de rango
            return Optional.empty();
        }
    }

    private static boolean inSupportedYears(Instant instant) {
        return !instant.isBefore(FIRST) && instant.isBefore(AFTER_LAST);
    }
}
