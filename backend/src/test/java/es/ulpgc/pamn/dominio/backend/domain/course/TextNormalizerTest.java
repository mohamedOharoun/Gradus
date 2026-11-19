package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextNormalizerTest {

    @Test
    void trimsCollapsesWhitespaceAndLowercases() {
        assertEquals("matriz de adyacencia", TextNormalizer.normalize("  Matriz   de ADYACENCIA "));
    }

    @Test
    void treatsComposedAndDecomposedAccentsAsEqual() {
        String composed = "café";       // «é» precompuesta
        String decomposed = "café";    // «e» + acento combinante U+0301
        assertNotEquals(composed, decomposed);

        assertEquals(TextNormalizer.normalize(composed), TextNormalizer.normalize(decomposed));
    }

    @Test
    void treatsTabsNewlinesAndNoBreakSpacesAsWhitespace() {
        assertEquals("lista de aristas", TextNormalizer.normalize(" Lista\t\nde   aristas "));
    }

    @Test
    void lowercasesTheSameWayWhateverTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            // En turco, «I» en minúscula es «ı» (sin punto); con Locale.ROOT siempre es «i».
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals("bfs inicial", TextNormalizer.normalize("BFS INICIAL"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void blankTextBecomesEmpty() {
        assertEquals("", TextNormalizer.normalize(" \t\n "));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> TextNormalizer.normalize(null));
    }
}
