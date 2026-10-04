package gr.taxpulse.client.dto;

import gr.taxpulse.common.validation.ValidAfm;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RepresentativeDto(
        UUID id,
        @NotBlank @Size(max = 150) String fullName,
        @ValidAfm String afm,
        @Size(max = 100) String role,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        boolean primary) {
}
