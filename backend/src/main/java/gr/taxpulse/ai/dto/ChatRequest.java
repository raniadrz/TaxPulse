package gr.taxpulse.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * @param clientId     optional: grounds the assistant on this client's profile and open obligations
 * @param useDocuments optional: retrieve relevant passages from uploaded documents (RAG)
 */
public record ChatRequest(
        @NotEmpty @Size(max = 40) List<@Valid ChatMessageDto> messages,
        UUID clientId,
        boolean useDocuments) {
}
