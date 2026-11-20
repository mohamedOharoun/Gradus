package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentOrderTest {

    static JsonPointer pointer(Object... tokens) {
        JsonPointer pointer = JsonPointer.root();
        for (Object token : tokens) {
            pointer = token instanceof Integer index ? pointer.index(index) : pointer.key((String) token);
        }
        return pointer;
    }

    static List<String> sorted(JsonPointer... pointers) {
        return Stream.of(pointers).sorted(DocumentOrder.POINTERS).map(JsonPointer::toString).toList();
    }

    @Test
    void indexesAreComparedAsNumbers() {
        assertEquals(List.of("/modules/2", "/modules/10"), sorted(pointer("modules", 10), pointer("modules", 2)));
    }

    @Test
    void aContainerComesBeforeWhatItContains() {
        assertEquals(List.of("", "/modules/1", "/modules/1/requires/0"),
                sorted(pointer("modules", 1, "requires", 0), pointer(), pointer("modules", 1)));
    }

    @Test
    void courseKeysFollowTheOrderOfTheSchema() {
        assertEquals(List.of("/title", "/tags/0", "/defaults/mastery", "/modules/0"),
                sorted(pointer("modules", 0), pointer("defaults", "mastery"), pointer("tags", 0), pointer("title")));
    }

    @Test
    void moduleKeysFollowTheOrderOfTheSchema() {
        assertEquals(List.of("/modules/0/requires/0", "/modules/0/mastery", "/modules/0/steps", "/modules/0/review"),
                sorted(pointer("modules", 0, "review"), pointer("modules", 0, "steps"),
                        pointer("modules", 0, "mastery"), pointer("modules", 0, "requires", 0)));
    }

    @Test
    void exerciseKeysFollowTheOrderOfTheSchema() {
        JsonPointer exercise = pointer("modules", 0, "review", 3);

        assertEquals(List.of("/modules/0/review/3/options/1", "/modules/0/review/3/correct/0"),
                sorted(exercise.key("correct").index(0), exercise.key("options").index(1)));
    }

    @Test
    void unknownKeysGoAfterTheKnownOnesAlphabetically() {
        assertEquals(List.of("/modules", "/autor", "/zeta"), sorted(pointer("zeta"), pointer("autor"), pointer("modules")));
    }
}
