package io.github.rowsmith.internal.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class RowsmithClock {
    private final Clock clock;

    public RowsmithClock(Clock clock) {
        this.clock = clock;
    }

    public ZoneId zone() { return clock.getZone(); }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    public OffsetDateTime toOffset(LocalDateTime t) {
        return t.atZone(zone()).toOffsetDateTime();
    }
}
