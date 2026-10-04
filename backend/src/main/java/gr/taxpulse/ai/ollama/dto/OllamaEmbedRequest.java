package gr.taxpulse.ai.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Body of {@code POST /api/embed} (batch embeddings). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaEmbedRequest(String model, List<String> input, @JsonProperty("keep_alive") String keepAlive) {
}
