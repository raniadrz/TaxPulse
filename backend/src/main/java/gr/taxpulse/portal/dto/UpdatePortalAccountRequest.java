package gr.taxpulse.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param password optional: set to reset the account's password */
public record UpdatePortalAccountRequest(
        @NotBlank @Size(max = 150) String fullName,
        boolean active,
        @Size(min = 10, max = 128, message = "Password must be 10-128 characters") String password) {
}
