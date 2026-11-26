package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.TestUsers.ANA;
import static es.ulpgc.pamn.dominio.backend.application.TestUsers.ANA_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Eliminar la cuenta desde Ajustes (docs/tareas/ajustes.md, fase 2, tarea 3, decisiones H e I). */
class DeleteAccountTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final SessionToken TOKEN = new SessionToken("token-de-ana");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final DeleteAccount deleteAccount = new DeleteAccount(users, hasher);

    DeleteAccountTest() {
        users.add(ANA);
        Session session = SessionPolicy.STANDARD.start(UUID.randomUUID(), ANA.id(), TOKEN.hash(), NOW);
        sessions.add(session);
    }

    private DeleteAccountResult delete(String password) {
        return deleteAccount.delete(ANA, new DeleteAccountCommand(password));
    }

    private boolean anaExists() {
        return users.findById(ANA.id()).isPresent();
    }

    // ── Bien ──

    @Test
    void withHerPasswordTheAccountIsDeleted() {
        assertInstanceOf(DeleteAccountResult.Deleted.class, delete(ANA_PASSWORD));

        assertFalse(anaExists());
    }

    // Su token deja de valer (en PostgreSQL, además, ON DELETE CASCADE borra sus sesiones).
    @Test
    void afterwardsHerTokenNoLongerWorks() {
        delete(ANA_PASSWORD);

        var result = new Authenticate(sessions, users, SessionPolicy.STANDARD, Clock.fixed(NOW, ZoneOffset.UTC))
                .authenticate(TOKEN);
        assertInstanceOf(AuthenticationResult.Unauthenticated.class, result);
    }

    @Test
    void otherAccountsStay() {
        User luis = new User(UUID.fromString("00000000-0000-0000-0000-000000000002"), new UserName("Luis"),
                new Email("luis@ulpgc.es"), ANA.passwordHash(), ANA.createdAt(), ANA.termsAcceptance());
        users.add(luis);

        delete(ANA_PASSWORD);

        assertEquals(List.of(luis), users.all());
    }

    // Su correo queda libre: puede volver a registrarse con él.
    @Test
    void theEmailIsFreeAgain() {
        delete(ANA_PASSWORD);

        assertTrue(users.add(ANA));
    }

    // ── No se borra ──

    @Test
    void aWrongPasswordDeletesNothing() {
        assertInstanceOf(DeleteAccountResult.WrongPassword.class, delete("NoEsLaMia1!"));

        assertTrue(anaExists());
    }

    @Test
    void thePasswordIsComparedAsTyped() {
        assertInstanceOf(DeleteAccountResult.WrongPassword.class, delete(" " + ANA_PASSWORD + " "));
        assertTrue(anaExists());
    }

    @Test
    void aMissingPasswordIsRequiredAndNotEvenChecked() {
        var errors = assertInstanceOf(DeleteAccountResult.Rejected.class, delete(null)).errors();

        assertEquals(List.of(new ValidationError("password", ErrorCode.REQUIRED, "Es obligatorio.")), errors);
        assertEquals(0, hasher.matchesCalls());
        assertTrue(anaExists());
    }

    // Decisión I: nadie se queda sin un curso que está estudiando.
    @Test
    void theAuthorOfCoursesCannotBeDeleted() {
        users.markAsAuthor(ANA.id());

        assertInstanceOf(DeleteAccountResult.AuthorOfCourses.class, delete(ANA_PASSWORD));
        assertTrue(anaExists());
    }

    // Antes se comprueba la contraseña: a quien no la sabe no se le dice si la cuenta tiene cursos.
    @Test
    void theAuthorWithAWrongPasswordGetsWrongPassword() {
        users.markAsAuthor(ANA.id());

        assertInstanceOf(DeleteAccountResult.WrongPassword.class, delete("NoEsLaMia1!"));
    }

    @Test
    void theCommandNeverRevealsThePassword() {
        assertFalse(new DeleteAccountCommand(ANA_PASSWORD).toString().contains(ANA_PASSWORD));
    }
}
