package gr.taxpulse.ai.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response of {@code GET /api/tags}: locally available models. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaTagsResponse(List<Model> models) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Model(String name, String model, Long size) {
    }
}
