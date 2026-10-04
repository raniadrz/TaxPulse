package gr.taxpulse.document.controller;

import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.document.dto.DocumentResponse;
import gr.taxpulse.document.service.DocumentService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Client documents: upload (multipart), list, download, re-index and delete. */
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping(value = "/api/v1/clients/{clientId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','ASSISTANT')")
    public ResponseEntity<DocumentResponse> upload(@PathVariable UUID clientId,
                                                   @RequestParam(required = false) UUID obligationId,
                                                   @RequestPart("file") MultipartFile file) {
        DocumentResponse created = service.upload(clientId, obligationId, file);
        return ResponseEntity.created(URI.create("/api/v1/documents/" + created.id())).body(created);
    }

    @GetMapping("/api/v1/clients/{clientId}/documents")
    public PageResponse<DocumentResponse> list(@PathVariable UUID clientId, @PageableDefault(size = 20) Pageable pageable) {
        return service.listForClient(clientId, pageable);
    }

    @GetMapping("/api/v1/documents/{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/api/v1/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        DocumentService.DownloadableDocument doc = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .contentLength(doc.size())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(doc.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(doc.resource());
    }

    @PostMapping("/api/v1/documents/{id}/reindex")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public DocumentResponse reindex(@PathVariable UUID id) {
        return service.requestReindex(id);
    }

    @DeleteMapping("/api/v1/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
