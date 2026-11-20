package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import java.util.List;
import java.util.stream.Stream;

/**
 * Una cláusula {@code WHERE} con sus parámetros, en el mismo orden que sus {@code ?}. Inmutable: se construye
 * combinando condiciones con {@link #and}, que junto con {@link #NONE} forma un monoide (elemento neutro y
 * operación asociativa), así que una lista de condiciones se reduce con {@code reduce(NONE, SqlWhere::and)}.
 */
record SqlWhere(List<String> conditions, List<SqlParameter> parameters) {

    /** Sin condiciones: no filtra nada. */
    static final SqlWhere NONE = new SqlWhere(List.of(), List.of());

    SqlWhere {
        conditions = List.copyOf(conditions);
        parameters = List.copyOf(parameters);
    }

    /** Factory method: una condición con un único {@code ?}. */
    static SqlWhere condition(String condition, SqlParameter parameter) {
        return new SqlWhere(List.of(condition), List.of(parameter));
    }

    SqlWhere and(SqlWhere other) {
        return new SqlWhere(
                Stream.concat(conditions.stream(), other.conditions.stream()).toList(),
                Stream.concat(parameters.stream(), other.parameters.stream()).toList());
    }

    /** El texto que se añade a la consulta: vacío si no hay condiciones. */
    String clause() {
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }
}
