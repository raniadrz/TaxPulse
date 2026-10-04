package gr.taxpulse.client.controller;

import gr.taxpulse.client.dto.ClientRequest;
import gr.taxpulse.client.dto.ClientResponse;
import gr.taxpulse.client.dto.ClientSearchCriteria;
import gr.taxpulse.client.dto.ClientSummaryResponse;
import gr.taxpulse.client.service.ClientService;
import gr.taxpulse.common.dto.PageResponse;
import gr.taxpulse.common.validation.ValidAfm;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client (CRM) REST API.
 * <ul>
 *   <li>Read: any authenticated staff member.</li>
 *   <li>Create / update: ADMIN, ACCOUNTANT.</li>
 *   <li>Delete: ADMIN only.</li>
 * </ul>
 */
@Validated
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    /** Paged search, e.g. {@code GET /api/v1/clients?q=0940&type=LEGAL_ENTITY&page=0&size=20&sort=name,asc}. */
    @GetMapping
    public PageResponse<ClientSummaryResponse> search(ClientSearchCriteria criteria,
                                                      @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
                                                      Pageable pageable) {
        return clientService.search(criteria, pageable);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable UUID id) {
        return clientService.findById(id);
    }

    /** Fast exact lookup by ΑΦΜ. */
    @GetMapping("/by-afm/{afm}")
    public ClientResponse getByAfm(@PathVariable @ValidAfm String afm) {
        return clientService.findByAfm(afm);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        ClientResponse created = clientService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/clients/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return clientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
