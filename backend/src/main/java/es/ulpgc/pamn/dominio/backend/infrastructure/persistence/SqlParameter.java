package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Un valor para un {@code ?} de una sentencia, con su tipo SQL. Los arrays necesitan la conexión para
 * crearse ({@code connection.createArrayOf}), así que aquí solo se describen y se vinculan después.
 */
sealed interface SqlParameter {

    // Los records de una interfaz son públicos implícitamente: su constructor también debe serlo.
    record Text(String value) implements SqlParameter {
        public Text {
            Objects.requireNonNull(value, "value");
        }
    }

    /** Un {@code TEXT[]} de PostgreSQL. */
    record TextArray(List<String> values) implements SqlParameter {
        public TextArray {
            values = List.copyOf(values);
        }
    }

    /** Un {@code TIMESTAMPTZ}: se vincula con {@link Timestamps#toColumn}, en UTC. */
    record Timestamp(Instant value) implements SqlParameter {
        public Timestamp {
            Objects.requireNonNull(value, "value");
        }
    }
}
