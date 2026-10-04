package gr.taxpulse.user.dto;

import gr.taxpulse.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role,
        @NotBlank @Size(min = 10, max = 128, message = "Password must be 10-128 characters") String password) {
}
