package gr.taxpulse.client.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @Size(max = 255) String street,
        @Size(max = 100) String city,
        @Pattern(regexp = "^\\d{3}\\s?\\d{2}$", message = "Ο Τ.Κ. πρέπει να έχει 5 ψηφία") String postalCode) {
}
