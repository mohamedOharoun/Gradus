package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import es.ulpgc.pamn.dominio.backend.domain.course.PageRequest;
import es.ulpgc.pamn.dominio.backend.domain.course.PublishedWithin;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Caso de uso: buscar entre las últimas versiones de los cursos (§4.5). Los filtros se normalizan en
 * {@link CourseSearch}; solo {@code page}, {@code size} y {@code published} pueden ser inválidos, y si
 * lo son varios se informan todos, en ese orden.
 */
public final class SearchCourses {

    /** Solo dígitos, y no tantos que desborden un {@code int}: «+1», «2.5» o «-1» no son válidos. */
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,9}");
    private static final String INVALID_PUBLISHED = "Debe ser «month» o «year».";

    private final CourseRepository repository;
    private final Clock clock;

    public SearchCourses(CourseRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SearchResult search(SearchCoursesQuery query) {
        return Result.combine(pageRequest(query), publishedWithin(query.published()),
                        (page, window) -> repository.search(criteria(query, window), page))
                .fold(SearchResult.Rejected::new, SearchResult.Found::new);
    }

    /**
     * Los filtros ya normalizados. El reloj se lee aquí, en cada búsqueda: «último mes» se mide desde
     * ahora, no desde que se creó el caso de uso.
     */
    private CourseSearch criteria(SearchCoursesQuery query, Optional<PublishedWithin> window) {
        CourseSearch search = CourseSearch.of(query.text(), query.tags(), query.language())
                .withAuthor(query.author());
        return window.map(within -> search.publishedSince(within.since(clock.instant()))).orElse(search);
    }

    private static Result<PageRequest> pageRequest(SearchCoursesQuery query) {
        return Result.combine(
                wholeNumber("page", query.page(), PageRequest.FIRST_PAGE, PageRequest.FIRST_PAGE,
                        Integer.MAX_VALUE, "Debe ser un número entero desde " + PageRequest.FIRST_PAGE + "."),
                wholeNumber("size", query.size(), PageRequest.DEFAULT_SIZE, PageRequest.MIN_SIZE,
                        PageRequest.MAX_SIZE, "Debe ser un número entero entre " + PageRequest.MIN_SIZE
                                + " y " + PageRequest.MAX_SIZE + "."),
                PageRequest::new);
    }

    /**
     * {@code published}: si falta o está en blanco, cualquier fecha (vacío). Hay que mirarlo antes de
     * {@link PublishedWithin#fromParameter}, que da vacío también para un valor inválido.
     */
    private static Result<Optional<PublishedWithin>> publishedWithin(String raw) {
        if (raw == null || raw.isBlank()) {
            return Result.success(Optional.empty());
        }
        Optional<PublishedWithin> within = PublishedWithin.fromParameter(raw);
        return within.isPresent() ? Result.success(within) : invalid("published", INVALID_PUBLISHED);
    }

    /** Un parámetro entero opcional: si falta o está en blanco, vale {@code fallback}. */
    private static Result<Integer> wholeNumber(String field, String raw, int fallback, int min, int max,
                                               String message) {
        if (raw == null || raw.isBlank()) {
            return Result.success(fallback);
        }
        String value = raw.strip();
        if (WHOLE_NUMBER.matcher(value).matches()) {
            int number = Integer.parseInt(value);
            if (number >= min && number <= max) {
                return Result.success(number);
            }
        }
        return invalid(field, message);
    }

    private static <T> Result<T> invalid(String field, String message) {
        return Result.failure(List.of(new ValidationError(field, ErrorCode.INVALID_PARAMETER, message)));
    }
}
