package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Conversión entre {@link Instant} y las columnas {@code TIMESTAMPTZ}. El driver de PostgreSQL
 * trabaja con {@link OffsetDateTime}; siempre se usa UTC para que no influya la zona del servidor.
 */
final class Timestamps {

    private Timestamps() {
    }

    static OffsetDateTime toColumn(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    static Instant fromColumn(ResultSet row, String column) throws SQLException {
        return row.getObject(column, OffsetDateTime.class).toInstant();
    }
}
