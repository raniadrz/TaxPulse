package gr.taxpulse.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** @param clientId optional: restrict retrieval to one client's documents */
public record DocumentQuestionRequest(@NotBlank @Size(max = 2000) String question, UUID clientId) {
}
