package es.ulpgc.pamn.dominio.backend.application;

import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static es.ulpgc.pamn.dominio.backend.application.TestUsers.ANA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Cambiar el nombre desde Ajustes (docs/tareas/ajustes.md, fase 2, tarea 1). */
class RenameUserTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final RenameUser renameUser = new RenameUser(repository);

    RenameUserTest() {
        repository.add(ANA);
    }

    private User stored(User user) {
        return repository.findById(user.id()).orElseThrow();
    }

    @Test
    void returnsTheRenamedUser() {
        var result = renameUser.rename(ANA, "Ana Ruiz");

        assertEquals(ANA.withName(new UserName("Ana Ruiz")), assertInstanceOf(RenameResult.Renamed.class, result).user());
    }

    @Test
    void storesTheNewName() {
        renameUser.rename(ANA, "Ana Ruiz");

        assertEquals("Ana Ruiz", stored(ANA).name().value());
    }

    // Igual que en el registro: sin espacios alrededor.
    @Test
    void storesTheNameWithoutSurroundingSpaces() {
        renameUser.rename(ANA, "  Ana Ruiz  ");

        assertEquals("Ana Ruiz", stored(ANA).name().value());
    }

    @Test
    void onlyTheNameChanges() {
        renameUser.rename(ANA, "Ana Ruiz");

        User after = stored(ANA);
        assertEquals(ANA.email(), after.email());
        assertEquals(ANA.passwordHash(), after.passwordHash());
        assertEquals(ANA.createdAt(), after.createdAt());
    }

    @Test
    void otherUsersDoNotChange() {
        User luis = new User(UUID.fromString("00000000-0000-0000-0000-000000000002"), new UserName("Luis"),
                new Email("luis@ulpgc.es"), ANA.passwordHash(), ANA.createdAt(), ANA.termsAcceptance());
        repository.add(luis);

        renameUser.rename(ANA, "Ana Ruiz");

        assertEquals(luis, stored(luis));
    }

    // Poner el mismo nombre no es un error.
    @Test
    void theSameNameIsFine() {
        assertInstanceOf(RenameResult.Renamed.class, renameUser.rename(ANA, "Ana"));
        assertEquals(ANA, stored(ANA));
    }

    @Test
    void rejectsATooShortNameAndKeepsTheOldOne() {
        var result = renameUser.rename(ANA, " A ");

        assertEquals(List.of(ErrorCode.TOO_SHORT), codes(result));
        assertEquals(ANA, stored(ANA));
    }

    @Test
    void rejectsATooLongName() {
        assertEquals(List.of(ErrorCode.TOO_LONG), codes(renameUser.rename(ANA, "a".repeat(UserName.MAX_LENGTH + 1))));
        assertEquals(ANA, stored(ANA));
    }

    // Un cuerpo sin «name» llega como null.
    @Test
    void rejectsAMissingName() {
        assertEquals(List.of(ErrorCode.REQUIRED), codes(renameUser.rename(ANA, null)));
        assertEquals(ANA, stored(ANA));
    }

    @Test
    void theErrorsAreAboutTheNameField() {
        var errors = assertInstanceOf(RenameResult.Rejected.class, renameUser.rename(ANA, "")).errors();

        assertEquals(List.of("name"), errors.stream().map(ValidationError::field).toList());
    }

    private static List<ErrorCode> codes(RenameResult result) {
        return assertInstanceOf(RenameResult.Rejected.class, result).errors().stream().map(ValidationError::code).toList();
    }
}
