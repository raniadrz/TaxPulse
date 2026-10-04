package gr.taxpulse.document.mapper;

import gr.taxpulse.document.dto.DocumentResponse;
import gr.taxpulse.document.entity.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getClient().getId(),
                d.getObligation() == null ? null : d.getObligation().getId(),
                d.getOriginalFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getChecksumSha256(),
                d.getIngestionStatus(),
                d.getIngestionError(),
                d.getUploadedBy() == null ? null : d.getUploadedBy().getFullName(),
                d.getCreatedAt());
    }
}
