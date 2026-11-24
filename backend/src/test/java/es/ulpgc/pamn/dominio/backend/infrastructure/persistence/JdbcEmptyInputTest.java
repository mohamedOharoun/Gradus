package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Con la entrada vacía los adaptadores JDBC responden al momento: no piden ninguna conexión. */
class JdbcEmptyInputTest {

    @Test
    void findExistingWithNoIdsAnswersWithoutTouchingTheDatabase() {
        JdbcCourseExistence existence = new JdbcCourseExistence(NoConnectionDataSource.create());

        assertTrue(existence.findExisting(Set.of()).isEmpty());
    }

    @Test
    void mergeAllWithNoEnrollmentsReturnsWithoutTouchingTheDatabase() {
        JdbcEnrollmentRepository repository = new JdbcEnrollmentRepository(NoConnectionDataSource.create());

        assertDoesNotThrow(() -> repository.mergeAll(UUID.randomUUID(), List.of()));
    }
}
