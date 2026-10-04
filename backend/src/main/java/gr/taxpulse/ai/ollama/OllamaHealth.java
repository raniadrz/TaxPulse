package gr.taxpulse.ai.ollama;

import java.util.List;

/** Reachability and model availability of the Ollama server. */
public record OllamaHealth(
        boolean reachable,
        String baseUrl,
        String chatModel,
        boolean chatModelAvailable,
        String embeddingModel,
        boolean embeddingModelAvailable,
        List<String> installedModels,
        String error) {
}
