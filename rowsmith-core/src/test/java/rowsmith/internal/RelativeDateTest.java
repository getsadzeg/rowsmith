package rowsmith.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import rowsmith.internal.config.DateExpr;
import rowsmith.internal.config.RowsmithClock;

class RelativeDateTest {

    private final RowsmithClock clock = new RowsmithClock(Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void relativeDates() {
        assertThat(DateExpr.parse("today-11d").toDateTime(clock).toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 4));
        assertThat(DateExpr.parse("today+1m").toDateTime(clock).toLocalDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(DateExpr.parse("now-2h").toDateTime(clock)).isEqualTo(LocalDate.of(2026, 1, 15).atTime(10, 0));
        assertThat(DateExpr.parse("yesterday")).isNull();
    }
}
