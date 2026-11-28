package es.ulpgc.pamn.dominio.backend.domain.course;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Ventana de «fecha de publicación» de la búsqueda (§4.5): el último mes o el último año. Cada constante
 * sabe restar su periodo (Strategy dentro del enum), así que {@link #since} es una función pura: no lee
 * el reloj, quien llama le da el «ahora».
 */
public enum PublishedWithin {

    /** Valor {@code month}. */
    LAST_MONTH("month", now -> now.minusMonths(1)),
    /** Valor {@code year}. */
    LAST_YEAR("year", now -> now.minusYears(1));

    private final String parameter;
    private final UnaryOperator<OffsetDateTime> subtractPeriod;

    PublishedWithin(String parameter, UnaryOperator<OffsetDateTime> subtractPeriod) {
        this.parameter = parameter;
        this.subtractPeriod = subtractPeriod;
    }

    /**
     * El valor del parámetro {@code published}. Se admiten espacios alrededor, pero solo en minúsculas
     * (como {@code page} y {@code size}, que también recortan). Nulo, vacío o desconocido da vacío.
     */
    public static Optional<PublishedWithin> fromParameter(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.strip();
        return Arrays.stream(values()).filter(within -> within.parameter.equals(value)).findFirst();
    }

    /**
     * Desde cuándo cuenta «último mes» o «último año» si ahora es {@code now}: la misma hora, restando el
     * periodo <b>en el calendario y en UTC</b> (nunca en la zona del servidor). Si el día no existe en el
     * mes de destino, se usa el último día de ese mes (31 de marzo → 28 de febrero).
     */
    public Instant since(Instant now) {
        Objects.requireNonNull(now, "now");
        return subtractPeriod.apply(now.atOffset(ZoneOffset.UTC)).toInstant();
    }
}
