package gr.taxpulse.portal.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Row of the office-wide list of client portal logins.
 *
 * @param clientActive a portal account only works while its client is active
 */
public record PortalAccountSummary(
        UUID id,
        String email,
        String fullName,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt,
        UUID clientId,
        String clientName,
        boolean clientActive) {
}
