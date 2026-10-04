package gr.taxpulse.ai.rag;

import gr.taxpulse.ai.ollama.EmbeddingModelClient;
import gr.taxpulse.ai.ollama.OllamaProperties;
import gr.taxpulse.document.entity.Document;
import gr.taxpulse.document.entity.IngestionStatus;
import gr.taxpulse.document.event.DocumentUploadedEvent;
import gr.taxpulse.document.repository.DocumentRepository;
import gr.taxpulse.document.service.DocumentTextExtractor;
import gr.taxpulse.document.storage.DocumentStorage;
import gr.taxpulse.notification.dto.NotificationCommand;
import gr.taxpulse.notification.entity.NotificationType;
import gr.taxpulse.notification.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RAG ingestion pipeline: extract text -> chunk -> embed (local Ollama) -> store in pgvector.
 *
 * <p>Runs asynchronously after the upload commits. Slow work (PDF parsing, embeddings) happens
 * outside any DB transaction; only the short status updates are transactional.</p>
 */
@Slf4j
@Service
public class DocumentIngestionService {

    private static final int EMBED_BATCH = 16;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final DocumentRepository documentRepository;
    private final DocumentStorage storage;
    private final DocumentTextExtractor textExtractor;
    private final EmbeddingModelClient embeddingClient;
    private final VectorStore vectorStore;
    private final NotificationService notificationService;
    private final TextChunker chunker;
    private final TransactionTemplate tx;

    public DocumentIngestionService(DocumentRepository documentRepository, DocumentStorage storage,
                                    DocumentTextExtractor textExtractor, EmbeddingModelClient embeddingClient,
                                    VectorStore vectorStore, NotificationService notificationService,
                                    OllamaProperties ollamaProperties, PlatformTransactionManager txManager) {
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.textExtractor = textExtractor;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.notificationService = notificationService;
        this.chunker = new TextChunker(ollamaProperties.rag().chunkSize(), ollamaProperties.rag().chunkOverlap());
        this.tx = new TransactionTemplate(txManager);
    }

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        ingest(event.documentId());
    }

    public void ingest(UUID documentId) {
        Optional<Job> maybeJob = tx.execute(status -> documentRepository.findDetailedById(documentId).map(doc -> {
            doc.setIngestionStatus(IngestionStatus.PROCESSING);
            return Job.of(doc);
        }));
        if (maybeJob == null || maybeJob.isEmpty()) {
            log.warn("Document {} vanished before ingestion", documentId);
            return;
        }
        Job job = maybeJob.get();
        try {
            Optional<String> text = textExtractor.extract(storage.resolve(job.storageKey()), job.contentType());
            if (text.isEmpty()) {
                finish(documentId, IngestionStatus.SKIPPED, null, "Δεν βρέθηκε κείμενο (πιθανώς σαρωμένο έγγραφο)");
                return;
            }
            List<String> chunks = chunker.split(text.get());
            List<float[]> embeddings = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i += EMBED_BATCH) {
                embeddings.addAll(embeddingClient.embed(chunks.subList(i, Math.min(i + EMBED_BATCH, chunks.size()))));
            }
            vectorStore.replaceChunks(documentId, job.clientId(), chunks, embeddings);
            finish(documentId, IngestionStatus.INDEXED, text.get(), null);
            log.info("Document {} indexed: {} chunks", documentId, chunks.size());
            notifyUploader(job, chunks.size());
        } catch (Exception ex) {
            log.error("Ingestion failed for document {}", documentId, ex);
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            finish(documentId, IngestionStatus.FAILED, null,
                    message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message);
        }
    }

    private void finish(UUID documentId, IngestionStatus status, String text, String error) {
        tx.executeWithoutResult(s -> documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setIngestionStatus(status);
            doc.setIngestionError(error);
            if (text != null) {
                doc.setExtractedText(text);
            }
        }));
    }

    private void notifyUploader(Job job, int chunks) {
        if (job.uploaderId() == null) {
            return;
        }
        notificationService.notify(new NotificationCommand(job.uploaderId(), null, NotificationType.DOCUMENT_PROCESSED,
                "Έγγραφο έτοιμο για αναζήτηση: " + job.filename(),
                "Το έγγραφο «%s» ευρετηριάστηκε (%d τμήματα) και είναι διαθέσιμο στον AI Copilot."
                        .formatted(job.filename(), chunks),
                null, job.clientId(), "DOC_INDEXED:%s:%s".formatted(job.documentId(), UUID.randomUUID())));
    }

    /** Detached snapshot of what the worker needs, so no entity escapes its transaction. */
    private record Job(UUID documentId, UUID clientId, UUID uploaderId, String storageKey, String contentType,
                       String filename) {
        static Job of(Document d) {
            return new Job(d.getId(), d.getClient().getId(),
                    d.getUploadedBy() == null ? null : d.getUploadedBy().getId(),
                    d.getStorageKey(), d.getContentType(), d.getOriginalFilename());
        }
    }
}
