package gr.taxpulse.ai.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Body of {@code POST /api/generate} (single-shot completion). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaGenerateRequest(
        String model,
        String prompt,
        String system,
        boolean stream,
        Object format,
        Map<String, Object> options,
        @JsonProperty("keep_alive") String keepAlive) {
}
