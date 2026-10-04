package gr.taxpulse.ai.dto;

import java.util.List;
import java.util.UUID;

/** Assistant reply plus the document passages it was grounded on (empty when RAG is not used). */
public record ChatResponse(String reply, String model, List<SourceRef> sources) {

    public record SourceRef(UUID documentId, String filename, int chunkIndex, double score) {
    }
}
