package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@link PageRequest} y {@link Page}. */
class PageTest {

    @Test
    void pagesStartAtZeroAndHoldBetween1And100Items() {
        assertDoesNotThrow(() -> new PageRequest(0, 1));
        assertDoesNotThrow(() -> new PageRequest(7, 100));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 101));
    }

    @Test
    void theDefaultSizeIs20() {
        assertEquals(new PageRequest(0, 20), PageRequest.first());
    }

    @Test
    void theOffsetSkipsThePreviousPages() {
        assertEquals(0, new PageRequest(0, 20).offset());
        assertEquals(60, new PageRequest(3, 20).offset());
    }

    @Test
    void aPageKeepsItsItemsAndTheTotalOfAllPages() {
        Page<String> page = new Page<>(List.of("a", "b"), new PageRequest(1, 2), 37);

        assertEquals(List.of("a", "b"), page.items());
        assertEquals(1, page.request().page());
        assertEquals(37, page.total());
    }

    @Test
    void aPageCannotHoldMoreItemsThanItsSizeOrThanTheTotal() {
        assertThrows(IllegalArgumentException.class, () -> new Page<>(List.of("a", "b", "c"), new PageRequest(0, 2), 10));
        assertThrows(IllegalArgumentException.class, () -> new Page<>(List.of("a", "b"), new PageRequest(0, 2), 1));
    }

    @Test
    void mappingChangesTheItemsAndKeepsThePosition() {
        Page<Integer> lengths = new Page<>(List.of("a", "bcd"), new PageRequest(0, 20), 2).map(String::length);

        assertEquals(List.of(1, 3), lengths.items());
        assertEquals(2, lengths.total());
    }
}
