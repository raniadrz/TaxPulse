package gr.taxpulse.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A chat turn sent by the UI. The {@code system} role is deliberately not accepted, so a client
 * can never override the server-side system prompt.
 */
public record ChatMessageDto(@NotNull Role role, @NotBlank @Size(max = 8000) String content) {

    public enum Role { USER, ASSISTANT }
}
