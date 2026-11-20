package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector falso con varios textos conocidos: cada texto se «lee» como el curso que se le asigne. Lo que
 * no conoce es JSON inválido. Así los tests de publicación tienen contenidos distintos (y SHA-256
 * distintos) para cada versión, sin depender de Jackson.
 */
final class FakeCourseReader implements CourseReader {

    static final CourseReport UNREADABLE =
            new CourseReport(List.of(Issue.at(JsonPointer.root(), CourseIssueCode.INVALID_JSON, "No es JSON.")));

    private final Map<String, CourseReadResult> results = new HashMap<>();

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    FakeCourseReader parsing(String text, CourseDocument course) {
        return answering(text, new CourseReadResult.Parsed(course, new CourseContent(text)));
    }

    FakeCourseReader answering(String text, CourseReadResult result) {
        results.put(text, result);
        return this;
    }

    @Override
    public CourseReadResult read(byte[] body) {
        return results.getOrDefault(new String(body, StandardCharsets.UTF_8),
                new CourseReadResult.Unreadable(UNREADABLE));
    }
}
