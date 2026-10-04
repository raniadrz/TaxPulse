package gr.taxpulse.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Free text (notes, e-mail body, OCR output) to extract structured data from. */
public record ExtractionRequest(@NotBlank @Size(max = 20_000) String text) {
}
