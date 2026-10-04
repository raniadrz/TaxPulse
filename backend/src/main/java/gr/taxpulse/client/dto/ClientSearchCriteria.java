package gr.taxpulse.client.dto;

import gr.taxpulse.client.entity.BookCategory;
import gr.taxpulse.client.entity.ClientType;
import java.util.UUID;

/** Optional filters of the client list endpoint (bound from query parameters). */
public record ClientSearchCriteria(
        String q,
        ClientType type,
        BookCategory bookCategory,
        Boolean active,
        UUID assignedAccountantId) {
}
