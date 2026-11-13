package es.ulpgc.pamn.dominio.backend.domain.validation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertFailure;
import static es.ulpgc.pamn.dominio.backend.domain.validation.ResultAssertions.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultTest {

    private static final ValidationError ERROR = new ValidationError("field", "message");

    @Test
    void mapTransformsTheValueOfASuccess() {
        Result<Integer> length = Result.success("abc").map(String::length);

        assertEquals(3, assertSuccess(length));
    }

    @Test
    void mapKeepsTheErrorsOfAFailure() {
        Result<Integer> length = Result.<String>failure(List.of(ERROR)).map(String::length);

        assertEquals(List.of(ERROR), assertFailure(length));
    }

    @Test
    void foldChoosesTheBranchThatMatches() {
        String fromSuccess = Result.success("ok").fold(errors -> "ko", value -> value);
        String fromFailure = Result.<String>failure(List.of(ERROR)).fold(errors -> "ko", value -> value);

        assertEquals("ok", fromSuccess);
        assertEquals("ko", fromFailure);
    }

    @Test
    void validateReturnsSuccessWhenThereAreNoErrors() {
        assertEquals("built", assertSuccess(Result.validate(List.of(), () -> "built")));
    }

    @Test
    void validateDoesNotBuildTheValueWhenThereAreErrors() {
        Result<String> result = Result.validate(List.of(ERROR), () -> {
            throw new AssertionError("No debería construirse");
        });

        assertEquals(List.of(ERROR), assertFailure(result));
    }

    @Test
    void failureNeedsAtLeastOneError() {
        assertThrows(IllegalArgumentException.class, () -> Result.failure(List.of()));
    }

    @Test
    void failureIsImmutableEvenIfTheOriginalListChanges() {
        List<ValidationError> mutable = new ArrayList<>(List.of(ERROR));
        Result<String> result = Result.failure(mutable);

        mutable.add(new ValidationError("other", "other"));

        List<ValidationError> errors = assertFailure(result);
        assertEquals(1, errors.size());
        assertThrows(UnsupportedOperationException.class, () -> errors.add(ERROR));
    }

    @Test
    void combineAppliesTheFunctionWhenEveryResultIsASuccess() {
        Result<String> combined = Result.combine(
                Result.success("a"), Result.success(2), Result.success(true),
                (text, number, flag) -> text + number + flag);

        assertEquals("a2true", assertSuccess(combined));
    }

    @Test
    void combineAccumulatesTheErrorsOfEveryFailureInOrder() {
        ValidationError first = new ValidationError("first", "1");
        ValidationError third = new ValidationError("third", "3");

        Result<String> combined = Result.combine(
                Result.<String>failure(List.of(first)), Result.success(2), Result.<Boolean>failure(List.of(third)),
                (text, number, flag) -> text + number + flag);

        assertEquals(List.of(first, third), assertFailure(combined));
    }

    @Test
    void combineDoesNotCallTheFunctionWhenThereIsAFailure() {
        Result<String> combined = Result.combine(
                Result.success("a"), Result.<Integer>failure(List.of(ERROR)), Result.success(true),
                (text, number, flag) -> {
                    throw new AssertionError("No debería llamarse");
                });

        assertEquals(List.of(ERROR), assertFailure(combined));
    }

    @Test
    void successDoesNotAcceptNull() {
        assertThrows(NullPointerException.class, () -> Result.success(null));
    }
}
