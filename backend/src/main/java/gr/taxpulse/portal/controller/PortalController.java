package gr.taxpulse.portal.controller;

import gr.taxpulse.ai.dto.ChatResponse;
import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.document.dto.DocumentResponse;
import gr.taxpulse.document.service.DocumentService;
import gr.taxpulse.obligation.entity.ObligationStatus;
import gr.taxpulse.portal.dto.PortalObligationResponse;
import gr.taxpulse.portal.dto.PortalProfileResponse;
import gr.taxpulse.portal.dto.PortalQuestionRequest;
import gr.taxpulse.portal.service.PortalService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Client portal API. Restricted to {@code CLIENT} accounts by {@code SecurityConfig}; every
 * endpoint works on the caller's own client.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService service;

    @GetMapping("/profile")
    public PortalProfileResponse profile() {
        return service.profile();
    }

    @GetMapping("/obligations")
    public PageResponse<PortalObligationResponse> obligations(@RequestParam(required = false) List<ObligationStatus> status,
                                                              @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC)
                                                              Pageable pageable) {
        return service.obligations(status, pageable);
    }

    @GetMapping("/documents")
    public PageResponse<DocumentResponse> documents(@PageableDefault(size = 20) Pageable pageable) {
        return service.documents(pageable);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@RequestParam(required = false) UUID obligationId,
                                                   @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(201).body(service.upload(obligationId, file));
    }

    @GetMapping("/documents/{id}/download")
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

    /** RAG over the caller's own indexed documents. */
    @PostMapping("/ai/ask")
    public ChatResponse ask(@Valid @RequestBody PortalQuestionRequest request) {
        return service.askDocuments(request.question());
    }
}
