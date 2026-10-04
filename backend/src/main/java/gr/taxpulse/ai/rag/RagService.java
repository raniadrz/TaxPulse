package gr.taxpulse.ai.rag;

import gr.taxpulse.ai.dto.ChatResponse;
import gr.taxpulse.ai.ollama.EmbeddingModelClient;
import gr.taxpulse.ai.ollama.OllamaProperties;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Retrieval half of RAG: finds the most relevant document passages for a question. */
@Service
@RequiredArgsConstructor
public class RagService {

    /** Passages below this cosine similarity are considered noise and dropped. */
    static final double MIN_SCORE = 0.25;

    private final EmbeddingModelClient embeddingClient;
    private final VectorStore vectorStore;
    private final OllamaProperties ollamaProperties;

    public List<RetrievedChunk> retrieve(String question, UUID clientId) {
        float[] query = embeddingClient.embed(question);
        return vectorStore.similaritySearch(query, clientId, ollamaProperties.rag().topK()).stream()
                .filter(c -> c.score() >= MIN_SCORE)
                .toList();
    }

    /** Formats passages as a numbered, delimited context block the model can cite as [1], [2]... */
    public static String toContext(List<RetrievedChunk> chunks) {
        StringBuilder sb = new StringBuilder("""
                <context>
                Αποσπάσματα από έγγραφα του γραφείου. Απάντησε ΜΟΝΟ με βάση αυτά και ανέφερε
                την πηγή ως [n]. Αν η απάντηση δεν υπάρχει στα αποσπάσματα, πες το ρητά.
                """);
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            sb.append("\n[").append(i + 1).append("] ").append(c.filename())
                    .append(" (τμήμα ").append(c.chunkIndex() + 1).append(")\n")
                    .append(c.content()).append('\n');
        }
        return sb.append("</context>").toString();
    }

    public static List<ChatResponse.SourceRef> toSources(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(c -> new ChatResponse.SourceRef(c.documentId(), c.filename(), c.chunkIndex(),
                        Math.round(c.score() * 1000) / 1000.0))
                .toList();
    }
}
