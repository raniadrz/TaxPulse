package gr.taxpulse.document.entity;

import gr.taxpulse.client.entity.Client;
import gr.taxpulse.common.entity.BaseEntity;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Metadata of an uploaded client document. Binary content lives in {@code DocumentStorage}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obligation_id")
    private TaxObligation obligation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "checksum_sha256", nullable = false, length = 64, columnDefinition = "bpchar(64)")
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false, length = 20)
    private IngestionStatus ingestionStatus = IngestionStatus.PENDING;

    @Column(name = "ingestion_error", columnDefinition = "text")
    private String ingestionError;

    /** Full extracted text (kept for re-indexing with a different chunking / embedding model). */
    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;
}
