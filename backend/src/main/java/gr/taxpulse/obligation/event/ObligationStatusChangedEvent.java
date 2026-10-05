package gr.taxpulse.obligation.event;

import gr.taxpulse.obligation.entity.ObligationStatus;
import java.util.UUID;

/** Published when a user moves an obligation through its workflow (not for system OVERDUE marking). */
public record ObligationStatusChangedEvent(
        UUID obligationId,
        UUID clientId,
        String title,
        String typeLabel,
        ObligationStatus previous,
        ObligationStatus current,
        String submissionRef) {
}
