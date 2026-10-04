package gr.taxpulse.client.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Port (Dependency Inversion) through which the CRM obtains obligation counters for a page of
 * clients, without the client module depending on the obligation module's internals.
 * Implemented by the obligation module.
 */
public interface ClientObligationStatsPort {

    /** Null-object used when no obligation module is wired (e.g. slice tests). */
    ClientObligationStatsPort NONE = clientIds -> Map.of();

    Map<UUID, Stats> statsFor(Collection<UUID> clientIds);

    record Stats(long open, long overdue, LocalDate nextDueDate) {
        public static final Stats EMPTY = new Stats(0, 0, null);
    }
}
