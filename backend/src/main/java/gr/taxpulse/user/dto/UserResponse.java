package gr.taxpulse.user.dto;

import gr.taxpulse.user.entity.Role;
import java.time.Instant;
import java.util.UUID;

/** Public representation of a user. Never contains the password hash. */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt,
        UUID clientId) {
}
