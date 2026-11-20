package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.TextNormalizer;

import java.util.stream.Stream;

/** Reglas de la capa 3 sobre identidad: ids únicos en todo el curso y etiquetas distintas. */
public final class IdentityRules {

    private IdentityRules() {
    }

    /** {@code DUPLICATE_ID}: módulos, lecciones y ejercicios comparten un único espacio de nombres (§2.4). */
    public static Stream<Issue> duplicateIds(CourseDocument course) {
        return Duplicates.by(Locations.ids(course).toList(), id -> id)
                .map(repeat -> Issue.at(repeat.repeated().path(), CourseIssueCode.DUPLICATE_ID,
                        "El id «" + repeat.repeated().value() + "» está repetido: ya se usa en "
                                + repeat.first().path() + ". Los ids de módulos, lecciones y ejercicios "
                                + "deben ser únicos en todo el curso."));
    }

    /** {@code DUPLICATE_TAG} (aviso): dos etiquetas que coinciden al normalizarlas (§3.4). */
    public static Stream<Issue> duplicateTags(CourseDocument course) {
        return Duplicates.by(Locations.indexed(course.tags(), Locations.TAGS), TextNormalizer::normalize)
                .map(repeat -> Issue.at(repeat.repeated().path(), CourseIssueCode.DUPLICATE_TAG,
                        "La etiqueta «" + repeat.repeated().value() + "» es la misma que «"
                                + repeat.first().value() + "» (" + repeat.first().path() + ") si se ignoran "
                                + "mayúsculas y espacios. Quita una de las dos."));
    }
}
