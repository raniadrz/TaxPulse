package gr.taxpulse.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** ΚΑΔ, accepted with or without dots (e.g. "69.20.10.01" or "69201001"). */
public record ActivityCodeDto(
        @NotBlank @Pattern(regexp = "^\\d{2}(\\.?\\d{1,2}){0,3}$", message = "Μη έγκυρος ΚΑΔ") String code,
        @Size(max = 255) String description,
        boolean primary) {
}
