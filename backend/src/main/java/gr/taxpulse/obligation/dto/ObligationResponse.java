package gr.taxpulse.obligation.dto;

import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.obligation.entity.ObligationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObligationResponse(
        UUID id,
        ClientRef client,
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
        UserRef assignedTo,
        Instant submittedAt,
        String submissionRef,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public record ClientRef(UUID id, String name, String afm) {
    }

    public record UserRef(UUID id, String fullName) {
    }
}
