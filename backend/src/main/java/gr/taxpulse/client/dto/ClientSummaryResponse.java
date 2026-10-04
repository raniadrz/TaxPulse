package gr.taxpulse.client.dto;

import gr.taxpulse.client.entity.BookCategory;
import gr.taxpulse.client.entity.ClientType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Row of the client table. Carries obligation counters so the UI can render status badges
 * without an extra request per row.
 */
public record ClientSummaryResponse(
        UUID id,
        ClientType clientType,
        String afm,
        String name,
        String doy,
        BookCategory bookCategory,
        String primaryKad,
        String email,
        String phone,
        String assignedAccountantName,
        boolean active,
        long openObligations,
        long overdueObligations,
        LocalDate nextDueDate) {
}
