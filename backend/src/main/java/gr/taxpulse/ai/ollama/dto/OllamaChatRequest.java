package gr.taxpulse.ai.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /api/chat}.
 *
 * @param format {@code null} for free text, {@code "json"} for any JSON, or a JSON Schema object
 *               to constrain decoding to that exact structure (structured outputs)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        Object format,
        Map<String, Object> options,
        @JsonProperty("keep_alive") String keepAlive) {
}
