package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;
import es.ulpgc.pamn.dominio.backend.domain.course.SizeLimit;

/**
 * Capa 1 (§3.2): bytes → árbol JSON. Usa un {@link ObjectMapper} propio, sin tocar el que Javalin usa
 * para el resto de la API (§3.1). Inmutable y reutilizable entre peticiones.
 */
final class CourseJson {

    /** Jackson 2 no tiene una excepción propia para las claves repetidas: se reconoce por su mensaje. */
    private static final String DUPLICATE_FIELD = "Duplicate field";

    private final ObjectMapper mapper = JsonMapper.builder()
            // Muchos analizadores se quedan en silencio con la última clave repetida; aquí se rechaza.
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            // «{…} {}» no es un documento JSON: lo que sigue al curso también es un error.
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    SyntaxCheck parse(byte[] body) {
        byte[] withoutBom = Utf8Text.withoutBom(body);
        if (withoutBom.length > CourseContent.MAX_SIZE_BYTES) {
            return new SyntaxCheck.Failed(SizeLimit.exceeded(withoutBom.length));
        }
        return switch (Utf8Text.decode(body)) {
            case Utf8Text.Malformed malformed -> failed(CourseIssueCode.INVALID_JSON, JsonPointer.root(),
                    "El cuerpo no es UTF-8 válido: hay una secuencia de bytes incorrecta en el byte "
                            + malformed.position() + ". Guarda el fichero con la codificación UTF-8.");
            case Utf8Text.Decoded decoded -> parseText(decoded.text());
        };
    }

    private SyntaxCheck parseText(String text) {
        try {
            JsonNode tree = mapper.readTree(text);
            if (tree.isMissingNode()) {
                return failed(CourseIssueCode.INVALID_JSON, JsonPointer.root(),
                        "El cuerpo está vacío: se esperaba un objeto JSON con el curso.");
            }
            return new SyntaxCheck.Passed(new CourseContent(text), tree);
        } catch (JacksonException e) {
            return isDuplicateKey(e) ? duplicateKey(e) : invalidJson(e);
        }
    }

    private static boolean isDuplicateKey(JacksonException e) {
        return e instanceof JsonParseException && e.getOriginalMessage().startsWith(DUPLICATE_FIELD);
    }

    /** La ruta es la del objeto que repite la clave: la del analizador sin su último elemento. */
    private static SyntaxCheck duplicateKey(JacksonException e) {
        JsonStreamContext context = ((JsonParser) e.getProcessor()).getParsingContext();
        com.fasterxml.jackson.core.JsonPointer objectPath = context.pathAsPointer().head();
        return failed(CourseIssueCode.DUPLICATE_KEY, toPointer(objectPath),
                "La clave «" + context.getCurrentName() + "» está repetida en el mismo objeto" + where(e)
                        + ". Muchos programas se quedarían en silencio con la última; deja solo una.");
    }

    private static SyntaxCheck invalidJson(JacksonException e) {
        return failed(CourseIssueCode.INVALID_JSON, JsonPointer.root(),
                "No es JSON válido" + where(e) + ". Detalle del analizador: " + e.getOriginalMessage());
    }

    private static String where(JacksonException e) {
        JsonLocation location = e.getLocation();
        return location == null ? ""
                : " (línea " + location.getLineNr() + ", columna " + location.getColumnNr() + ")";
    }

    /** De la ruta de Jackson a la nuestra: los segmentos numéricos son índices de listas. */
    private static JsonPointer toPointer(com.fasterxml.jackson.core.JsonPointer jackson) {
        JsonPointer pointer = JsonPointer.root();
        for (var rest = jackson; rest != null && !rest.matches(); rest = rest.tail()) {
            pointer = rest.getMatchingIndex() >= 0
                    ? pointer.index(rest.getMatchingIndex())
                    : pointer.key(rest.getMatchingProperty());
        }
        return pointer;
    }

    private static SyntaxCheck failed(CourseIssueCode code, JsonPointer path, String message) {
        return new SyntaxCheck.Failed(Issue.at(path, code, message));
    }
}
