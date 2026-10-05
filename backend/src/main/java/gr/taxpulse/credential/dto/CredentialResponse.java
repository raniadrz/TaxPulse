package gr.taxpulse.credential.dto;

import gr.taxpulse.credential.entity.CredentialKind;
import java.time.Instant;
import java.util.UUID;

/** A stored login without its password (that is only returned by the audited reveal). */
public record CredentialResponse(
        UUID id,
        CredentialKind kind,
        String kindLabel,
        String label,
        String username,
        String updatedByName,
        boolean updatedByClient,
        Instant updatedAt) {
}
