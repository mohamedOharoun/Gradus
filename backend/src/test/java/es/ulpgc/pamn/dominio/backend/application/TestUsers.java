package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.TermsAcceptance;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;

import java.time.Instant;
import java.util.UUID;

/** Usuarios de ejemplo compartidos por los tests de los casos de uso (Object Mother). */
final class TestUsers {

    static final String ANA_PASSWORD = "Gradus2026!";

    private static final Instant REGISTERED_AT = Instant.parse("2026-11-13T10:00:00Z");

    /** Registrada con {@link FakePasswordHasher}, así que su hash es {@code fake$Gradus2026!}. */
    static final User ANA = new User(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            new UserName("Ana"), new Email("ana@ulpgc.es"), FakePasswordHasher.hashOf(ANA_PASSWORD),
            REGISTERED_AT, new TermsAcceptance(REGISTERED_AT));

    private TestUsers() {
    }
}
