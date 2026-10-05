package gr.taxpulse.portal.dto;

import gr.taxpulse.obligation.dto.ObligationResponse;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.ObligationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** An obligation as shown to the client: no internal notes, assignee or concurrency metadata. */
public record PortalObligationResponse(
        UUID id,
        ObligationType obligationType,
        String obligationTypeLabel,
        String title,
        String description,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate dueDate,
        long daysUntilDue,
        ObligationStatus status,
        BigDecimal amount,
        Instant submittedAt,
        String submissionRef,
        long messageCount) {

    public static PortalObligationResponse from(ObligationResponse o) {
        return new PortalObligationResponse(o.id(), o.obligationType(), o.obligationTypeLabel(), o.title(),
                o.description(), o.periodStart(), o.periodEnd(), o.dueDate(), o.daysUntilDue(), o.status(),
                o.amount(), o.submittedAt(), o.submissionRef(), o.messageCount());
    }
}
