package es.ulpgc.pamn.dominio.backend.domain.validation;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Resultado de una operación que puede fallar por validación: o bien un valor ({@link Success}),
 * o bien una lista no vacía de errores ({@link Failure}). Sustituye a las excepciones para los
 * errores esperables, de forma que el compilador obliga a tratar ambos casos.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {
        public Success {
            Objects.requireNonNull(value, "value");
        }
    }

    record Failure<T>(List<ValidationError> errors) implements Result<T> {
        public Failure {
            errors = List.copyOf(errors);
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("Un Failure necesita al menos un error");
            }
        }
    }

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(List<ValidationError> errors) {
        return new Failure<>(errors);
    }

    /** Si no hay errores, construye el valor con {@code factory}; si los hay, ni siquiera lo intenta. */
    static <T> Result<T> validate(List<ValidationError> errors, Supplier<T> factory) {
        return errors.isEmpty() ? success(factory.get()) : failure(errors);
    }

    /** Función de tres argumentos (Java solo trae {@code Function} y {@code BiFunction}). */
    @FunctionalInterface
    interface Function3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    /**
     * Une tres resultados independientes. Si todos son éxito, aplica {@code combiner} a sus valores;
     * si no, devuelve los errores de <b>todos</b> los fallos (no solo del primero).
     */
    static <A, B, C, R> Result<R> combine(Result<A> first, Result<B> second, Result<C> third,
                                          Function3<? super A, ? super B, ? super C, ? extends R> combiner) {
        if (first instanceof Success<A>(A a) && second instanceof Success<B>(B b) && third instanceof Success<C>(C c)) {
            return success(combiner.apply(a, b, c));
        }
        return failure(Stream.of(first, second, third).flatMap(Result::errorsOf).toList());
    }

    private static Stream<ValidationError> errorsOf(Result<?> result) {
        return switch (result) {
            case Success<?> success -> Stream.empty();
            case Failure<?> failure -> failure.errors().stream();
        };
    }

    default <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        return switch (this) {
            case Success<T> success -> success(mapper.apply(success.value()));
            case Failure<T> failure -> failure(failure.errors());
        };
    }

    default <R> R fold(Function<List<ValidationError>, R> onFailure, Function<? super T, R> onSuccess) {
        return switch (this) {
            case Success<T> success -> onSuccess.apply(success.value());
            case Failure<T> failure -> onFailure.apply(failure.errors());
        };
    }
}
