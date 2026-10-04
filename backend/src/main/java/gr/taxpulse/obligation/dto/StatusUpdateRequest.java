package gr.taxpulse.obligation.dto;

import gr.taxpulse.obligation.entity.ObligationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Workflow transition, optionally with the submission protocol number (αριθμός πρωτοκόλλου). */
public record StatusUpdateRequest(
        @NotNull ObligationStatus status,
        @Size(max = 100) String submissionRef) {
}
