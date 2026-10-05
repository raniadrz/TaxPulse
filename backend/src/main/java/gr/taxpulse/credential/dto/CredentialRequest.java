package gr.taxpulse.credential.dto;

import gr.taxpulse.credential.entity.CredentialKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param password required when adding; on update, blank keeps the stored one */
public record CredentialRequest(
        @NotNull CredentialKind kind,
        @Size(max = 100) String label,
        @NotBlank @Size(max = 255) String username,
        @Size(max = 255) String password) {
}
