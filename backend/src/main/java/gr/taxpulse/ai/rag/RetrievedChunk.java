package gr.taxpulse.ai.rag;

import java.util.UUID;

/**
 * A chunk returned by similarity search.
 *
 * @param score cosine similarity in [-1, 1]; higher is more relevant
 */
public record RetrievedChunk(UUID documentId, String filename, int chunkIndex, String content, double score) {
}
