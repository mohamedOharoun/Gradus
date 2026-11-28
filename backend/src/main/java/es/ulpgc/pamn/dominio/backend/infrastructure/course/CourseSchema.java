package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import com.networknt.schema.regex.JDKRegularExpressionFactory;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Capa 2 (§3.2): el schema del formato v1, compilado una sola vez. Adapter entre la librería de JSON
 * Schema y el dominio: cada violación se convierte en un {@code SCHEMA_VIOLATION} con su ruta.
 * Inmutable y reutilizable entre peticiones.
 */
public final class CourseSchema {

    /** El mismo fichero que validan los tests: así schema y ejemplo no se desincronizan (§6). */
    public static final String RESOURCE = "/schemas/course-v1.schema.json";

    /**
     * Los patrones del schema son expresiones regulares que un autor no tiene por qué leer: se sustituyen
     * por una explicación. La clave es el lugar del schema donde está el patrón.
     */
    private static final Map<String, String> PATTERN_EXPLANATIONS = Map.of(
            "/$defs/id/pattern",
            "debe ser un id: letras minúsculas sin tildes, dígitos y guiones sueltos (por ejemplo «bfs-r1»)",
            "/$defs/plainText/pattern",
            "debe ser texto de una sola línea, sin espacios ni saltos de línea al principio o al final",
            "/$defs/markdown/pattern",
            "no puede estar en blanco",
            "/properties/language/pattern",
            "debe ser un código de idioma ISO 639-1, opcionalmente con región: es, en, es-ES");

    private final Schema schema;

    private CourseSchema(Schema schema) {
        this.schema = schema;
    }

    /** Factory method: carga y compila el schema del classpath. Un fallo aquí es un error de despliegue. */
    public static CourseSchema load() {
        SchemaRegistryConfig config = SchemaRegistryConfig.builder()
                .locale(Locale.forLanguageTag("es"))
                .pathType(PathType.JSON_POINTER)
                // Soporta el lookahead (?![\s\S]) de los patrones y su \S es el mismo que el de Require.
                .regularExpressionFactory(JDKRegularExpressionFactory.getInstance())
                .build();
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
                builder -> builder.schemaRegistryConfig(config));
        try (InputStream in = CourseSchema.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encuentra el schema de cursos en el classpath: " + RESOURCE);
            }
            return new CourseSchema(registry.getSchema(in));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Una entrada {@code SCHEMA_VIOLATION} por violación; lista vacía si el documento cumple el schema. */
    public List<Issue> check(JsonNode document) {
        return schema.validate(document).stream().map(CourseSchema::toIssue).toList();
    }

    private static Issue toIssue(Error error) {
        JsonPointer path = toPointer(error.getInstanceLocation());
        String where = path.toString().isEmpty() ? "En la raíz del curso" : "En " + path;
        return Issue.at(path, CourseIssueCode.SCHEMA_VIOLATION, where + ": " + explain(error) + ".");
    }

    private static String explain(Error error) {
        String schemaPlace = error.getSchemaLocation().getFragment().toString();
        return Optional.ofNullable(PATTERN_EXPLANATIONS.get(schemaPlace))
                .map(explanation -> explanation + "; se encontró «" + error.getInstanceNode().asText() + "»")
                .orElseGet(error::getMessage);
    }

    /** Los elementos de la ruta de la librería son claves ({@code String}) o índices ({@code Integer}). */
    private static JsonPointer toPointer(NodePath location) {
        JsonPointer pointer = JsonPointer.root();
        for (int i = 0; i < location.getNameCount(); i++) {
            Object element = location.getElement(i);
            pointer = element instanceof Integer index ? pointer.index(index) : pointer.key(element.toString());
        }
        return pointer;
    }
}
