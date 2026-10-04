package gr.taxpulse.document.entity;

/** Lifecycle of a document in the RAG pipeline. */
public enum IngestionStatus {
    /** Uploaded, waiting for the async ingestion worker. */
    PENDING,
    PROCESSING,
    /** Text extracted, chunked and embedded: searchable by the copilot. */
    INDEXED,
    FAILED,
    /** Stored only (e.g. images without OCR). */
    SKIPPED
}
