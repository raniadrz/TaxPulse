package gr.taxpulse.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A question about the caller's own documents; the client scope comes from the session, never the request. */
public record PortalQuestionRequest(@NotBlank @Size(max = 2000) String question) {
}
