package es.ulpgc.pamn.dominio.backend.domain.course;

/** Qué página de resultados se pide (§4.5): desde 0, con entre 1 y 100 elementos. */
public record PageRequest(int page, int size) {

    public static final int FIRST_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < FIRST_PAGE) {
            throw new IllegalArgumentException("Las páginas empiezan en " + FIRST_PAGE + ": " + page);
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre " + MIN_SIZE + " y "
                    + MAX_SIZE + ": " + size);
        }
    }

    /** Factory method: la primera página con el tamaño por defecto. */
    public static PageRequest first() {
        return new PageRequest(FIRST_PAGE, DEFAULT_SIZE);
    }

    /** Cuántos resultados se saltan las páginas anteriores. {@code long} para no desbordarse nunca. */
    public long offset() {
        return (long) page * size;
    }
}
