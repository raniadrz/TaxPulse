package gr.taxpulse.document.service;

import gr.taxpulse.client.entity.Client;
import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.common.exception.BusinessRuleException;
import gr.taxpulse.common.exception.ConflictException;
import gr.taxpulse.common.exception.ResourceNotFoundException;
import gr.taxpulse.document.dto.DocumentResponse;
import gr.taxpulse.document.entity.Document;
import gr.taxpulse.document.entity.IngestionStatus;
import gr.taxpulse.document.event.DocumentUploadedEvent;
import gr.taxpulse.document.mapper.DocumentMapper;
import gr.taxpulse.document.repository.DocumentRepository;
import gr.taxpulse.document.storage.DocumentStorage;
import gr.taxpulse.obligation.entity.TaxObligation;
import gr.taxpulse.obligation.service.TaxObligationService;
import gr.taxpulse.security.CurrentUser;
import gr.taxpulse.user.service.UserService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Upload, listing, download and deletion of client documents. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    /** Accepted MIME types -> file extension used for the stored object. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", ".pdf",
            "text/plain", ".txt",
            "text/csv", ".csv",
            "text/markdown", ".md",
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");

    private final DocumentRepository repository;
    private final DocumentStorage storage;
    private final DocumentMapper mapper;
    private final DocumentTextExtractor textExtractor;
    private final ClientService clientService;
    private final TaxObligationService obligationService;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Transactional
    public DocumentResponse upload(UUID clientId, UUID obligationId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Το αρχείο είναι κενό");
        }
        String contentType = normalizeContentType(file.getContentType());
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new BusinessRuleException("Μη υποστηριζόμενος τύπος αρχείου: " + contentType);
        }
        Client client = clientService.getEntity(clientId);
        TaxObligation obligation = obligationId == null ? null : obligationService.getDetailed(obligationId);
        if (obligation != null && !obligation.getClient().getId().equals(clientId)) {
            throw new BusinessRuleException("Η υποχρέωση δεν ανήκει στον πελάτη");
        }

        LocalDate today = LocalDate.now();
        String key = "%s/%d/%02d/%s%s".formatted(clientId, today.getYear(), today.getMonthValue(), UUID.randomUUID(), extension);
        String checksum = storeAndHash(key, file, contentType);
        deleteFileOnRollback(key);

        if (repository.existsByClientIdAndChecksumSha256(clientId, checksum)) {
            throw new ConflictException("Το ίδιο αρχείο έχει ήδη ανέβει για αυτόν τον πελάτη");
        }

        Document doc = new Document();
        doc.setClient(client);
        doc.setObligation(obligation);
        doc.setUploadedBy(CurrentUser.get().map(p -> userService.getEntity(p.id())).orElse(null));
        doc.setOriginalFilename(sanitizeFilename(file.getOriginalFilename()));
        doc.setContentType(contentType);
        doc.setSizeBytes(file.getSize());
        doc.setStorageKey(key);
        doc.setChecksumSha256(checksum);
        doc.setIngestionStatus(textExtractor.supports(contentType) ? IngestionStatus.PENDING : IngestionStatus.SKIPPED);
        Document saved = repository.save(doc);

        if (saved.getIngestionStatus() == IngestionStatus.PENDING) {
            events.publishEvent(new DocumentUploadedEvent(saved.getId()));
        }
        log.info("Document uploaded id={} client={} type={} size={}", saved.getId(), clientId, contentType, file.getSize());
        return mapper.toResponse(saved);
    }

    public PageResponse<DocumentResponse> listForClient(UUID clientId, Pageable pageable) {
        clientService.getEntity(clientId); // 404 for unknown client
        return PageResponse.from(repository.findByClientIdOrderByCreatedAtDesc(clientId, pageable), mapper::toResponse);
    }

    public DocumentResponse findById(UUID id) {
        return mapper.toResponse(getDetailed(id));
    }

    public DownloadableDocument download(UUID id) {
        Document doc = getDetailed(id);
        return new DownloadableDocument(storage.load(doc.getStorageKey()), doc.getOriginalFilename(),
                doc.getContentType(), doc.getSizeBytes());
    }

    @Transactional
    public void delete(UUID id) {
        Document doc = getDetailed(id);
        repository.delete(doc); // document_chunks are removed by ON DELETE CASCADE
        String key = doc.getStorageKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.delete(key);
            }
        });
    }

    /** Marks a document for re-processing (e.g. after switching embedding model). */
    @Transactional
    public DocumentResponse requestReindex(UUID id) {
        Document doc = getDetailed(id);
        if (!textExtractor.supports(doc.getContentType())) {
            throw new BusinessRuleException("Ο τύπος αρχείου δεν υποστηρίζει ευρετηρίαση");
        }
        doc.setIngestionStatus(IngestionStatus.PENDING);
        doc.setIngestionError(null);
        events.publishEvent(new DocumentUploadedEvent(doc.getId()));
        return mapper.toResponse(doc);
    }

    private Document getDetailed(UUID id) {
        return repository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    /** Streams to storage while computing SHA-256 in one pass; verifies PDF magic bytes. */
    private String storeAndHash(String key, MultipartFile file, String contentType) {
        if ("application/pdf".equals(contentType)) {
            try (InputStream head = file.getInputStream()) {
                if (!"%PDF".equals(new String(head.readNBytes(4), StandardCharsets.US_ASCII))) {
                    throw new BusinessRuleException("Το αρχείο δεν είναι έγκυρο PDF");
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            storage.store(key, new DigestInputStream(in, digest));
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void deleteFileOnRollback(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storage.delete(key);
                }
            }
        });
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        int semi = contentType.indexOf(';');
        return (semi >= 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase();
    }

    /** Keeps only the base name and strips control / path characters. */
    static String sanitizeFilename(String name) {
        if (!StringUtils.hasText(name)) {
            return "document";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\p{Cntrl}\"<>|:*?]", "_").trim();
        if (base.isEmpty()) {
            return "document";
        }
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
    }

    public record DownloadableDocument(Resource resource, String filename, String contentType, long size) {
    }
}
