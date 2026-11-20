package es.ulpgc.pamn.dominio.backend.application.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseSearch;
import es.ulpgc.pamn.dominio.backend.domain.course.PageRequest;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.Result;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Caso de uso: buscar entre las últimas versiones de los cursos (§4.5). Los filtros se normalizan en
 * {@link CourseSearch}; solo {@code page} y {@code size} pueden ser inválidos (decisión del paso 4).
 */
public final class SearchCourses {

    /** Solo dígitos, y no tantos que desborden un {@code int}: «+1», «2.5» o «-1» no son válidos. */
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,9}");

    private final CourseRepository repository;

    public SearchCourses(CourseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public SearchResult search(SearchCoursesQuery query) {
        return Result.combine(
                        wholeNumber("page", query.page(), PageRequest.FIRST_PAGE, PageRequest.FIRST_PAGE,
                                Integer.MAX_VALUE, "Debe ser un número entero desde " + PageRequest.FIRST_PAGE + "."),
                        wholeNumber("size", query.size(), PageRequest.DEFAULT_SIZE, PageRequest.MIN_SIZE,
                                PageRequest.MAX_SIZE, "Debe ser un número entero entre " + PageRequest.MIN_SIZE
                                        + " y " + PageRequest.MAX_SIZE + "."),
                        PageRequest::new)
                .fold(SearchResult.Rejected::new, page -> new SearchResult.Found(repository.search(
                        CourseSearch.of(query.text(), query.tags(), query.language()), page)));
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
        return Result.failure(List.of(new ValidationError(field, ErrorCode.INVALID_PARAMETER, message)));
    }
}
