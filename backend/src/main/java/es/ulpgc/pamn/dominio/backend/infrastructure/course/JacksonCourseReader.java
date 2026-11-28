package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.JsonNode;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReadResult;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReader;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter del puerto {@link CourseReader} con Jackson y el JSON Schema: capa 1 ({@link CourseJson}),
 * capa 2 (versión del formato y {@link CourseSchema}) y mapeo a los records ({@link CourseNodeMapper}).
 * Inmutable y reutilizable entre peticiones.
 */
public final class JacksonCourseReader implements CourseReader {

    private static final JsonPointer FORMAT_VERSION = JsonPointer.root().key("formatVersion");
    private static final BigDecimal SUPPORTED_VERSION = BigDecimal.valueOf(CourseDocument.FORMAT_VERSION);

    private final CourseJson json = new CourseJson();
    private final CourseSchema schema;

    public JacksonCourseReader(CourseSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
    }

    @Override
    public CourseReadResult read(byte[] body) {
        return switch (json.parse(body)) {
            case SyntaxCheck.Failed failed -> failed.issue().code() == CourseIssueCode.PAYLOAD_TOO_LARGE
                    ? new CourseReadResult.TooLarge(reportOf(List.of(failed.issue())))
                    : new CourseReadResult.Unreadable(reportOf(List.of(failed.issue())));
            case SyntaxCheck.Passed passed -> readStructure(passed);
        };
    }

    /** Capa 2: primero la versión del formato, para dar un mensaje claro; si es soportada, el schema. */
    private CourseReadResult readStructure(SyntaxCheck.Passed passed) {
        List<Issue> issues = unsupportedVersion(passed.tree())
                .map(List::of)
                .orElseGet(() -> schema.check(passed.tree()));
        if (!issues.isEmpty()) {
            return new CourseReadResult.Invalid(reportOf(issues));
        }
        try {
            return new CourseReadResult.Parsed(CourseNodeMapper.course(passed.tree()), passed.content());
        } catch (IllegalArgumentException e) {
            // El schema y los constructores del dominio repiten las mismas reglas: si no coinciden, es un fallo nuestro.
            throw new IllegalStateException("El curso cumple el schema pero no los records del dominio", e);
        }
    }

    /** §3.3: solo si {@code formatVersion} existe, es un entero y no es 1. Si no, decide el schema. */
    private static Optional<Issue> unsupportedVersion(JsonNode tree) {
        JsonNode version = tree.path("formatVersion");
        if (!version.isNumber() || !version.canConvertToExactIntegral()
                || version.decimalValue().compareTo(SUPPORTED_VERSION) == 0) {
            return Optional.empty();
        }
        return Optional.of(Issue.at(FORMAT_VERSION, CourseIssueCode.UNSUPPORTED_FORMAT_VERSION,
                "La versión del formato es " + version.decimalValue().stripTrailingZeros().toPlainString()
                        + ", pero este servidor solo admite formatVersion " + CourseDocument.FORMAT_VERSION
                        + ". Es la versión del formato del fichero, no la de tu curso: la versión del curso la "
                        + "asigna el servidor al publicar."));
    }

    private static CourseReport reportOf(List<Issue> issues) {
        return new CourseReport(issues);
    }
}
