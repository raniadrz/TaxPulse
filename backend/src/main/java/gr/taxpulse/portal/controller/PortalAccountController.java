package gr.taxpulse.portal.controller;

import gr.taxpulse.portal.dto.CreatePortalAccountRequest;
import gr.taxpulse.portal.dto.UpdatePortalAccountRequest;
import gr.taxpulse.portal.service.PortalAccountService;
import gr.taxpulse.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Portal logins of a client. Read: all staff. Create / update: ADMIN, ACCOUNTANT. */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/portal-accounts")
@RequiredArgsConstructor
public class PortalAccountController {

    private final PortalAccountService service;

    @GetMapping
    public List<UserResponse> list(@PathVariable UUID clientId) {
        return service.list(clientId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<UserResponse> create(@PathVariable UUID clientId,
                                               @Valid @RequestBody CreatePortalAccountRequest request) {
        UserResponse created = service.create(clientId, request);
        return ResponseEntity.created(URI.create("/api/v1/clients/%s/portal-accounts/%s".formatted(clientId, created.id())))
                .body(created);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public UserResponse update(@PathVariable UUID clientId, @PathVariable UUID userId,
                               @Valid @RequestBody UpdatePortalAccountRequest request) {
        return service.update(clientId, userId, request);
    }
}
