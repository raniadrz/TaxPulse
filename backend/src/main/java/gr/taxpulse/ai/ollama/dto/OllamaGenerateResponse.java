package gr.taxpulse.ai.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaGenerateResponse(
        String model,
        String response,
        boolean done,
        @JsonProperty("done_reason") String doneReason,
        @JsonProperty("total_duration") Long totalDuration,
        @JsonProperty("eval_count") Integer evalCount) {
}
