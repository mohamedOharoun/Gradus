package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.module;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseDocumentTest {

    @Test
    void theDefaultTestCourseIsBuilt() {
        CourseDocument course = course().build();

        assertEquals(1, course.formatVersion());
        assertEquals(List.of("representacion", "bfs"), course.modules().stream().map(Module::id).toList());
    }

    @Test
    void changingOneModuleLeavesTheOthersAsTheyWere() {
        CourseDocument course = course().module(1, m -> m.title("Recorrido en anchura")).build();

        assertEquals("Recorrido en anchura", course.modules().get(1).title());
        assertEquals(List.of("representacion"), course.modules().get(1).requires());
        assertEquals(course().build().modules().getFirst(), course.modules().getFirst());
    }

    @Test
    void onlyFormatVersion1IsSupported() {
        assertThrows(IllegalArgumentException.class, () -> course().formatVersion(2).build());
    }

    @Test
    void titleAndDescriptionAreSingleLinePlainText() {
        assertDoesNotThrow(() -> course().title("x".repeat(120)).description("y".repeat(500)).build());
        assertThrows(IllegalArgumentException.class, () -> course().title("x".repeat(121)).build());
        assertThrows(IllegalArgumentException.class, () -> course().description("y".repeat(501)).build());
        assertThrows(IllegalArgumentException.class, () -> course().title(" Grafos").build());
        assertThrows(IllegalArgumentException.class, () -> course().description("Dos\nlíneas").build());
    }

    @Test
    void languageIsAnIsoCodeWithOptionalRegion() {
        assertDoesNotThrow(() -> course().language("es-ES").build());
        assertThrows(IllegalArgumentException.class, () -> course().language("spanish").build());
    }

    @Test
    void tagsAreBetween1And10DistinctShortPlainTexts() {
        String[] ten = IntStream.range(0, 10).mapToObj(i -> "tag" + i).toArray(String[]::new);
        String[] eleven = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toArray(String[]::new);

        assertDoesNotThrow(() -> course().tags(ten).build());
        assertThrows(IllegalArgumentException.class, () -> course().tags().build());
        assertThrows(IllegalArgumentException.class, () -> course().tags(eleven).build());
        assertThrows(IllegalArgumentException.class, () -> course().tags("grafos", "grafos").build());
        assertThrows(IllegalArgumentException.class, () -> course().tags("x".repeat(33)).build());
    }

    @Test
    void tagsThatOnlyDifferInCaseCanBeBuiltSoTheRulesCanWarn() {
        // Para el schema son distintas; DUPLICATE_TAG (aviso, capa 3) las compara normalizadas.
        assertDoesNotThrow(() -> course().tags("grafos", "Grafos").build());
    }

    @Test
    void modulesAreBetween1And100() {
        List<Module> hundred = Collections.nCopies(100, module("m").build());
        List<Module> hundredOne = new ArrayList<>(hundred);
        hundredOne.add(module("m").build());

        assertDoesNotThrow(() -> course().modules(hundred).build());
        assertThrows(IllegalArgumentException.class, () -> course().modules(List.of()).build());
        assertThrows(IllegalArgumentException.class, () -> course().modules(hundredOne).build());
    }

    @Test
    void repeatedModuleIdsCanBeBuiltSoTheRulesCanReportThem() {
        // DUPLICATE_ID es una regla de la capa 3.
        assertDoesNotThrow(() -> course().modules(module("m").build(), module("m").build()).build());
    }

    @Test
    void requiresTheDefaultMastery() {
        assertThrows(NullPointerException.class, () -> new CourseDocument(1, "T", "D", "es", List.of("t"), null,
                List.of(module("m").build())));
    }
}
