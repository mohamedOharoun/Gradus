package es.ulpgc.pamn.dominio.backend.domain.course;

/**
 * El problema {@code PAYLOAD_TOO_LARGE} (§4.7). Lo producen tanto el lector como la web, cuando Javalin
 * rechaza el cuerpo antes de leerlo: así el mensaje es el mismo venga de donde venga.
 */
public final class SizeLimit {

    private static final String LIMIT = "2 MiB (" + CourseContent.MAX_SIZE_BYTES + " bytes)";

    private SizeLimit() {
    }

    public static Issue exceeded(long sizeBytes) {
        return issue("El curso ocupa " + sizeBytes + " bytes y el máximo es " + LIMIT + ".");
    }

    /** Cuando no se sabe cuánto ocupa el cuerpo, solo que supera el límite. */
    public static Issue exceeded() {
        return issue("El curso supera el máximo de " + LIMIT + ".");
    }

    private static Issue issue(String message) {
        return Issue.at(JsonPointer.root(), CourseIssueCode.PAYLOAD_TOO_LARGE, message);
    }
}
