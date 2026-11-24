package es.ulpgc.pamn.dominio.backend.web.progress;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.Optional;

/**
 * Lee el cuerpo de {@code POST /api/progress/sync}: bytes → {@link ParsedRequest}. Lector propio y estricto
 * en vez de {@code bodyAsClass}, porque Jackson por defecto es permisivo (campo ausente → {@code null},
 * número → texto). Usa un {@link ObjectMapper} propio, sin tocar el de Javalin. Inmutable y reutilizable.
 */
public final class ProgressRequestReader {

    private static final ParsedRequest MALFORMED = new ParsedRequest.Malformed();

    private final ObjectMapper mapper = JsonMapper.builder()
            // Dos claves iguales en un objeto: muchos analizadores se quedan con la última; aquí se rechaza.
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            // «{…} {}» no es un documento JSON: lo que sobra tras el cuerpo también es un error.
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public ParsedRequest read(byte[] body) {
        if (body == null) {
            return MALFORMED;
        }
        return StrictUtf8.decode(body)
                .flatMap(this::tree)
                .flatMap(SyncRequestJson::drafts)
                .<ParsedRequest>map(ParsedRequest.Parsed::new)
                .orElse(MALFORMED);
    }

    /** Texto → árbol. Vacío si no es JSON, si sobra texto o si salta un límite de Jackson (profundidad, tamaño). */
    private Optional<JsonNode> tree(String text) {
        try {
            JsonNode tree = mapper.readTree(text);
            return tree.isMissingNode() ? Optional.empty() : Optional.of(tree); // cuerpo vacío
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }
}
