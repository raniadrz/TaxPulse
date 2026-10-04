package gr.taxpulse.document.dto;

import gr.taxpulse.document.entity.IngestionStatus;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID clientId,
        UUID obligationId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        IngestionStatus ingestionStatus,
        String ingestionError,
        String uploadedBy,
        Instant createdAt) {
}
