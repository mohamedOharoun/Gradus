package es.ulpgc.pamn.dominio.backend.domain.progress;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fusiona el progreso guardado con el entrante (reglas 1, 2, 3 y 9 de {@code api-progreso.md}):
 * unión, gana la fecha más antigua y se quitan los repetidos. Función pura: no modifica sus entradas.
 * El orden del resultado es el de primera aparición (primero lo guardado, luego lo nuevo).
 */
public final class ProgressMerge {

    private ProgressMerge() {
    }

    public static List<Enrollment> merge(List<Enrollment> stored, List<Enrollment> incoming) {
        return Stream.concat(stored.stream(), incoming.stream())
                .map(ProgressMerge::withoutDuplicateModules)
                .collect(Collectors.toMap(Enrollment::courseId, Function.identity(),
                        ProgressMerge::combine, LinkedHashMap::new))
                .values().stream().toList();
    }

    private static Enrollment combine(Enrollment a, Enrollment b) {
        return new Enrollment(a.courseId(), oldest(a.enrolledAt(), b.enrolledAt()),
                uniqueModules(Stream.concat(a.mastered().stream(), b.mastered().stream())));
    }

    private static Enrollment withoutDuplicateModules(Enrollment enrollment) {
        return new Enrollment(enrollment.courseId(), enrollment.enrolledAt(),
                uniqueModules(enrollment.mastered().stream()));
    }

    private static List<MasteredModule> uniqueModules(Stream<MasteredModule> modules) {
        return modules
                .collect(Collectors.toMap(MasteredModule::moduleId, Function.identity(),
                        (a, b) -> new MasteredModule(a.moduleId(), oldest(a.masteredAt(), b.masteredAt())),
                        LinkedHashMap::new))
                .values().stream().toList();
    }

    private static Instant oldest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
