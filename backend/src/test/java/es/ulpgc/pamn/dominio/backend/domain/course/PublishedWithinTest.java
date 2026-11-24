package es.ulpgc.pamn.dominio.backend.domain.course;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublishedWithinTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    @Test
    void monthAndYearAreTheOnlyValidParameters() {
        assertEquals(Optional.of(PublishedWithin.LAST_MONTH), PublishedWithin.fromParameter("month"));
        assertEquals(Optional.of(PublishedWithin.LAST_YEAR), PublishedWithin.fromParameter("year"));
    }

    @Test
    void spacesAroundTheParameterAreIgnored() {
        assertEquals(Optional.of(PublishedWithin.LAST_MONTH), PublishedWithin.fromParameter("  month "));
        assertEquals(Optional.of(PublishedWithin.LAST_YEAR), PublishedWithin.fromParameter("\tyear\n"));
    }

    @Test
    void anythingElseIsEmpty() {
        for (String invalid : new String[]{"Month", "YEAR", "week", "any", "1", "mon th", "", "   "}) {
            assertEquals(Optional.empty(), PublishedWithin.fromParameter(invalid), "«" + invalid + "»");
        }
    }

    @Test
    void aNullParameterIsEmpty() {
        assertEquals(Optional.empty(), PublishedWithin.fromParameter(null));
    }

    @Test
    void lastMonthStartsAtTheSameTimeOneMonthAgo() {
        assertEquals(Instant.parse("2026-10-22T10:00:00Z"), PublishedWithin.LAST_MONTH.since(NOW));
    }

    @Test
    void lastYearStartsAtTheSameTimeOneYearAgo() {
        assertEquals(Instant.parse("2025-11-22T10:00:00Z"), PublishedWithin.LAST_YEAR.since(NOW));
    }

    @Test
    void aShorterMonthClampsToItsLastDay() {
        assertEquals(Instant.parse("2026-02-28T10:00:00Z"),
                PublishedWithin.LAST_MONTH.since(Instant.parse("2026-03-31T10:00:00Z")));
    }

    @Test
    void inALeapYearTheMonthClampsToTheTwentyNinth() {
        assertEquals(Instant.parse("2028-02-29T10:00:00Z"),
                PublishedWithin.LAST_MONTH.since(Instant.parse("2028-03-31T10:00:00Z")));
    }

    @Test
    void aYearBeforeTheTwentyNinthOfFebruaryIsThe28th() {
        assertEquals(Instant.parse("2027-02-28T10:00:00Z"),
                PublishedWithin.LAST_YEAR.since(Instant.parse("2028-02-29T10:00:00Z")));
    }

    @Test
    void theCalendarIsUtcWhateverTheDefaultTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            // En Madrid el 15 de abril es CEST (UTC+2) y el 15 de marzo, CET (UTC+1): contando en
            // la zona local, «hace un mes» se desplazaría una hora. En UTC no.
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
            assertEquals(Instant.parse("2026-03-15T12:00:00Z"),
                    PublishedWithin.LAST_MONTH.since(Instant.parse("2026-04-15T12:00:00Z")));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void theSubSecondPartIsKept() {
        assertEquals(Instant.parse("2026-10-22T10:00:00.123456789Z"),
                PublishedWithin.LAST_MONTH.since(Instant.parse("2026-11-22T10:00:00.123456789Z")));
    }
}
