package gr.taxpulse.ai.rag;

import java.util.List;
import java.util.UUID;

/** Port for chunk/embedding persistence and nearest-neighbour search. */
public interface VectorStore {

    /** Replaces all chunks of a document atomically. */
    void replaceChunks(UUID documentId, UUID clientId, List<String> chunks, List<float[]> embeddings);

    /**
     * @param clientId restrict the search to one client's documents, or {@code null} for all
     */
    List<RetrievedChunk> similaritySearch(float[] queryEmbedding, UUID clientId, int topK);

    void deleteByDocument(UUID documentId);
}
