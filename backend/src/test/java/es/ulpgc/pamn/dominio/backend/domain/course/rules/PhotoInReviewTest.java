package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Exercise;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.Severity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.REVIEW_SIZE;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.course;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.lesson;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.photo;
import static es.ulpgc.pamn.dominio.backend.domain.course.TestCourses.reviewPool;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.codes;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.paths;
import static es.ulpgc.pamn.dominio.backend.domain.course.rules.Issues.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code PHOTO_IN_REVIEW} (docs/tareas/ejercicio-foto.md, decisión D y tarea 2). */
class PhotoInReviewTest {

    /** La reserva normal del módulo {@code bfs} (índice 1) con fotos en las posiciones indicadas. */
    static CourseDocument withPhotosInReview(int... positions) {
        List<Exercise> review = new ArrayList<>(reviewPool("bfs", REVIEW_SIZE));
        for (int position : positions) {
            review.set(position, photo("bfs-foto-" + position));
        }
        return course().module(1, m -> m.review(review)).build();
    }

    @Test
    void theDefaultCourseHasNoPhotoInReview() {
        assertEquals(List.of(), ExerciseRules.photosInReview(course().build()).toList());
    }

    @Test
    void aPhotoInReviewIsAnErrorOnItsOwnPath() {
        Issue issue = single(ExerciseRules.photosInReview(withPhotosInReview(2)));

        assertEquals(CourseIssueCode.PHOTO_IN_REVIEW, issue.code());
        assertEquals(Severity.ERROR, issue.code().severity());
        assertEquals("/modules/1/review/2", issue.path().toString());
    }

    @Test
    void theMessageNamesTheExerciseAndSaysWhereItCanGo() {
        String message = single(ExerciseRules.photosInReview(withPhotosInReview(2))).message();

        assertTrue(message.contains("«bfs-foto-2»"), message);
        assertTrue(message.contains("steps"), message);
    }

    @Test
    void aPhotoInStepsIsFine() {
        CourseDocument course = course().module(0, m -> m.steps(lesson("representacion-l1"), photo("repr-foto"))).build();

        assertEquals(List.of(), ExerciseRules.photosInReview(course).toList());
    }

    @Test
    void everyPhotoInReviewIsReportedInDocumentOrder() {
        assertEquals(List.of("/modules/1/review/0", "/modules/1/review/5"),
                paths(ExerciseRules.photosInReview(withPhotosInReview(5, 0))));
    }

    @Test
    void theStandardValidatorRejectsACourseWithAPhotoInReview() {
        // Es lo que hace que al publicar se responda 422.
        assertFalse(CourseValidator.standard().validate(withPhotosInReview(2)).isValid());
    }

    @Test
    void theErrorStopsBeforeTheMasteryRules() {
        // La regla está en la capa 3, que para antes de la 5: así no sale además un MASTERY_GUESSABLE
        // (una foto cuenta como acierto seguro) que solo despistaría al autor.
        assertEquals(List.of(CourseIssueCode.PHOTO_IN_REVIEW),
                codes(CourseValidator.standard().validate(withPhotosInReview(2)).issues().stream()));
    }
}
