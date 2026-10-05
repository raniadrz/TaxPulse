package gr.taxpulse.portal.dto;

import gr.taxpulse.client.dto.AddressDto;
import gr.taxpulse.client.entity.ClientType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What a client sees about itself. Deliberately narrower than the staff {@code ClientResponse}:
 * internal notes and office bookkeeping fields are never exposed to the portal.
 */
public record PortalProfileResponse(
        UUID id,
        ClientType clientType,
        String name,
        String afm,
        String doy,
        String legalForm,
        String email,
        String phone,
        AddressDto address,
        Accountant accountant,
        long openObligations,
        long overdueObligations,
        LocalDate nextDueDate) {

    public record Accountant(String fullName, String email) {
    }
}
