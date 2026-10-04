package gr.taxpulse.obligation.service;

import gr.taxpulse.config.TaxPulseProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Calendar helper bound to the office time zone (Europe/Athens). Deadlines are calendar dates in
 * Greece, so "today" must never be computed in UTC (which is 2-3 hours behind).
 */
@Component
public class OfficeClock {

    private final Clock clock;
    private final ZoneId zone;

    public OfficeClock(Clock clock, TaxPulseProperties properties) {
        this.clock = clock;
        this.zone = ZoneId.of(properties.reminders().zone());
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    public Instant now() {
        return clock.instant();
    }

    public ZoneId zone() {
        return zone;
    }
}
