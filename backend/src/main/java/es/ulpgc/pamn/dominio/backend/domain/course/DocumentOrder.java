package es.ulpgc.pamn.dominio.backend.domain.course;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Orden de documento de las rutas (decisión del paso 2): los índices de menor a mayor, un contenedor
 * antes que su contenido y las claves en el orden en que las declara el schema. Los records no saben
 * en qué orden escribió el autor las claves, así que se usa el del schema, que es el habitual.
 */
public final class DocumentOrder {

    /**
     * Un único orden de claves compatible con todos los objetos del schema: curso (formatVersion, title,
     * description, language, tags, defaults, modules), módulo (id, title, requires, mastery, steps,
     * review), ejercicio (type, id, prompt, options, answer, correct, pairs, explanation), lección
     * (type, id, text), opción (id, text), pareja (left, right) y prueba (questions, minCorrect).
     */
    private static final List<String> KEYS = List.of(
            "formatVersion", "type", "id", "title", "description", "language", "tags", "defaults", "requires",
            "mastery", "questions", "minCorrect", "modules", "steps", "review", "text", "prompt", "options",
            "answer", "correct", "pairs", "left", "right", "explanation");
    private static final Pattern INDEX = Pattern.compile("0|[1-9][0-9]*");

    public static final Comparator<JsonPointer> POINTERS = DocumentOrder::compare;
    public static final Comparator<Issue> ISSUES = Comparator.comparing(Issue::path, POINTERS);

    private DocumentOrder() {
    }

    private static int compare(JsonPointer first, JsonPointer second) {
        List<String> a = first.tokens();
        List<String> b = second.tokens();
        return IntStream.range(0, Math.min(a.size(), b.size()))
                .map(i -> compareTokens(a.get(i), b.get(i)))
                .filter(result -> result != 0)
                .findFirst()
                .orElse(Integer.compare(a.size(), b.size()));
    }

    private static int compareTokens(String a, String b) {
        if (isIndex(a) && isIndex(b)) {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        }
        return Comparator.comparingInt(DocumentOrder::rank).thenComparing(Comparator.naturalOrder()).compare(a, b);
    }

    /** Posición de la clave en el schema; las desconocidas, al final. */
    private static int rank(String key) {
        int position = KEYS.indexOf(key);
        return position >= 0 ? position : KEYS.size();
    }

    private static boolean isIndex(String token) {
        return INDEX.matcher(token).matches();
    }
}
