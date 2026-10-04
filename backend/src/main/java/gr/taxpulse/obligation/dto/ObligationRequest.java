package gr.taxpulse.obligation.dto;

import gr.taxpulse.obligation.entity.ObligationType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Create / full-update payload. Status is changed only through the dedicated status endpoint. */
public record ObligationRequest(
        @NotNull UUID clientId,
        @NotNull ObligationType obligationType,
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate periodStart,
        LocalDate periodEnd,
        @NotNull LocalDate dueDate,
        @Digits(integer = 12, fraction = 2) BigDecimal amount,
        UUID assignedToId,
        String notes) {

    @AssertTrue(message = "Η λήξη περιόδου πρέπει να είναι μετά την έναρξη")
    boolean isPeriodValid() {
        return periodStart == null || periodEnd == null || !periodEnd.isBefore(periodStart);
    }
}
