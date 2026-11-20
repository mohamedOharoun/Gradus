package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static es.ulpgc.pamn.dominio.backend.domain.course.PublishedCourses.CONTENT;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseSearchTest {

    /** «Introducción a los grafos», etiquetas grafos y algoritmos, en español. */
    static final CourseMetadata GRAPHS = CourseMetadata.of(course().build(), CONTENT);

    @Test
    void textAndTagsAreNormalizedAndBlanksAreIgnored() {
        CourseSearch search = CourseSearch.of("  Los   GRAFOS ", List.of("Algoritmos", " ", "grafos", "GRAFOS"), "");

        assertEquals(Optional.of("los grafos"), search.text());
        assertEquals(List.of("algoritmos", "grafos"), search.tags());
        assertEquals(Optional.empty(), search.language());
    }

    @Test
    void missingParametersMeanNoFilter() {
        CourseSearch search = CourseSearch.of(null, null, null);

        assertEquals(CourseSearch.ANYTHING, search);
        assertTrue(search.matches(GRAPHS));
    }

    @Test
    void theTextIsASubstringOfTitleOrDescription() {
        assertTrue(CourseSearch.of("GRAFOS", List.of(), null).matches(GRAPHS));
        assertTrue(CourseSearch.of("en anchura", List.of(), null).matches(GRAPHS));
        assertFalse(CourseSearch.of("dijkstra", List.of(), null).matches(GRAPHS));
    }

    @Test
    void everyTagMustBePresent() {
        assertTrue(CourseSearch.of(null, List.of("Grafos", "algoritmos"), null).matches(GRAPHS));
        assertFalse(CourseSearch.of(null, List.of("grafos", "redes"), null).matches(GRAPHS));
    }

    @Test
    void aTagMustMatchWholeNotAsASubstring() {
        assertFalse(CourseSearch.of(null, List.of("graf"), null).matches(GRAPHS));
    }

    @Test
    void theLanguageMustMatchExactly() {
        assertTrue(CourseSearch.of(null, null, "es").matches(GRAPHS));
        assertFalse(CourseSearch.of(null, null, "es-ES").matches(GRAPHS));
    }

    @Test
    void wildcardsInTheTextAreLiteral() {
        // §4.5: buscar «100%» no puede convertirse en un comodín.
        CourseMetadata percent = CourseMetadata.of(course().title("Grafos al 100% de capacidad").build(), CONTENT);

        assertTrue(CourseSearch.of("100%", null, null).matches(percent));
        assertFalse(CourseSearch.of("100%", null, null).matches(GRAPHS));
        assertFalse(CourseSearch.of("gra_os", null, null).matches(GRAPHS));
    }

    @Test
    void theTagListIsImmutable() {
        CourseSearch search = CourseSearch.of(null, Arrays.asList("grafos"), null);

        assertEquals(List.of("grafos"), search.tags());
        assertThrows(UnsupportedOperationException.class, () -> search.tags().add("x"));
    }
}
