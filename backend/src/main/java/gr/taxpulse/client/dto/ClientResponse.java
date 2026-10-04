package gr.taxpulse.client.dto;

import gr.taxpulse.client.entity.BookCategory;
import gr.taxpulse.client.entity.ClientType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full client view (detail screen). */
public record ClientResponse(
        UUID id,
        ClientType clientType,
        String afm,
        String doy,
        String name,
        String tradeName,
        String legalForm,
        BookCategory bookCategory,
        String gemiNumber,
        String email,
        String phone,
        String mobile,
        AddressDto address,
        AccountantRef assignedAccountant,
        String notes,
        boolean active,
        List<ActivityCodeDto> activityCodes,
        List<RepresentativeDto> representatives,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public record AccountantRef(UUID id, String fullName) {
    }
}
