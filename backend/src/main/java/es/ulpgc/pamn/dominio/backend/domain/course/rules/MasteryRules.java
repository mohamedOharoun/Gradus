package es.ulpgc.pamn.dominio.backend.domain.course.rules;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseDocument;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import es.ulpgc.pamn.dominio.backend.domain.course.JsonPointer;
import es.ulpgc.pamn.dominio.backend.domain.course.Mastery;
import es.ulpgc.pamn.dominio.backend.domain.course.Module;

import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reglas de la capa 5 sobre las pruebas de dominio (§3.3). Las del objeto {@code mastery} se informan
 * una vez por objeto; las de la reserva, una vez por módulo. Opción A (ver la tarea): si la prueba
 * efectiva de un módulo tiene un error, se omiten sus avisos de dominio.
 */
public final class MasteryRules {

    /** Por encima de esta probabilidad de aprobar adivinando, se avisa. */
    public static final double GUESSING_LIMIT = 0.05;

    private static final JsonPointer DEFAULT_MASTERY = JsonPointer.root().key("defaults").key("mastery");
    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private MasteryRules() {
    }

    /** {@code MASTERY_MIN_EXCEEDS_QUESTIONS}: pide más aciertos que preguntas. */
    public static Stream<Issue> minExceedsQuestions(CourseDocument course) {
        return masteryObjects(course)
                .filter(object -> object.mastery().minCorrect() > object.mastery().questions())
                .map(object -> Issue.at(object.path(), CourseIssueCode.MASTERY_MIN_EXCEEDS_QUESTIONS,
                        object.owner() + " pide " + object.mastery().minCorrect() + " aciertos de solo "
                                + object.mastery().questions() + " preguntas: minCorrect no puede ser mayor "
                                + "que questions."));
    }

    /** {@code MASTERY_REQUIRES_PERFECT} (aviso): un solo despiste suspende. */
    public static Stream<Issue> requiresPerfect(CourseDocument course) {
        return masteryObjects(course)
                .filter(object -> object.mastery().minCorrect() == object.mastery().questions())
                .map(object -> Issue.at(object.path(), CourseIssueCode.MASTERY_REQUIRES_PERFECT,
                        object.owner() + " pide acertar todas sus preguntas (" + object.mastery().minCorrect()
                                + " de " + object.mastery().questions() + "): un solo despiste suspende. "
                                + "Considera bajar minCorrect o subir questions."));
    }

    /** {@code MASTERY_POOL_TOO_SMALL}: la reserva no llega para sortear la prueba ni una vez. */
    public static Stream<Issue> poolTooSmall(CourseDocument course) {
        return moduleMasteries(course)
                .filter(ModuleMastery::poolTooSmall)
                .map(test -> Issue.at(test.reviewPath(), CourseIssueCode.MASTERY_POOL_TOO_SMALL,
                        "El módulo «" + test.id() + "» sortea " + test.questions() + " preguntas para su prueba "
                                + "de dominio, pero su reserva (review) solo tiene " + test.poolSize()
                                + " ejercicios. Añade al menos " + (test.questions() - test.poolSize())
                                + " o baja questions."));
    }

    /** {@code MASTERY_POOL_SMALL_FOR_RETRY} (aviso): menos de 2 · questions ejercicios en la reserva. */
    public static Stream<Issue> poolSmallForRetry(CourseDocument course) {
        return moduleMasteries(course)
                .filter(test -> !test.hasError() && test.poolSize() < 2 * test.questions())
                .map(test -> Issue.at(test.reviewPath(), CourseIssueCode.MASTERY_POOL_SMALL_FOR_RETRY,
                        "El módulo «" + test.id() + "» tiene " + test.poolSize() + " ejercicios en review y su "
                                + "prueba sortea " + test.questions() + ": un reintento repetirá preguntas. "
                                + "Con " + 2 * test.questions() + " o más no se repetirían."));
    }

    /** {@code MASTERY_GUESSABLE} (aviso): la cota de aprobar adivinando supera el 5 % (§3.6). */
    public static Stream<Issue> guessable(CourseDocument course) {
        return moduleMasteries(course)
                .filter(test -> !test.hasError())
                .flatMap(MasteryRules::guessingIssue);
    }

    private static Stream<Issue> guessingIssue(ModuleMastery test) {
        double p = GuessingOdds.maxGuessProbability(test.module().value().review());
        double bound = GuessingOdds.passBound(test.questions(), test.minCorrect(), p);
        if (bound <= GUESSING_LIMIT) {
            return Stream.empty();
        }
        return Stream.of(Issue.at(test.reviewPath(), CourseIssueCode.MASTERY_GUESSABLE,
                "En el módulo «" + test.id() + "» se puede aprobar la prueba de dominio adivinando con una "
                        + "probabilidad de hasta el " + String.format(SPANISH, "%.1f", bound * 100) + " % ("
                        + test.questions() + " preguntas y " + test.minCorrect() + " aciertos; el ejercicio más "
                        + "fácil de adivinar se acierta al azar 1 de cada " + Math.round(1 / p) + " veces). "
                        + "Aumenta questions o usa ejercicios con más opciones."));
    }

    /** Un objeto {@code mastery} del documento, con su ruta y cómo nombrarlo en los mensajes. */
    private record MasteryObject(JsonPointer path, Mastery mastery, String owner) {
    }

    private static Stream<MasteryObject> masteryObjects(CourseDocument course) {
        return Stream.concat(
                Stream.of(new MasteryObject(DEFAULT_MASTERY, course.defaultMastery(),
                        "La prueba de dominio por defecto (defaults.mastery)")),
                Locations.modules(course).stream().flatMap(module -> module.value().mastery().stream()
                        .map(mastery -> new MasteryObject(module.path().key("mastery"), mastery,
                                "La prueba de dominio del módulo «" + module.value().id() + "»"))));
    }

    /** Un módulo con su prueba efectiva (§2.6). */
    private record ModuleMastery(Located<Module> module, Mastery mastery) {

        String id() {
            return module.value().id();
        }

        int questions() {
            return mastery.questions();
        }

        int minCorrect() {
            return mastery.minCorrect();
        }

        int poolSize() {
            return module.value().review().size();
        }

        JsonPointer reviewPath() {
            return module.path().key("review");
        }

        boolean poolTooSmall() {
            return questions() > poolSize();
        }

        /** Opción A: un error de la prueba efectiva calla los avisos de dominio del módulo. */
        boolean hasError() {
            return minCorrect() > questions() || poolTooSmall();
        }
    }

    private static Stream<ModuleMastery> moduleMasteries(CourseDocument course) {
        return Locations.modules(course).stream()
                .map(module -> new ModuleMastery(module, module.value().effectiveMastery(course.defaultMastery())));
    }
}
