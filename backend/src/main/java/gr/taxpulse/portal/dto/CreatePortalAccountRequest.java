package gr.taxpulse.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A login for a client (or one of its representatives) to the client portal. */
public record CreatePortalAccountRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(min = 10, max = 128, message = "Password must be 10-128 characters") String password) {
}
