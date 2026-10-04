package gr.taxpulse.ai.ollama;

import java.util.List;

/** Provider-agnostic port for text embeddings (used by RAG ingestion and retrieval). */
public interface EmbeddingModelClient {

    /** Embeds every input, preserving order. */
    List<float[]> embed(List<String> inputs);

    default float[] embed(String input) {
        return embed(List.of(input)).get(0);
    }
}
