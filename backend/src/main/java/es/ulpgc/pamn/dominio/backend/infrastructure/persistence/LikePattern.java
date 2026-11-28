package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

/**
 * Patrones para {@code LIKE ? ESCAPE '\'} (§4.5). En {@code LIKE}, «%» y «_» son comodines: sin escaparlos,
 * buscar «100%» encontraría «100 grafos». Función pura.
 */
final class LikePattern {

    private static final String ESCAPE = "\\";

    private LikePattern() {
    }

    /** Patrón que encuentra {@code text} en cualquier posición, tomando sus caracteres literalmente. */
    static String containing(String text) {
        return "%" + escape(text) + "%";
    }

    /** Primero el propio «\», para no duplicar el que se pone después delante de «%» y «_». */
    static String escape(String text) {
        return text.replace(ESCAPE, ESCAPE + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }
}
