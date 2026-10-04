package gr.taxpulse.obligation.controller;

import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.obligation.dto.ObligationRequest;
import gr.taxpulse.obligation.dto.ObligationResponse;
import gr.taxpulse.obligation.dto.ObligationSearchCriteria;
import gr.taxpulse.obligation.dto.StatusUpdateRequest;
import gr.taxpulse.obligation.service.TaxObligationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tax calendar REST API.
 * <ul>
 *   <li>Read and status changes: all staff (assistants update the workflow).</li>
 *   <li>Create / edit: ADMIN, ACCOUNTANT. Delete: ADMIN.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/obligations")
@RequiredArgsConstructor
public class TaxObligationController {

    private final TaxObligationService service;

    @GetMapping
    public PageResponse<ObligationResponse> search(ObligationSearchCriteria criteria,
                                                   @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC)
                                                   Pageable pageable) {
        return service.search(criteria, pageable);
    }

    @GetMapping("/{id}")
    public ObligationResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ObligationResponse> create(@Valid @RequestBody ObligationRequest request) {
        ObligationResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/obligations/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ObligationResponse update(@PathVariable UUID id, @Valid @RequestBody ObligationRequest request) {
        return service.update(id, request);
    }

    /** Workflow transition: PENDING_DOCS / IN_PROGRESS / SUBMITTED (OVERDUE is system-managed). */
    @PatchMapping("/{id}/status")
    public ObligationResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        return service.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
