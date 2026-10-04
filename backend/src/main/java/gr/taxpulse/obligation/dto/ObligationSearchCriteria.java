package gr.taxpulse.obligation.dto;

import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.ObligationType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filters of the obligations list, e.g.
 * {@code ?status=PENDING_DOCS&status=IN_PROGRESS&dueFrom=2026-10-01&dueTo=2026-10-31&mine=true}.
 */
public record ObligationSearchCriteria(
        UUID clientId,
        List<ObligationStatus> status,
        ObligationType type,
        UUID assignedToId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
        Boolean mine) {
}
