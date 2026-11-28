package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReadResult;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * §6: un fixture por cada código de §3.3, como mutación mínima del ejemplo (decisión: JSON Patch).
 * Cada fixture se aplica al ejemplo, se pasa por el lector y por las reglas, y debe producir
 * <b>exactamente</b> su código en su ruta.
 */
class CourseFixturesTest {

    static final String FOLDER = "/courses/fixtures";
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final CourseValidator VALIDATOR = CourseValidator.standard();

    /** Lo que esperamos de un fixture y la mutación que lo produce. */
    record Fixture(String name, String description, CourseIssueCode code, String path, JsonNode patch) {

        /** Así se llama cada caso en el informe de los tests. */
        @Override
        public String toString() {
            return name;
        }
    }

    /** Las capas 1–5, como hará el caso de uso del paso 4: lector y, si lee bien, reglas. */
    static CourseReport report(byte[] body) {
        return switch (JacksonCourseReaderTest.READER.read(body)) {
            case CourseReadResult.Parsed parsed -> VALIDATOR.validate(parsed.course());
            case CourseReadResult.TooLarge tooLarge -> tooLarge.report();
            case CourseReadResult.Unreadable unreadable -> unreadable.report();
            case CourseReadResult.Invalid invalid -> invalid.report();
        };
    }

    static List<String> found(CourseReport report) {
        return report.issues().stream().map(issue -> issue.code() + " " + issue.path()).toList();
    }

    static Stream<Fixture> fixtures() {
        try (Stream<Path> files = Files.list(Path.of(CourseFixturesTest.class.getResource(FOLDER).toURI()))) {
            return files.filter(file -> file.toString().endsWith(".json")).sorted().map(CourseFixturesTest::load)
                    .toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static Fixture load(Path file) {
        try {
            JsonNode fixture = MAPPER.readTree(file.toFile());
            return new Fixture(file.getFileName().toString(), fixture.get("description").asText(),
                    CourseIssueCode.valueOf(fixture.get("expected").get("code").asText()),
                    fixture.get("expected").get("path").asText(), fixture.get("patch"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void eachFixtureProducesExactlyItsCodeAtItsPath(Fixture fixture) {
        JsonNode mutated = JsonPatch.apply(ExampleCourse.tree(), fixture.patch());

        CourseReport report = report(mutated.toString().getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of(fixture.code() + " " + fixture.path()), found(report), fixture.description());
    }

    @Test
    void invalidJsonFromTheExample() {
        // La capa 1 no se puede escribir como parche: un árbol JSON siempre es JSON válido.
        String broken = ExampleCourse.text().replaceFirst("\"title\":", "\"title\"");

        assertEquals(List.of("INVALID_JSON "), found(report(broken.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void invalidUtf8FromTheExample() {
        byte[] body = ExampleCourse.bytes();
        body[10] = (byte) 0xFF;

        assertEquals(List.of("INVALID_JSON "), found(report(body)));
    }

    @Test
    void duplicateKeyFromTheExample() {
        // Ni un árbol JSON puede tener claves repetidas: se escribe en el texto.
        String repeated = ExampleCourse.text().replaceFirst("\"answer\": \"a\",", "\"answer\": \"a\", \"answer\": \"b\",");

        assertEquals(List.of("DUPLICATE_KEY /modules/0/steps/2"),
                found(report(repeated.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void theExampleItselfHasNoIssues() {
        assertEquals(List.of(), found(report(ExampleCourse.bytes())));
    }

    @Test
    void everyCodeOfTheCatalogHasAFixture() {
        // Fuera del validador quedan PAYLOAD_TOO_LARGE (§4.7) y REMOVED_IDS (§4.6), con sus propios tests.
        Set<CourseIssueCode> catalog = EnumSet.allOf(CourseIssueCode.class);
        catalog.removeAll(Set.of(CourseIssueCode.PAYLOAD_TOO_LARGE, CourseIssueCode.REMOVED_IDS));
        Set<CourseIssueCode> covered = fixtures().map(Fixture::code).collect(Collectors.toCollection(
                () -> EnumSet.noneOf(CourseIssueCode.class)));
        covered.addAll(Set.of(CourseIssueCode.INVALID_JSON, CourseIssueCode.DUPLICATE_KEY));

        assertEquals(catalog, covered, "Faltan fixtures para: " + Arrays.stream(CourseIssueCode.values())
                .filter(code -> catalog.contains(code) && !covered.contains(code)).toList());
    }

    @Test
    void everyFixtureIsNamedAfterItsCode() {
        fixtures().forEach(fixture -> assertEquals(fixture.code() + ".json", fixture.name()));
    }
}
