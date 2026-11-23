package es.ulpgc.pamn.dominio.backend.application.course;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Reloj falso para tests que se puede adelantar: sirve para comprobar que un caso de uso lee la hora en
 * cada llamada y no una sola vez al crearse. {@link Clock#fixed} no vale, porque no cambia nunca.
 */
final class AdjustableClock extends Clock {

    private Instant now;

    AdjustableClock(Instant now) {
        this.now = Objects.requireNonNull(now, "now");
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /** Los casos de uso solo leen {@link #instant()}: cambiar de zona no hace falta en los tests. */
    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("AdjustableClock solo trabaja en UTC");
    }
}
