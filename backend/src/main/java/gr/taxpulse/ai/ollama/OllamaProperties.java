package gr.taxpulse.ai.ollama;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Connection and model settings for the local Ollama server ({@code ollama.*}).
 *
 * @param keepAlive how long Ollama keeps the model loaded in memory after a request (e.g. "10m")
 */
@Validated
@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
        @NotBlank String baseUrl,
        @NotBlank String chatModel,
        @NotBlank String embeddingModel,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        String keepAlive,
        double temperature,
        @Min(512) int contextWindow,
        @Valid @NotNull Rag rag) {

    /**
     * Retrieval settings.
     *
     * @param chunkSize    characters per chunk
     * @param chunkOverlap characters shared between consecutive chunks (keeps sentences intact)
     * @param topK         number of chunks injected into the prompt
     */
    public record Rag(@Min(200) int chunkSize, @Min(0) int chunkOverlap, @Min(1) int topK) {
    }
}
