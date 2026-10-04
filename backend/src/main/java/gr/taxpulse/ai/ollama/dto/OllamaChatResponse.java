package gr.taxpulse.ai.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Non-streaming response of {@code POST /api/chat}. Durations are in nanoseconds. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(
        String model,
        OllamaMessage message,
        boolean done,
        @JsonProperty("done_reason") String doneReason,
        @JsonProperty("total_duration") Long totalDuration,
        @JsonProperty("prompt_eval_count") Integer promptEvalCount,
        @JsonProperty("eval_count") Integer evalCount) {
}
