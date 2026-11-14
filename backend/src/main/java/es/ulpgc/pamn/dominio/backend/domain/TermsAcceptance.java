package es.ulpgc.pamn.dominio.backend.domain;

import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Constancia de que el usuario aceptó los términos y cuándo. Solo puede existir si los aceptó. */
public record TermsAcceptance(Instant acceptedAt) {

    private static final String FIELD = "acceptedTerms";
    private static final String NOT_ACCEPTED = "Debes aceptar los términos para crear la cuenta.";

    public TermsAcceptance {
        Objects.requireNonNull(acceptedAt, "acceptedAt");
    }

    /**
     * @param accepted lo que marcó el usuario; {@code null} (campo ausente) cuenta como no aceptado.
     * @param now      el momento que quedará registrado si los aceptó.
     */
    public static Result<TermsAcceptance> of(Boolean accepted, Instant now) {
        return Boolean.TRUE.equals(accepted)
                ? Result.success(new TermsAcceptance(now))
                : Result.failure(List.of(new ValidationError(FIELD, ErrorCode.TERMS_NOT_ACCEPTED, NOT_ACCEPTED)));
    }
}
