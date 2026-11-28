package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las entregas propias en un curso (docs/tareas/ejercicio-foto.md, paso 3c). */
class ListMySubmissionsTest {

    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID LUIS = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID GRAFOS = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID REDES = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final Instant MORNING = Instant.parse("2026-11-24T09:00:00Z");

    private final InMemorySubmissionRepository repository = new InMemorySubmissionRepository();
    private final ListMySubmissions listMine = new ListMySubmissions(repository);

    /** Una entrega guardada, con el id acabado en {@code n} y hecha {@code minutes} después de las 9:00. */
    private Submission stored(int n, UUID student, UUID course, int minutes) {
        Jpeg photo = ((JpegCheck.Valid) Jpeg.check(SubmitPhotoTest.jpeg(100 + n))).jpeg();
        Submission submission = Submission.pending(UUID.fromString("00000000-0000-0000-0000-0000000000%02d".formatted(n)),
                student, course, 1, "repr-foto", MORNING.plusSeconds(60L * minutes), photo);
        repository.add(submission, photo);
        return submission;
    }

    @Test
    void withoutSubmissionsTheListIsEmpty() {
        assertTrue(listMine.list(ANA, GRAFOS).isEmpty());
    }

    // Decisión I: cada envío es una entrega nueva; la más reciente, primero (es la que cuenta).
    @Test
    void theNewestGoesFirst() {
        Submission first = stored(1, ANA, GRAFOS, 0);
        Submission second = stored(2, ANA, GRAFOS, 30);
        Submission third = stored(3, ANA, GRAFOS, 10);

        assertEquals(List.of(second, third, first), listMine.list(ANA, GRAFOS));
    }

    // Dos entregas en el mismo instante: el orden no cambia de una vez a otra (por id, de mayor a menor).
    @Test
    void submissionsAtTheSameMomentKeepAFixedOrder() {
        Submission a = stored(1, ANA, GRAFOS, 5);
        Submission b = stored(2, ANA, GRAFOS, 5);

        assertEquals(List.of(b, a), listMine.list(ANA, GRAFOS));
    }

    // Decisión H: cada alumno solo ve las suyas.
    @Test
    void onlyTheStudentsOwnAreListed() {
        Submission mine = stored(1, ANA, GRAFOS, 0);
        stored(2, LUIS, GRAFOS, 1);

        assertEquals(List.of(mine), listMine.list(ANA, GRAFOS));
    }

    @Test
    void onlyThoseOfThatCourseAreListed() {
        Submission graphs = stored(1, ANA, GRAFOS, 0);
        stored(2, ANA, REDES, 1);

        assertEquals(List.of(graphs), listMine.list(ANA, GRAFOS));
    }

    // Un curso que no existe no es un error: simplemente no hay entregas (y no se cuenta si existe).
    @Test
    void anUnknownCourseGivesAnEmptyList() {
        stored(1, ANA, GRAFOS, 0);
        assertTrue(listMine.list(ANA, UUID.fromString("00000000-0000-0000-0000-0000000000ff")).isEmpty());
    }

    // Lo listado es Submission: lleva el tamaño de la foto, nunca sus bytes (listar no carga fotos).
    @Test
    void theListCarriesTheSizeOfThePhotoNotItsBytes() {
        stored(7, ANA, GRAFOS, 0);
        assertEquals(107, listMine.list(ANA, GRAFOS).getFirst().photoBytes());
    }
}
