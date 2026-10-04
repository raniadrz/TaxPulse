package gr.taxpulse.document.event;

import java.util.UUID;

/** Published inside the upload transaction; ingestion starts only after it commits. */
public record DocumentUploadedEvent(UUID documentId) {
}
